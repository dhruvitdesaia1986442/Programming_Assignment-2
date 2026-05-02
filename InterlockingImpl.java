import java.util.*;

public class InterlockingImpl implements Interlocking {

    private static final int OUT_OF_SYSTEM = -1;

    private static class Train {
        String name;
        int entry;
        int destination;
        int[] route;
        int routeIndex = 0;
        boolean active = true;

        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
        }

        int getCurrentSection() {
            return route[routeIndex];
        }

        Integer getNextSection() {
            if (routeIndex >= route.length - 1) {
                return null;
            }
            return route[routeIndex + 1];
        }

        void moveForward() {
            routeIndex++;
        }

        void exitSystem() {
            active = false;
        }
    }

    private final Map<Integer, String> sections = new HashMap<>();
    private final Map<String, Train> activeTrains = new HashMap<>();
    private final Set<String> allTrainNames = new HashSet<>();

    public InterlockingImpl() {
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        if (allTrainNames.contains(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        int[] route = getRoute(entryTrackSection, destinationTrackSection);

        if (route == null) {
            throw new IllegalArgumentException("Invalid journey.");
        }

        Train train = new Train(trainName, entryTrackSection, destinationTrackSection, route);

        activeTrains.put(trainName, train);
        allTrainNames.add(trainName);
        sections.put(entryTrackSection, trainName);
    }

    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Null train list.");
        }

        int movedCount = 0;

        Set<String> seen = new HashSet<>();
        List<String> requested = new ArrayList<>();

        // Remove duplicate requested trains while keeping order.
        for (String name : trainNames) {
            if (name != null && seen.add(name)) {
                requested.add(name);
            }
        }

        Map<String, int[]> desiredMoves = new LinkedHashMap<>();
        Set<String> plannedExits = new HashSet<>();
        Set<String> blocked = new HashSet<>();

        // Phase 1: collect exits and desired moves.
        for (String name : requested) {
            Train train = activeTrains.get(name);

            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();
            Integer next = train.getNextSection();

            // Train has reached the end of its route, so it exits.
            if (next == null) {
                plannedExits.add(name);
                continue;
            }

            if (canMove(train, next, desiredMoves, plannedExits)) {
                desiredMoves.put(name, new int[]{current, next});
            }
        }

        // Phase 2: detect same-target and swap conflicts.
        List<String> names = new ArrayList<>(desiredMoves.keySet());

        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                String a = names.get(i);
                String b = names.get(j);

                int[] m1 = desiredMoves.get(a);
                int[] m2 = desiredMoves.get(b);

                int from1 = m1[0];
                int to1 = m1[1];
                int from2 = m2[0];
                int to2 = m2[1];

                // Same destination = collision risk.
                if (to1 == to2) {
                    blocked.add(a);
                    blocked.add(b);
                }

                // Direct swap = collision/deadlock.
                if (from1 == to2 && from2 == to1) {
                    blocked.add(a);
                    blocked.add(b);
                }
            }
        }

        // Phase 3: apply exits.
        for (String name : plannedExits) {
            if (blocked.contains(name)) {
                continue;
            }

            Train train = activeTrains.get(name);

            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();

            sections.put(current, null);
            train.exitSystem();
            activeTrains.remove(name);
            movedCount++;
        }

        // Phase 4: apply moves.
        for (Map.Entry<String, int[]> entry : desiredMoves.entrySet()) {
            String name = entry.getKey();

            if (blocked.contains(name)) {
                continue;
            }

            Train train = activeTrains.get(name);

            if (train == null || !train.active) {
                continue;
            }

            int from = entry.getValue()[0];
            int to = entry.getValue()[1];

            sections.put(from, null);
            train.moveForward();
            sections.put(to, name);
            movedCount++;
        }

        return movedCount;
    }

    @Override
    public String getSection(int trackSection) {
        validateSection(trackSection);
        return sections.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) {
        validateTrainName(trainName);

        Train train = activeTrains.get(trainName);

        if (train != null) {
            return train.getCurrentSection();
        }

        if (allTrainNames.contains(trainName)) {
            return OUT_OF_SYSTEM;
        }

        throw new IllegalArgumentException("Unknown train.");
    }

    private boolean canMove(
            Train train,
            int next,
            Map<String, int[]> plannedMoves,
            Set<String> plannedExits
    ) {
        int current = train.getCurrentSection();

        // Passenger has priority over freight near the shared junction.
        if (isFreightCrossingRoute(train) && hasPassengerCrossingActive()) {
            return false;
        }

        String occupant = sections.get(next);

        if (occupant != null && !occupant.equals(train.name)) {

            // Do not enter a section only because the current train is exiting in the same cycle.
            if (plannedExits.contains(occupant)) {
                return false;
            }

            // Allow only if the occupying train is already planned to move away.
            if (!plannedMoves.containsKey(occupant)) {
                return false;
            }
        }

        // Protect route 4 -> 3 if section 3 or 7 is occupied.
        if (current == 4 && next == 1 && train.destination == 3) {
            if (occupiedByOther(3, train.name) || occupiedByOther(7, train.name)) {
                return false;
            }
        }

        // Protect movement into section 3.
        if (current == 7 && next == 3) {
            if (occupiedByOther(3, train.name)) {
                return false;
            }
        }

        // Block passenger line 5 -> 6 if its final target is already occupied.
        if (current == 5 && next == 6) {
            if (train.destination == 8 && occupiedByOther(8, train.name)) {
                return false;
            }

            if (train.destination == 9 && occupiedByOther(9, train.name)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasPassengerCrossingActive() {
        for (Train train : activeTrains.values()) {
            if (train == null || !train.active) {
                continue;
            }

            Integer next = train.getNextSection();

            if (next == null) {
                continue;
            }

            int current = train.getCurrentSection();

            if ((current == 1 && next == 5) || (current == 6 && next == 2)) {
                return true;
            }
        }

        return false;
    }

    private boolean isFreightCrossingRoute(Train train) {
        if (train == null) {
            return false;
        }

        int current = train.getCurrentSection();
        Integer next = train.getNextSection();

        if (next == null) {
            return false;
        }

        return (current == 3 && next == 7)
                || (current == 7 && next == 6)
                || (current == 6 && next == 7)
                || (current == 7 && next == 3)
                || (current == 4 && next == 1);
    }

    private boolean occupiedByOther(int section, String selfName) {
        String occupant = sections.get(section);
        return occupant != null && !occupant.equals(selfName);
    }

    private int[] getRoute(int entry, int destination) {
        if (entry == destination) {
            return new int[]{entry};
        }

        // From section 1.
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // From section 3.
        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        // From section 4.
        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        // From section 9.
        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};

        // From section 10.
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        // From section 11.
        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    private void validateSection(int section) {
        if (!sections.containsKey(section)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }
}
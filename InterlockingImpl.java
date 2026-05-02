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

        // Remove duplicate train names but keep order.
        List<String> requested = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String name : trainNames) {
            if (name != null && seen.add(name)) {
                requested.add(name);
            }
        }

        // Passenger crossing moves get priority over freight crossing moves.
        requested.sort((a, b) -> {
            Train ta = activeTrains.get(a);
            Train tb = activeTrains.get(b);
            boolean pa = isPassengerCrossingMove(ta);
            boolean pb = isPassengerCrossingMove(tb);
            return Boolean.compare(pb, pa);
        });

        Set<String> requestedSet = new HashSet<>(requested);
        Map<String, int[]> plannedMoves = new LinkedHashMap<>();
        Set<String> plannedExits = new HashSet<>();
        Set<String> blocked = new HashSet<>();

        // Phase 1: plan moves and exits.
        for (String name : requested) {
            if (blocked.contains(name)) {
                continue;
            }

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();
            Integer next = train.getNextSection();

            // Train has reached final route section: exit on this call.
            if (next == null) {
                plannedExits.add(name);
                continue;
            }

            if (!canMove(train, next, plannedMoves, plannedExits, requestedSet)) {
                continue;
            }

            String conflictTrain = findPlannedConflict(train, current, next, plannedMoves);

            // Collision/deadlock: block both trains involved.
            if (conflictTrain != null) {
                plannedMoves.remove(conflictTrain);
                blocked.add(conflictTrain);
                blocked.add(name);
                continue;
            }

            plannedMoves.put(name, new int[]{current, next});
        }

        // Phase 2: apply exits first so following trains can enter vacated sections.
        for (String name : plannedExits) {
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

        // Phase 3: apply valid planned moves.
        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            String name = entry.getKey();
            Train train = activeTrains.get(name);

            if (train == null || !train.active || blocked.contains(name)) {
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
            Set<String> plannedExits,
            Set<String> requestedSet
    ) {
        int current = train.getCurrentSection();

        // Passenger priority: freight 3<->4 path must wait if passenger crossing is requested.
        if (isFreightCrossingRoute(train) && hasPassengerCrossingRequested(requestedSet)) {
            return false;
        }

        // If next section is occupied, it is only allowed if that train is moving/exiting this cycle.
        String occupyingTrainName = sections.get(next);
        if (occupyingTrainName != null && !occupyingTrainName.equals(train.name)) {
            if (!plannedMoves.containsKey(occupyingTrainName) && !plannedExits.contains(occupyingTrainName)) {
                return false;
            }
        }

        // Extra minimal blockers for section 3 path.
        if (current == 4 && next == 1 && train.destination == 3) {
            if (occupiedByOther(3, train.name) || occupiedByOther(7, train.name)) {
                return false;
            }
        }

        if (current == 7 && next == 3) {
            if (occupiedByOther(3, train.name)) {
                return false;
            }
        }

        return true;
    }

    private String findPlannedConflict(
            Train train,
            int current,
            int next,
            Map<String, int[]> plannedMoves
    ) {
        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            int otherFrom = entry.getValue()[0];
            int otherTo = entry.getValue()[1];

            // Same destination = local deadlock/collision risk.
            if (next == otherTo) {
                return entry.getKey();
            }

            // Direct swap = collision.
            if (current == otherTo && next == otherFrom) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean isPassengerCrossingMove(Train train) {
        if (train == null || !train.active) {
            return false;
        }

        Integer next = train.getNextSection();
        if (next == null) {
            return false;
        }

        int current = train.getCurrentSection();

        // Passenger crossing lines from the tutor note: 1 -> 5 and 6 -> 2.
        return (current == 1 && next == 5) || (current == 6 && next == 2);
    }

    private boolean hasPassengerCrossingRequested(Set<String> requestedSet) {
        for (String name : requestedSet) {
            Train train = activeTrains.get(name);
            if (isPassengerCrossingMove(train)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFreightCrossingRoute(Train train) {
        if (train == null) {
            return false;
        }

        // Freight line crossing passenger lines at 3, 4 and 7.
        return (train.entry == 3 && train.destination == 4)
                || (train.entry == 4 && train.destination == 3);
    }

    private boolean occupiedByOther(int section, String selfName) {
        String occupant = sections.get(section);
        return occupant != null && !occupant.equals(selfName);
    }

    private int[] getRoute(int entry, int destination) {
        // Self-stop routes.
        if (entry == destination) {
            return new int[]{entry};
        }

        // From 1.
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // From 3.
        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        // From 4.
        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        // From 9.
        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};

        // From 10.
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        // From 11.
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
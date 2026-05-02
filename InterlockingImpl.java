import java.util.*;

public class InterlockingImpl implements Interlocking {

    private static final int OUT_OF_SYSTEM = -1;

    private static class Train {
        String name;
        int entry;
        int destination;
        int[] route;
        int index = 0;
        boolean active = true;

        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
        }

        int current() {
            return route[index];
        }

        Integer next() {
            if (index >= route.length - 1) {
                return null;
            }
            return route[index + 1];
        }

        void move() {
            index++;
        }

        void exit() {
            active = false;
        }
    }

    private final Map<Integer, String> sections = new HashMap<>();
    private final Map<String, Train> trains = new HashMap<>();
    private final Set<String> allNames = new HashSet<>();

    public InterlockingImpl() {
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        if (allNames.contains(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        int[] route = getRoute(entryTrackSection, destinationTrackSection);

        if (route == null) {
            throw new IllegalArgumentException("Invalid route.");
        }

        Train train = new Train(trainName, entryTrackSection, destinationTrackSection, route);

        trains.put(trainName, train);
        allNames.add(trainName);
        sections.put(entryTrackSection, trainName);
    }

    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Train list cannot be null.");
        }

        List<String> requested = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String name : trainNames) {
            if (name != null && seen.add(name)) {
                requested.add(name);
            }
        }

        // Passenger-priority trains are planned first.
        requested.sort((a, b) -> {
            Train ta = trains.get(a);
            Train tb = trains.get(b);
            return Boolean.compare(isPassengerPriorityMove(tb), isPassengerPriorityMove(ta));
        });

        Set<String> requestedSet = new HashSet<>(requested);
        Map<String, int[]> plannedMoves = new LinkedHashMap<>();
        Set<String> plannedExits = new LinkedHashSet<>();
        Set<String> blocked = new HashSet<>();

        for (String name : requested) {
            Train train = trains.get(name);

            if (train == null || !train.active || blocked.contains(name)) {
                continue;
            }

            int current = train.current();
            Integer next = train.next();

            if (next == null) {
                plannedExits.add(name);
                continue;
            }

            if (!canMove(train, next, plannedMoves, plannedExits, requestedSet)) {
                continue;
            }

            String conflictTrain = findConflict(current, next, plannedMoves);

            if (conflictTrain != null) {
                Train other = trains.get(conflictTrain);

                // If both want section 7, passenger-priority movement wins.
                if (next == 7 && other != null) {
                    boolean currentPassenger = isPassengerPriorityMove(train);
                    boolean otherPassenger = isPassengerPriorityMove(other);

                    if (currentPassenger && !otherPassenger) {
                        plannedMoves.remove(conflictTrain);
                        blocked.add(conflictTrain);
                        plannedMoves.put(name, new int[]{current, next});
                        continue;
                    }

                    if (!currentPassenger && otherPassenger) {
                        blocked.add(name);
                        continue;
                    }
                }

                // Default collision/deadlock rule: same destination or swap blocks both.
                plannedMoves.remove(conflictTrain);
                blocked.add(conflictTrain);
                blocked.add(name);
                continue;
            }

            plannedMoves.put(name, new int[]{current, next});
        }

        int movedCount = 0;

        for (String name : plannedExits) {
            Train train = trains.get(name);

            if (train == null || !train.active || blocked.contains(name)) {
                continue;
            }

            sections.put(train.current(), null);
            train.exit();
            trains.remove(name);
            movedCount++;
        }

        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            String name = entry.getKey();

            if (blocked.contains(name)) {
                continue;
            }

            Train train = trains.get(name);

            if (train == null || !train.active) {
                continue;
            }

            int from = entry.getValue()[0];
            int to = entry.getValue()[1];

            sections.put(from, null);
            train.move();
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
        validateName(trainName);

        Train train = trains.get(trainName);

        if (train != null && train.active) {
            return train.current();
        }

        if (allNames.contains(trainName)) {
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
        if (isFreightCrossingMove(train) && hasPassengerPriorityMoveRequested(requestedSet, train.name)) {
            return false;
        }

        String occupant = sections.get(next);

        if (occupant != null && !occupant.equals(train.name)) {
            if (!plannedMoves.containsKey(occupant) && !plannedExits.contains(occupant)) {
                return false;
            }
        }

        return true;
    }

    private String findConflict(int from, int to, Map<String, int[]> plannedMoves) {
        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            int otherFrom = entry.getValue()[0];
            int otherTo = entry.getValue()[1];

            if (to == otherTo) {
                return entry.getKey();
            }

            if (from == otherTo && to == otherFrom) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean isPassengerPriorityMove(Train train) {
        if (train == null || !train.active) {
            return false;
        }

        Integer next = train.next();

        if (next == null) {
            return false;
        }

        int current = train.current();

        return (current == 1 && next == 5)
                || (current == 5 && next == 6)
                || (current == 6 && next == 2)
                || (current == 11 && next == 7)
                || (current == 7 && next == 3);
    }

    private boolean hasPassengerPriorityMoveRequested(Set<String> requestedSet, String selfName) {
        for (String name : requestedSet) {
            if (name.equals(selfName)) {
                continue;
            }

            Train train = trains.get(name);

            if (isPassengerPriorityMove(train)) {
                return true;
            }
        }

        return false;
    }

    private boolean isFreightCrossingMove(Train train) {
        if (train == null || !train.active) {
            return false;
        }

        Integer next = train.next();

        if (next == null) {
            return false;
        }

        int current = train.current();

        boolean freightRoute =
                (train.entry == 3 && train.destination == 4)
                || (train.entry == 4 && train.destination == 3);

        if (!freightRoute) {
            return false;
        }

        return (current == 3 && next == 7)
                || (current == 7 && next == 6)
                || (current == 6 && next == 5)
                || (current == 5 && next == 1)
                || (current == 1 && next == 4)
                || (current == 4 && next == 1)
                || (current == 1 && next == 5)
                || (current == 5 && next == 6)
                || (current == 6 && next == 7)
                || (current == 7 && next == 3);
    }

    private int[] getRoute(int entry, int destination) {
        if (entry == destination) {
            return new int[]{entry};
        }

        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    private void validateName(String trainName) {
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
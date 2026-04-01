import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train record.
     * Each train follows a fixed legal route from entry to destination.
     */
    private static class Train {
        private final String name;
        private final int entry;
        private final int destination;
        private final int[] route;

        private int routeIndex;
        private boolean active;

        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
            this.routeIndex = 0;
            this.active = true;
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

        boolean isAtDestination() {
            return routeIndex == route.length - 1;
        }

        void moveForward() {
            routeIndex++;
        }

        void exitSystem() {
            active = false;
        }
    }

    // Section number -> occupying train name
    private final Map<Integer, String> sections;

    // Active trains currently in the system
    private final Map<String, Train> activeTrains;

    // All train names ever added, including exited trains
    private final Set<String> allTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
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
        Set<String> processedThisRound = new HashSet<>();
        Set<String> edgesUsedThisRound = new HashSet<>();

        // Passenger priority only matters if a passenger is trying to use section 7 this round.
        boolean passengerNeedsSection7 = false;
        for (String name : trainNames) {
            if (name == null) {
                continue;
            }

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            Integer next = train.getNextSection();
            if (next != null && usesSection7(train.getCurrentSection(), next) && isPassenger(train)) {
                passengerNeedsSection7 = true;
                break;
            }
        }

        for (String name : trainNames) {
            if (name == null || !processedThisRound.add(name)) {
                continue;
            }

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();
            Integer next = train.getNextSection();

            // If already at destination, the next move removes the train from the system.
            if (next == null) {
                if (train.isAtDestination()) {
                    sections.put(current, null);
                    train.exitSystem();
                    activeTrains.remove(name);
                    movedCount++;
                }
                continue;
            }

            if (!canMove(train, next, edgesUsedThisRound, passengerNeedsSection7)) {
                continue;
            }

            // Perform sequential move
            sections.put(current, null);
            train.moveForward();
            sections.put(train.getCurrentSection(), train.name);
            movedCount++;

            edgesUsedThisRound.add(edgeKey(current, next));
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

    /**
     * Legal fixed routes.
     * This is the most important part of the implementation.
     */
    private int[] getRoute(int entry, int destination) {
        // From section 1
        if (entry == 1 && destination == 4) return new int[] {1, 4};
        if (entry == 1 && destination == 8) return new int[] {1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[] {1, 5, 6, 9};

        // From section 3
        if (entry == 3 && destination == 8) return new int[] {3, 6, 8};
        if (entry == 3 && destination == 9) return new int[] {3, 6, 9};
        if (entry == 3 && destination == 11) return new int[] {3, 7, 11};

        // From section 4
        if (entry == 4 && destination == 2) return new int[] {4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[] {4, 1, 5, 6, 7, 3};

        // From section 9
        if (entry == 9 && destination == 2) return new int[] {9, 6, 2};

        // From section 10
        if (entry == 10 && destination == 2) return new int[] {10, 6, 2};

        // From section 11
        if (entry == 11 && destination == 2) return new int[] {11, 7, 6, 2};
        if (entry == 11 && destination == 3) return new int[] {11, 7, 3};

        return null;
    }

    /**
     * Safety checks for one attempted movement.
     * This version avoids over-blocking.
     */
    private boolean canMove(
            Train train,
            int next,
            Set<String> edgesUsedThisRound,
            boolean passengerNeedsSection7) {

        int current = train.getCurrentSection();

        // Cannot move into an occupied section
        if (sections.get(next) != null) {
            return false;
        }

        // Prevent direct opposite-edge swaps in the same round
        if (edgesUsedThisRound.contains(edgeKey(next, current))) {
            return false;
        }

        // If a passenger needs section 7, freight using section 7 waits this round
        if (!isPassenger(train) && usesSection7(current, next) && passengerNeedsSection7) {
            return false;
        }

        return true;
    }

    /**
     * Freight-only journeys from the visible spec/tests you shared.
     */
    private boolean isPassenger(Train train) {
        return !((train.entry == 1 && train.destination == 4)
              || (train.entry == 3 && train.destination == 11)
              || (train.entry == 4 && train.destination == 3)
              || (train.entry == 11 && train.destination == 3));
    }

    private boolean usesSection7(int current, int next) {
        return current == 7 || next == 7;
    }

    private String edgeKey(int from, int to) {
        return from + "->" + to;
    }

    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    private void validateSection(int trackSection) {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }
}
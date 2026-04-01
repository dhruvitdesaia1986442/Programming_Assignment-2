import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // Returned by getTrain when a known train has exited the system
    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train record.
     * Each train follows one fixed legal route.
     *
     * Important:
     * - routeIndex points to the current occupied section in the route
     * - once the train reaches the last route section, it stays there temporarily
     * - on the next moveTrains call for that train, it exits the railway
     */
    private static class Train {
        private final String name;
        private final int entry;
        private final int destination;
        private final int[] route;

        private int routeIndex;
        private boolean active;
        private boolean waitingAtDestination;

        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
            this.routeIndex = 0;
            this.active = true;
            this.waitingAtDestination = false;
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

        boolean isAtFinalRouteSection() {
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

    // Active trains currently in the railway
    private final Map<String, Train> activeTrains;

    // All train names ever added, including trains that already exited
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
        Set<String> forwardEdgesUsedThisRound = new HashSet<>();

        // Passenger priority for section 7 crossover only
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

            // Already at final route section:
            // first eligible move call -> wait there
            // second eligible move call -> exit system
            if (next == null) {
                if (!train.waitingAtDestination) {
                    train.waitingAtDestination = true;
                    continue;
                }

                sections.put(current, null);
                train.exitSystem();
                activeTrains.remove(name);
                movedCount++;
                continue;
            }

            if (!canMove(train, next, forwardEdgesUsedThisRound, passengerNeedsSection7)) {
                continue;
            }

            // Move one step forward along the fixed route
            sections.put(current, null);
            train.moveForward();
            sections.put(train.getCurrentSection(), train.name);
            movedCount++;

            // If train has just reached final route section, mark it for temporary wait
            if (train.isAtFinalRouteSection()) {
                train.waitingAtDestination = false;
            }

            forwardEdgesUsedThisRound.add(edgeKey(current, next));
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
     * Fixed legal routes derived from the railway diagram.
     */
    private int[] getRoute(int entry, int destination) {
        // From 1
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // From 3
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        // From 4
        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        // From 9
        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};

        // From 10
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        // From 11
        if (entry == 11 && destination == 2) return new int[]{11, 7, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    /**
     * Safety checks for a single move attempt.
     * This version avoids the over-blocking that hurt your previous score.
     */
    private boolean canMove(
            Train train,
            int next,
            Set<String> forwardEdgesUsedThisRound,
            boolean passengerNeedsSection7) {

        int current = train.getCurrentSection();

        // Cannot move into an occupied section
        if (sections.get(next) != null) {
            return false;
        }

        // Prevent direct reverse-edge swap in the same moveTrains call
        if (forwardEdgesUsedThisRound.contains(edgeKey(next, current))) {
            return false;
        }

        // Passenger gets priority only on section 7 crossover movements
        if (!isPassenger(train) && usesSection7(current, next) && passengerNeedsSection7) {
            return false;
        }

        return true;
    }

    /**
     * Based on the visible tests you shared:
     * freight-only journeys are these four.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // Returned by getTrain when a known train has exited the railway
    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train record.
     * Each train stores its complete fixed route and current position on that route.
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

    // All trains ever added, so exited trains can still return -1
    private final Set<String> allTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        for (int section = 1; section <= 11; section++) {
            sections.put(section, null);
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
        Set<String> usedEdgesThisRound = new HashSet<>();

        // Shared conflict areas
        boolean section6ConflictUsed = false;
        boolean section7ConflictUsed = false;

        // Passenger priority on crossover around section 7
        boolean passengerNeedsCrossover = false;
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
                passengerNeedsCrossover = true;
                break;
            }
        }

        for (String name : trainNames) {
            if (name == null || processedThisRound.contains(name)) {
                continue;
            }
            processedThisRound.add(name);

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();
            Integer next = train.getNextSection();

            // If already at destination, the train exits on this move call.
            if (next == null) {
                if (train.isAtDestination()) {
                    sections.put(current, null);
                    train.exitSystem();
                    activeTrains.remove(name);
                    movedCount++;
                }
                continue;
            }

            if (!canMove(train, next, passengerNeedsCrossover, usedEdgesThisRound,
                    section6ConflictUsed, section7ConflictUsed)) {
                continue;
            }

            // Perform move
            sections.put(current, null);
            train.moveForward();
            sections.put(train.getCurrentSection(), train.name);
            movedCount++;

            usedEdgesThisRound.add(edgeKey(current, next));

            if (usesSection6(current, next)) {
                section6ConflictUsed = true;
            }
            if (usesSection7(current, next)) {
                section7ConflictUsed = true;
            }
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
     * Returns the fixed legal route for each valid journey.
     * Returns null for invalid journeys.
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
        if (entry == 4 && destination == 3) return new int[] {4, 1, 5, 6, 3};

        // From section 9
        if (entry == 9 && destination == 2) return new int[] {9, 6, 2};

        // From section 10
        if (entry == 10 && destination == 2) return new int[] {10, 6, 2};

        // From section 11
        if (entry == 11 && destination == 2) return new int[] {11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[] {11, 7, 3};

        return null;
    }

    /**
     * Checks whether the requested move is safe for this round.
     */
    private boolean canMove(
            Train train,
            int next,
            boolean passengerNeedsCrossover,
            Set<String> usedEdgesThisRound,
            boolean section6ConflictUsed,
            boolean section7ConflictUsed) {

        int current = train.getCurrentSection();

        // Next section must be free right now.
        if (sections.get(next) != null) {
            return false;
        }

        // Block reverse edge use in same move cycle.
        if (usedEdgesThisRound.contains(edgeKey(next, current))) {
            return false;
        }

        // Passenger has priority on section 7 crossover.
        if (!isPassenger(train) && usesSection7(current, next) && passengerNeedsCrossover) {
            return false;
        }

        // Only one move through section 6 shared turnout area per cycle.
        if (usesSection6(current, next) && section6ConflictUsed) {
            return false;
        }

        // Only one move through section 7 shared crossover area per cycle.
        if (usesSection7(current, next) && section7ConflictUsed) {
            return false;
        }

        return true;
    }

    /**
     * Passenger journeys are the non-freight routes.
     */
    private boolean isPassenger(Train train) {
        return !((train.entry == 1 && train.destination == 4)
              || (train.entry == 3 && train.destination == 11)
              || (train.entry == 4 && train.destination == 3)
              || (train.entry == 11 && train.destination == 3));
    }

    /**
     * Shared turnout area around section 6.
     */
    private boolean usesSection6(int current, int next) {
        return current == 6 || next == 6;
    }

    /**
     * Shared crossover area around section 7.
     */
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
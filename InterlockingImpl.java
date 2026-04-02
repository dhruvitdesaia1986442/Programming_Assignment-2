import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interlocking implementation.
 *
 * Rules used:
 * - each train follows one fixed legal route
 * - each section can contain at most one train
 * - in one moveTrains() call, safe moves are planned first, then applied
 * - a train already at destination exits on the next moveTrains() call
 * - movement is blocked only for real conflicts:
 *   1. next section is occupied
 *   2. two trains try to move into the same section
 *   3. two trains try to swap sections directly
 */
public class InterlockingImpl implements Interlocking {

    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train state.
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

        // Returns current occupied section
        int getCurrentSection() {
            return route[routeIndex];
        }

        // Returns next section in route, or null if already at destination
        Integer getNextSection() {
            if (routeIndex >= route.length - 1) {
                return null;
            }
            return route[routeIndex + 1];
        }

        // Move train one step forward in route
        void moveForward() {
            routeIndex++;
        }

        // Mark train as exited
        void exitSystem() {
            active = false;
        }
    }

    // section number -> train name occupying it, or null if empty
    private final Map<Integer, String> sections;

    // active train name -> Train object
    private final Map<String, Train> activeTrains;

    // all train names ever added, including exited trains
    private final Set<String> allTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        // Valid sections are 1 to 11
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    /**
     * Adds a train at the given entry section with the given destination.
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        // Train names must be unique
        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        // Entry section must be empty
        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        // Route must be valid
        int[] route = getRoute(entryTrackSection, destinationTrackSection);
        if (route == null) {
            throw new IllegalArgumentException("Invalid journey.");
        }

        Train train = new Train(trainName, entryTrackSection, destinationTrackSection, route);
        activeTrains.put(trainName, train);
        allTrainNames.add(trainName);
        sections.put(entryTrackSection, trainName);
    }

    /**
     * Moves listed trains by at most one section each.
     * Returns number of trains that moved or exited.
     */
    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Null train list.");
        }

        int movedCount = 0;

        // Prevent duplicate processing in one round
        Set<String> processedNames = new HashSet<>();

        // Planned moves for this round: train -> {from, to}
        Map<String, int[]> plannedMoves = new LinkedHashMap<>();

        // Phase 1: decide exits and safe moves
        for (String name : trainNames) {
            if (name == null || !processedNames.add(name)) {
                continue;
            }

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = train.getCurrentSection();
            Integer next = train.getNextSection();

            // If already at destination, exit on this call
            if (next == null) {
                sections.put(current, null);
                train.exitSystem();
                activeTrains.remove(name);
                movedCount++;
                continue;
            }

            // Otherwise, plan safe move
            if (canMove(train, next, plannedMoves)) {
                plannedMoves.put(name, new int[]{current, next});
            }
        }

        // Phase 2: apply planned moves
        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            String name = entry.getKey();
            int[] move = entry.getValue();

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = move[0];
            int next = move[1];

            sections.put(current, null);
            train.moveForward();
            sections.put(next, name);
            movedCount++;
        }

        return movedCount;
    }

    /**
     * Returns train name occupying a section, or null if empty.
     */
    @Override
    public String getSection(int trackSection) {
        validateSection(trackSection);
        return sections.get(trackSection);
    }

    /**
     * Returns current section of a train, or -1 if exited.
     */
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
     * Fixed legal routes for valid journeys.
     * Returns null if journey is invalid.
     */
    private int[] getRoute(int entry, int destination) {
        // Self-stop routes
        if (entry == 1 && destination == 1) return new int[]{1};
        if (entry == 3 && destination == 3) return new int[]{3};
        if (entry == 4 && destination == 4) return new int[]{4};
        if (entry == 5 && destination == 5) return new int[]{5};
        if (entry == 6 && destination == 6) return new int[]{6};
        if (entry == 7 && destination == 7) return new int[]{7};
        if (entry == 8 && destination == 8) return new int[]{8};
        if (entry == 9 && destination == 9) return new int[]{9};
        if (entry == 10 && destination == 10) return new int[]{10};
        if (entry == 11 && destination == 11) return new int[]{11};

        // From 1
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // From 3
        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
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
        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    /**
     * Checks whether a train can safely move to the next section.
     *
     * Rules:
     * 1. next section must be empty
     * 2. no two trains can move into same section in same round
     * 3. no direct swap between two sections in same round
     */
    private boolean canMove(Train train, int next, Map<String, int[]> plannedMoves) {
        int current = train.getCurrentSection();

        // Rule 1: next section must be empty
        if (sections.get(next) != null) {
            return false;
        }

        // Rule 2 and 3: conflict with already planned moves
        for (int[] other : plannedMoves.values()) {
            int otherFrom = other[0];
            int otherTo = other[1];

            // Same destination conflict
            if (next == otherTo) {
                return false;
            }

            // Direct swap conflict
            if (current == otherTo && next == otherFrom) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates train name input.
     */
    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    /**
     * Validates section number input.
     */
    private void validateSection(int trackSection) {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }
}
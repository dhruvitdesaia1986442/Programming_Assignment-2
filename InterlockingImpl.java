import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interlocking implementation.
 *
 * Main idea:
 * - Each train follows one fixed legal route.
 * - Each section can contain at most one train.
 * - In one moveTrains() call, we first plan safe moves and then apply them.
 * - A train already at destination exits on the next moveTrains() call that includes it.
 */
public class InterlockingImpl implements Interlocking {

    // Returned by getTrain() after a train has exited the system
    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train record.
     */
    private static class Train {
        private final String name;
        private final int entry;
        private final int destination;
        private final int[] route;

        // Current position inside the route array
        private int routeIndex;

        // True while the train is still inside the railway
        private boolean active;

        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
            this.routeIndex = 0;
            this.active = true;
        }

        // Current occupied section
        int getCurrentSection() {
            return route[routeIndex];
        }

        // Next section on the route, or null if already at destination
        Integer getNextSection() {
            if (routeIndex >= route.length - 1) {
                return null;
            }
            return route[routeIndex + 1];
        }

        // True if the train is currently at the final route section
        boolean isAtDestination() {
            return routeIndex == route.length - 1;
        }

        // Advance one step along the route
        void moveForward() {
            routeIndex++;
        }

        // Mark as exited
        void exitSystem() {
            active = false;
        }
    }

    // section number -> train name occupying it, or null if empty
    private final Map<Integer, String> sections;

    // active train name -> Train object
    private final Map<String, Train> activeTrains;

    // all trains ever added, including exited trains
    private final Set<String> allTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        // Railway sections are 1 to 11
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    /**
     * Add a train to the system.
     */
    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        // Train names must be unique across the whole simulation
        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        // Entry section must be free
        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        // Build the legal fixed route for this journey
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
     * Move the listed trains by at most one section each.
     * Returns the number of trains that actually moved or exited.
     */
    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Null train list.");
        }

        int movedCount = 0;

        // Prevent duplicate processing in one moveTrains() call
        Set<String> processedNames = new HashSet<>();

        // Planned safe moves for this round
        // value = {fromSection, toSection}
        Map<String, int[]> plannedMoves = new LinkedHashMap<>();

        // Phase 1: decide which trains can exit or move
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

            // Already at destination -> exit on this call
            if (next == null) {
                sections.put(current, null);
                train.exitSystem();
                activeTrains.remove(name);
                movedCount++;
                continue;
            }

            // Otherwise, check whether the move is safe
            if (canMove(train, next, plannedMoves)) {
                plannedMoves.put(name, new int[]{current, next});
            }
        }

        // Phase 2: apply all safe planned moves
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
     * Return the train name occupying a section, or null if empty.
     */
    @Override
    public String getSection(int trackSection) {
        validateSection(trackSection);
        return sections.get(trackSection);
    }

    /**
     * Return the current section of a train, or -1 if it has exited.
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
     * Returns null if the journey is invalid.
     */
    private int[] getRoute(int entry, int destination) {
        // Temporary/self-stop routes
        if (entry == 1 && destination == 1) return new int[]{1};
        if (entry == 3 && destination == 3) return new int[]{3};
        if (entry == 4 && destination == 4) return new int[]{4};
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
     * Checks whether a proposed move is safe.
     */
    private boolean canMove(Train train, int next, Map<String, int[]> plannedMoves) {
        int current = train.getCurrentSection();

        // Next section must be empty right now
        if (sections.get(next) != null) {
            return false;
        }

        // Check against already planned moves in this round
        for (int[] other : plannedMoves.values()) {
            if (conflicts(current, next, other[0], other[1])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Two moves conflict if:
     * - both enter the same destination section
     * - they directly swap sections
     */
    private boolean conflicts(int from1, int to1, int from2, int to2) {
        // Same destination
        if (to1 == to2) {
            return true;
        }

        // Direct swap
        if (from1 == to2 && from2 == to1) {
            return true;
        }

        return false;
    }

    /**
     * Validate train name.
     */
    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    /**
     * Validate section number.
     */
    private void validateSection(int trackSection) {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }
}
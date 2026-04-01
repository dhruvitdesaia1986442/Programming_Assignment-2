import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // Returned when a train has already exited the railway
    private static final int OUT_OF_SYSTEM = -1;

    /**
     * Internal train object.
     * Each train has:
     * - a name
     * - an entry section
     * - a destination section
     * - a fixed legal route
     * - a current position on that route
     * - an active flag to show whether it is still in the railway
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

        // Returns the current section occupied by this train
        int getCurrentSection() {
            return route[routeIndex];
        }

        // Returns the next section in the route, or null if already at destination
        Integer getNextSection() {
            if (routeIndex >= route.length - 1) {
                return null;
            }
            return route[routeIndex + 1];
        }

        // Checks whether the train is currently at the final section of its route
        boolean isAtDestination() {
            return routeIndex == route.length - 1;
        }

        // Moves the train forward by one step in its route
        void moveForward() {
            routeIndex++;
        }

        // Marks the train as no longer active in the railway
        void exitSystem() {
            active = false;
        }
    }

    // Maps each section number to the train currently occupying it
    private final Map<Integer, String> sections;

    // Stores all trains still active in the railway
    private final Map<String, Train> activeTrains;

    // Stores all train names ever added, including exited trains
    private final Set<String> allTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        // Initialise sections 1 to 11 as empty
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        // Validate train name and section numbers
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        // Reject duplicate train names
        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        // Entry section must be empty before adding the train
        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        // Get the legal route for this journey
        int[] route = getRoute(entryTrackSection, destinationTrackSection);

        // If no route exists, the journey is invalid
        if (route == null) {
            throw new IllegalArgumentException("Invalid journey.");
        }

        // Create and store the train
        Train train = new Train(trainName, entryTrackSection, destinationTrackSection, route);
        activeTrains.put(trainName, train);
        allTrainNames.add(trainName);
        sections.put(entryTrackSection, trainName);
    }

    @Override
    public int moveTrains(String[] trainNames) {
        // Null input is invalid
        if (trainNames == null) {
            throw new IllegalArgumentException("Null train list.");
        }

        int movedCount = 0;

        // Prevent duplicate processing of the same name in one call
        Set<String> processedNames = new HashSet<>();

        // Stores planned moves first, then applies them after checking conflicts
        Map<String, int[]> plannedMoves = new LinkedHashMap<>();

        // Detect whether any passenger train needs section 7 in this round
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

        // First phase: decide which trains can move or exit
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

            // If already at destination, remove train from railway
            if (next == null) {
                sections.put(current, null);
                train.exitSystem();
                activeTrains.remove(name);
                movedCount++;
                continue;
            }

            // Otherwise, check whether the move is safe
            if (canMove(train, next, plannedMoves, passengerNeedsSection7)) {
                plannedMoves.put(name, new int[]{current, next});
            }
        }

        // Second phase: apply all safe planned moves
        for (Map.Entry<String, int[]> entry : plannedMoves.entrySet()) {
            String name = entry.getKey();
            int[] move = entry.getValue();

            Train train = activeTrains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            int current = move[0];
            int next = move[1];

            // Clear old section and occupy new section
            sections.put(current, null);
            train.moveForward();
            sections.put(next, name);
            movedCount++;
        }

        return movedCount;
    }

    @Override
    public String getSection(int trackSection) {
        // Validate input section
        validateSection(trackSection);

        // Return occupying train name or null
        return sections.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) {
        // Validate train name
        validateTrainName(trainName);

        // If still active, return its current section
        Train train = activeTrains.get(trainName);
        if (train != null) {
            return train.getCurrentSection();
        }

        // If it existed before but has exited, return -1
        if (allTrainNames.contains(trainName)) {
            return OUT_OF_SYSTEM;
        }

        // Otherwise it is unknown
        throw new IllegalArgumentException("Unknown train.");
    }

    /**
     * Returns the fixed legal route for each valid journey.
     * If the journey is invalid, returns null.
     */
    private int[] getRoute(int entry, int destination) {
        // Same-section temporary stop routes
        if (entry == 1 && destination == 1) return new int[]{1};
        if (entry == 3 && destination == 3) return new int[]{3};
        if (entry == 4 && destination == 4) return new int[]{4};
        if (entry == 9 && destination == 9) return new int[]{9};
        if (entry == 10 && destination == 10) return new int[]{10};
        if (entry == 11 && destination == 11) return new int[]{11};

        // Routes starting from section 1
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // Routes starting from section 3
        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        // Routes starting from section 4
        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        // Routes starting from section 9
        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};

        // Routes starting from section 10
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        // Routes starting from section 11
        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    /**
     * Checks whether a train can move safely.
     * Conditions checked:
     * - next section must be empty
     * - freight yields to passenger on section 7
     * - move must not conflict with already planned moves
     */
    private boolean canMove(Train train, int next, Map<String, int[]> plannedMoves, boolean passengerNeedsSection7) {
        int current = train.getCurrentSection();

        // Cannot move into an occupied section
        if (sections.get(next) != null) {
            return false;
        }

        // Freight yields if passenger needs section 7 this round
        if (!isPassenger(train) && usesSection7(current, next) && passengerNeedsSection7) {
            return false;
        }

        // Check all already planned moves for conflicts
        for (int[] otherMove : plannedMoves.values()) {
            int otherFrom = otherMove[0];
            int otherTo = otherMove[1];

            if (conflicts(current, next, otherFrom, otherTo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if two moves conflict.
     */
    private boolean conflicts(int from1, int to1, int from2, int to2) {
        // Two trains cannot move into the same destination section
        if (to1 == to2) {
            return true;
        }

        // Prevent direct swap in opposite directions
        if (from1 == to2 && from2 == to1) {
            return true;
        }

        // Prevent two trains entering section 6 at same time
        if (to1 == 6 && to2 == 6) {
            return true;
        }

        // Prevent two trains entering section 7 at same time
        if (to1 == 7 && to2 == 7) {
            return true;
        }

        return false;
    }

    /**
     * Defines which journeys are treated as passenger journeys.
     * Everything else is freight.
     */
    private boolean isPassenger(Train train) {
        return !((train.entry == 1 && train.destination == 4)
              || (train.entry == 3 && train.destination == 11)
              || (train.entry == 4 && train.destination == 3)
              || (train.entry == 11 && train.destination == 3));
    }

    // Returns true if a move uses section 7
    private boolean usesSection7(int current, int next) {
        return current == 7 || next == 7;
    }

    // Validates train name input
    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    // Validates section number input
    private void validateSection(int trackSection) {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }
}
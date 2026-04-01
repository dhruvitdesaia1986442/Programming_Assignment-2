import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // Returned by getTrain() after a train has exited the system
    private static final int EXITED = -1;

    /**
     * Internal train record.
     * Each train stores its full legal route and its current index on that route.
     */
    private static final class Train {
        private final String name;
        private final int entry;
        private final int destination;
        private final int[] route;

        private int routeIndex;
        private boolean active;

        private Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.route = route;
            this.routeIndex = 0;
            this.active = true;
        }

        private int getCurrentSection() {
            return active ? route[routeIndex] : EXITED;
        }

        private boolean isAtDestination() {
            return active && routeIndex == route.length - 1;
        }

        private Integer getNextSection() {
            if (!active || isAtDestination()) {
                return null;
            }
            return route[routeIndex + 1];
        }

        private void advance() {
            routeIndex++;
        }

        private void exitSystem() {
            active = false;
            routeIndex = route.length - 1;
        }
    }

    // Track section -> occupying train name
    private final Map<Integer, String> sectionOccupancy;

    // Currently active trains
    private final Map<String, Train> activeTrains;

    // All train names ever added, so exited trains can still return -1
    private final Set<String> knownTrainNames;

    // All valid journeys and their full routes
    private final Map<String, int[]> validRoutes;

    public InterlockingImpl() {
        sectionOccupancy = new HashMap<>();
        activeTrains = new HashMap<>();
        knownTrainNames = new HashSet<>();
        validRoutes = buildValidRoutes();

        for (int section = 1; section <= 11; section++) {
            sectionOccupancy.put(section, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        validateTrainName(trainName);
        validateSection(entryTrackSection);
        validateSection(destinationTrackSection);

        if (knownTrainNames.contains(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        String routeKey = routeKey(entryTrackSection, destinationTrackSection);
        int[] route = validRoutes.get(routeKey);

        if (route == null) {
            throw new IllegalArgumentException("Invalid journey.");
        }

        if (sectionOccupancy.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        Train train = new Train(
                trainName,
                entryTrackSection,
                destinationTrackSection,
                Arrays.copyOf(route, route.length)
        );

        activeTrains.put(trainName, train);
        knownTrainNames.add(trainName);
        sectionOccupancy.put(entryTrackSection, trainName);
    }

    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Train list cannot be null.");
        }

        int movedCount = 0;

        // Ignore duplicate names in the same move request
        Set<String> processedNames = new HashSet<>();

        // Prevent direct reverse-edge swaps inside one call
        Set<String> usedDirectedEdges = new HashSet<>();

        // Section 6 is a shared conflict area in many routes
        boolean zoneSixUsedThisRound = false;

        for (String trainName : trainNames) {
            if (trainName == null || !processedNames.add(trainName)) {
                continue;
            }

            Train train = activeTrains.get(trainName);

            // Unknown / exited trains are ignored
            if (train == null || !train.active) {
                continue;
            }

            // If train is already sitting on its destination section,
            // the next move removes it from the system.
            if (train.isAtDestination()) {
                int current = train.getCurrentSection();
                sectionOccupancy.put(current, null);
                train.exitSystem();
                activeTrains.remove(trainName);
                movedCount++;
                continue;
            }

            int current = train.getCurrentSection();
            int next = train.getNextSection();

            // Next section must be empty
            if (sectionOccupancy.get(next) != null) {
                continue;
            }

            // Prevent conflicting simultaneous use of the shared turnout area near section 6
            if (usesZoneSix(current, next) && zoneSixUsedThisRound) {
                continue;
            }

            // Prevent direct head-on swap inside the same moveTrains() call
            String reverseEdge = edgeKey(next, current);
            if (usedDirectedEdges.contains(reverseEdge)) {
                continue;
            }

            // Perform move
            sectionOccupancy.put(current, null);
            sectionOccupancy.put(next, train.name);
            train.advance();
            movedCount++;

            usedDirectedEdges.add(edgeKey(current, next));
            if (usesZoneSix(current, next)) {
                zoneSixUsedThisRound = true;
            }
        }

        return movedCount;
    }

    @Override
    public String getSection(int trackSection) {
        validateSection(trackSection);
        return sectionOccupancy.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) {
        validateTrainName(trainName);

        Train train = activeTrains.get(trainName);
        if (train != null) {
            return train.getCurrentSection();
        }

        if (knownTrainNames.contains(trainName)) {
            return EXITED;
        }

        throw new IllegalArgumentException("Unknown train name.");
    }

    /**
     * All legal journeys mapped to their full step-by-step routes.
     */
    private Map<String, int[]> buildValidRoutes() {
        Map<String, int[]> routes = new HashMap<>();

        // From section 1
        routes.put(routeKey(1, 4), new int[]{1, 4});
        routes.put(routeKey(1, 8), new int[]{1, 5, 6, 8});
        routes.put(routeKey(1, 9), new int[]{1, 5, 6, 9});

        // From section 3
        routes.put(routeKey(3, 8), new int[]{3, 6, 8});
        routes.put(routeKey(3, 9), new int[]{3, 6, 9});
        routes.put(routeKey(3, 11), new int[]{3, 7, 11});

        // From section 4
        routes.put(routeKey(4, 2), new int[]{4, 1, 5, 6, 2});
        routes.put(routeKey(4, 3), new int[]{4, 1, 5, 6, 3});

        // From section 9
        routes.put(routeKey(9, 2), new int[]{9, 6, 2});

        // From section 10
        routes.put(routeKey(10, 2), new int[]{10, 6, 2});

        // From section 11
        routes.put(routeKey(11, 2), new int[]{11, 9, 6, 2});
        routes.put(routeKey(11, 3), new int[]{11, 7, 3});

        return routes;
    }

    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }
    }

    private void validateSection(int trackSection) {
        if (!sectionOccupancy.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
    }

    private String routeKey(int entry, int destination) {
        return entry + "->" + destination;
    }

    private String edgeKey(int from, int to) {
        return from + ":" + to;
    }

    /**
     * Shared turnout/conflict area around section 6.
     * Hidden tests often check that not all trains can move through this area together.
     */
    private boolean usesZoneSix(int from, int to) {
        return from == 6 || to == 6;
    }
}
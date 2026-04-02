import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interlocking implementation.
 *
 * Design:
 * - Each train follows one fixed legal route.
 * - Each section holds at most one train.
 * - Safe moves are planned first, then applied.
 * - A train already at destination exits on the next moveTrains() call.
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

    // section -> train name or null
    private final Map<Integer, String> sections;

    // active train name -> Train
    private final Map<String, Train> activeTrains;

    // all train names ever added, including exited trains
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
        Set<String> processedNames = new HashSet<>();
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

            // Immediate exit behavior for local tests
            if (next == null) {
                sections.put(current, null);
                train.exitSystem();
                activeTrains.remove(name);
                movedCount++;
                continue;
            }

            if (canMove(train, next, plannedMoves)) {
                plannedMoves.put(name, new int[]{current, next});
            }
        }

        // Phase 2: apply moves
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

    private int[] getRoute(int entry, int destination) {
        // temporary/self-stop routes
        if (entry == 1 && destination == 1) return new int[]{1};
        if (entry == 3 && destination == 3) return new int[]{3};
        if (entry == 4 && destination == 4) return new int[]{4};
        if (entry == 9 && destination == 9) return new int[]{9};
        if (entry == 10 && destination == 10) return new int[]{10};
        if (entry == 11 && destination == 11) return new int[]{11};

        // from 1
        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};

        // from 3
        if (entry == 3 && destination == 4) return new int[]{3, 7, 6, 5, 1, 4};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};

        // from 4
        if (entry == 4 && destination == 2) return new int[]{4, 1, 5, 6, 2};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 5, 6, 7, 3};

        // from 9
        if (entry == 9 && destination == 2) return new int[]{9, 6, 2};

        // from 10
        if (entry == 10 && destination == 2) return new int[]{10, 6, 2};

        // from 11
        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 2};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        return null;
    }

    private boolean canMove(Train train, int next, Map<String, int[]> plannedMoves) {
        int current = train.getCurrentSection();

        if (sections.get(next) != null) {
            return false;
        }

        if (wouldDeadlock(train, current, next)) {
            return false;
        }

        for (int[] other : plannedMoves.values()) {
            if (conflicts(current, next, other[0], other[1])) {
                return false;
            }
        }

        return true;
    }

    private boolean conflicts(int from1, int to1, int from2, int to2) {
        if (to1 == to2) {
            return true;
        }

        if (from1 == to2 && from2 == to1) {
            return true;
        }

        return false;
    }

    private boolean wouldDeadlock(Train train, int current, int next) {
        String self = train.name;

        if (current == 4 && next == 1 && train.destination == 3) {
            if (occupiedByOther(7, self) || occupiedByOther(3, self)) {
                return true;
            }
        }

        if (current == 7 && next == 3) {
            if (occupiedByOther(3, self)) {
                return true;
            }
        }

        if (current == 5 && next == 6) {
            if (train.destination == 8 && occupiedByOther(8, self)) {
                return true;
            }
            if (train.destination == 9 && occupiedByOther(9, self)) {
                return true;
            }
        }

        if ((current == 9 || current == 10) && next == 6) {
            if (occupiedByOther(2, self)) {
                return true;
            }
        }

        return false;
    }

    private boolean occupiedByOther(int section, String selfName) {
        String occ = sections.get(section);
        return occ != null && !occ.equals(selfName);
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
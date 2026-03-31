import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class InterlockingImpl implements Interlocking {

    // -----------------------------
    // STEP 1: Train type definition
    // -----------------------------
    private enum TrainType {
        PASSENGER,
        FREIGHT
    }

    // -----------------------------
    // STEP 2: Direction definition
    // -----------------------------
    private enum Direction {
        SOUTH,
        NORTH
    }

    // -----------------------------
    // STEP 3: Internal Train class
    // Stores train details and route
    // -----------------------------
    private static class Train {
        private final String name;
        private final TrainType type;
        private final Direction direction;
        private final List<Integer> route;
        private int index;

        Train(String name, TrainType type, Direction direction, List<Integer> route) {
            this.name = name;
            this.type = type;
            this.direction = direction;
            this.route = route;
            this.index = 0;
        }

        int currentSection() {
            return route.get(index);
        }

        boolean hasNext() {
            return index < route.size() - 1;
        }

        int nextSection() {
            return route.get(index + 1);
        }

        void advance() {
            index++;
        }
    }

    // -----------------------------
    // STEP 4: Track section occupancy
    // section number -> train name
    // -----------------------------
    private final Map<Integer, String> sections;

    // -----------------------------
    // STEP 5: Active trains storage
    // train name -> train object
    // -----------------------------
    private final Map<String, Train> trains;

    // -----------------------------
    // STEP 6: Passenger priority tracking
    // Used at crossover junction
    // -----------------------------
    private final Set<String> passengerWaitingAtCrossover;

    // -----------------------------
    // STEP 7: Constructor
    // Initialise all 11 sections as empty
    // -----------------------------
    public InterlockingImpl() {
        sections = new HashMap<Integer, String>();
        trains = new HashMap<String, Train>();
        passengerWaitingAtCrossover = new TreeSet<String>();

        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    // -----------------------------
    // STEP 8: Add train into system
    // Includes all validations
    // -----------------------------
    @Override
    public void addTrain(String trainName, String trainType, int entrySection, String direction) {
        validateTrainName(trainName);

        TrainType type = parseTrainType(trainType);
        Direction dir = parseDirection(direction);

        List<Integer> route = getRoute(type, dir, entrySection);
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException("Invalid entry section for train type and direction.");
        }

        int startSection = route.get(0);

        // -----------------------------
        // STEP 9: Entry section occupancy check
        // -----------------------------
        if (sections.get(startSection) != null) {
            throw new IllegalStateException("Entry section is already occupied.");
        }

        Train train = new Train(trainName, type, dir, route);
        trains.put(trainName, train);
        sections.put(startSection, trainName);
    }

    // -----------------------------
    // STEP 10: Move one train forward
    // -----------------------------
    @Override
    public boolean moveTrain(String trainName) {
        Train train = getTrain(trainName);

        // -----------------------------
        // STEP 11: Exit logic
        // Remove train when route is complete
        // -----------------------------
        if (!train.hasNext()) {
            sections.put(train.currentSection(), null);
            trains.remove(trainName);
            passengerWaitingAtCrossover.remove(trainName);
            return true;
        }

        int current = train.currentSection();
        int next = train.nextSection();

        // -----------------------------
        // STEP 12: Passenger priority detection
        // -----------------------------
        if (isPassengerCrossoverMove(train, current, next)) {
            passengerWaitingAtCrossover.add(train.name);
        }

        // -----------------------------
        // STEP 13: Safety check before moving
        // -----------------------------
        if (!canMove(train, current, next)) {
            return false;
        }

        // -----------------------------
        // STEP 14: Perform train movement
        // -----------------------------
        sections.put(current, null);
        train.advance();
        sections.put(train.currentSection(), train.name);

        if (!isApproachingCrossover(train)) {
            passengerWaitingAtCrossover.remove(train.name);
        }

        return true;
    }

    // -----------------------------
    // STEP 15: Move all trains
    // Passenger trains get priority
    // -----------------------------
    @Override
    public List<String> moveAllTrains() {
        List<Train> ordered = new ArrayList<Train>(trains.values());

        // -----------------------------
        // STEP 16: Sort trains for priority
        // -----------------------------
        ordered.sort((a, b) -> {
            boolean aPriority = isApproachingCrossover(a) && a.type == TrainType.PASSENGER;
            boolean bPriority = isApproachingCrossover(b) && b.type == TrainType.PASSENGER;

            if (aPriority && !bPriority) {
                return -1;
            }
            if (!aPriority && bPriority) {
                return 1;
            }
            if (a.type != b.type) {
                return a.type == TrainType.PASSENGER ? -1 : 1;
            }
            return a.name.compareTo(b.name);
        });

        List<String> moved = new ArrayList<String>();
        for (Train train : ordered) {
            if (trains.containsKey(train.name) && moveTrain(train.name)) {
                moved.add(train.name);
            }
        }
        return moved;
    }

    // -----------------------------
    // STEP 17: Get train section
    // -----------------------------
    @Override
    public int getTrainSection(String trainName) {
        return getTrain(trainName).currentSection();
    }

    // -----------------------------
    // STEP 18: Get section occupancy
    // -----------------------------
    @Override
    public Map<Integer, String> getSectionOccupancy() {
        return new TreeMap<Integer, String>(sections);
    }

    // -----------------------------
    // STEP 19: Get active trains
    // -----------------------------
    @Override
    public Set<String> getActiveTrains() {
        return new TreeSet<String>(trains.keySet());
    }

    // -----------------------------
    // STEP 20: Train name validation
    // -----------------------------
    private void validateTrainName(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Train name cannot be empty.");
        }
        if (trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }
    }

    // -----------------------------
    // STEP 21: Get train helper
    // -----------------------------
    private Train getTrain(String trainName) {
        Train train = trains.get(trainName);
        if (train == null) {
            throw new IllegalArgumentException("Train not found.");
        }
        return train;
    }

    // -----------------------------
    // STEP 22: Parse train type
    // -----------------------------
    private TrainType parseTrainType(String trainType) {
        if (trainType == null) {
            throw new IllegalArgumentException("Train type cannot be null.");
        }
        if ("passenger".equalsIgnoreCase(trainType)) {
            return TrainType.PASSENGER;
        }
        if ("freight".equalsIgnoreCase(trainType)) {
            return TrainType.FREIGHT;
        }
        throw new IllegalArgumentException("Invalid train type.");
    }

    // -----------------------------
    // STEP 23: Parse direction
    // -----------------------------
    private Direction parseDirection(String direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
        if ("south".equalsIgnoreCase(direction)) {
            return Direction.SOUTH;
        }
        if ("north".equalsIgnoreCase(direction)) {
            return Direction.NORTH;
        }
        throw new IllegalArgumentException("Invalid direction.");
    }

    // -----------------------------
    // STEP 24: Main safety logic
    // -----------------------------
    private boolean canMove(Train train, int current, int next) {

        // -----------------------------
        // STEP 25: Collision prevention
        // Train cannot enter occupied section
        // -----------------------------
        if (sections.get(next) != null) {
            return false;
        }

        // -----------------------------
        // STEP 26: Passenger priority at crossover
        // Freight must wait
        // -----------------------------
        if (isFreightCrossoverMove(train, current, next) && hasPassengerWaitingAtCrossover()) {
            return false;
        }

        // -----------------------------
        // STEP 27: Prevent simple head-on swap
        // -----------------------------
        if (wouldSwapHeadOn(current, next, train.name)) {
            return false;
        }

        return true;
    }

    // -----------------------------
    // STEP 28: Check waiting passenger
    // -----------------------------
    private boolean hasPassengerWaitingAtCrossover() {
        return !passengerWaitingAtCrossover.isEmpty();
    }

    // -----------------------------
    // STEP 29: Passenger crossover move
    // -----------------------------
    private boolean isPassengerCrossoverMove(Train train, int current, int next) {
        return train.type == TrainType.PASSENGER && isCrossoverEdge(current, next);
    }

    // -----------------------------
    // STEP 30: Freight crossover move
    // -----------------------------
    private boolean isFreightCrossoverMove(Train train, int current, int next) {
        return train.type == TrainType.FREIGHT && isCrossoverEdge(current, next);
    }

    // -----------------------------
    // STEP 31: Approaching crossover
    // -----------------------------
    private boolean isApproachingCrossover(Train train) {
        if (!train.hasNext()) {
            return false;
        }
        return train.type == TrainType.PASSENGER
                && isCrossoverEdge(train.currentSection(), train.nextSection());
    }

    // -----------------------------
    // STEP 32: Crossover edge definition
    // -----------------------------
    private boolean isCrossoverEdge(int current, int next) {
        return (current == 3 && next == 7) || (current == 7 && next == 3);
    }

    // -----------------------------
    // STEP 33: Head-on swap prevention
    // -----------------------------
    private boolean wouldSwapHeadOn(int current, int next, String movingTrainName) {
        for (Train other : trains.values()) {
            if (other.name.equals(movingTrainName)) {
                continue;
            }
            if (other.hasNext()) {
                int otherCurrent = other.currentSection();
                int otherNext = other.nextSection();
                if (otherCurrent == next && otherNext == current) {
                    return true;
                }
            }
        }
        return false;
    }

    // -----------------------------
    // STEP 34: ROUTES (IMPORTANT)
    // Defines legal train paths only
    // -----------------------------
    private List<Integer> getRoute(TrainType type, Direction direction, int entrySection) {
        if (type == TrainType.FREIGHT) {
            return getFreightRoute(direction, entrySection);
        }
        return getPassengerRoute(direction, entrySection);
    }

    // -----------------------------
    // STEP 35: Freight train routes
    // -----------------------------
    private List<Integer> getFreightRoute(Direction direction, int entrySection) {
        if (direction == Direction.SOUTH) {
            if (entrySection == 3) {
                return Arrays.asList(3, 7, 11);
            }
            if (entrySection == 1) {
                return Arrays.asList(1, 4);
            }
        } else {
            if (entrySection == 11) {
                return Arrays.asList(11, 7, 3);
            }
            if (entrySection == 4) {
                return Arrays.asList(4, 1);
            }
        }
        return null;
    }

    // -----------------------------
    // STEP 36: Passenger train routes
    // -----------------------------
    private List<Integer> getPassengerRoute(Direction direction, int entrySection) {
        if (direction == Direction.SOUTH) {
            if (entrySection == 1) {
                return Arrays.asList(1, 5, 6, 8);
            }
            if (entrySection == 3) {
                return Arrays.asList(3, 6, 9);
            }
        } else {
            if (entrySection == 9) {
                return Arrays.asList(9, 6, 5, 2);
            }
            if (entrySection == 10) {
                return Arrays.asList(10, 6, 5, 2);
            }
            if (entrySection == 11) {
                return Arrays.asList(11, 9, 6, 5, 2);
            }
            if (entrySection == 4) {
                return Arrays.asList(4, 1, 2);
            }
        }
        return null;
    }

    // -----------------------------
    // STEP 37: AI usage note
    // Used AI to understand Petri-net ideas,
    // code structure, and testing strategy
    // -----------------------------
}
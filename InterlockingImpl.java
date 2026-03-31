import java.util.*;

public class InterlockingImpl implements Interlocking {

    enum TrainType { PASSENGER, FREIGHT }
    enum Direction { SOUTH, NORTH }

    class Train {
        String name;
        TrainType type;
        Direction direction;
        List<Integer> route;
        int index = 0;

        Train(String name, TrainType type, Direction direction, List<Integer> route) {
            this.name = name;
            this.type = type;
            this.direction = direction;
            this.route = route;
        }

        int current() {
            return route.get(index);
        }

        boolean hasNext() {
            return index < route.size() - 1;
        }

        int next() {
            return route.get(index + 1);
        }

        void move() {
            index++;
        }
    }

    private Map<Integer, String> sections = new HashMap<>();
    private Map<String, Train> trains = new HashMap<>();
    private Set<String> passengerPriority = new HashSet<>();

    public InterlockingImpl() {
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    // -----------------------------
    // ADD TRAIN
    // -----------------------------
    public void addTrain(String trainName, String trainType, int entrySection, String direction) {
        if (trains.containsKey(trainName))
            throw new IllegalArgumentException("Duplicate train");

        TrainType type = parseType(trainType);
        Direction dir = parseDirection(direction);

        List<Integer> route = getRoute(type, dir, entrySection);
        if (route == null)
            throw new IllegalArgumentException("Invalid entry");

        if (sections.get(route.get(0)) != null)
            throw new IllegalStateException("Section occupied");

        Train t = new Train(trainName, type, dir, route);
        trains.put(trainName, t);
        sections.put(route.get(0), trainName);
    }

    // -----------------------------
    // MOVE ONE TRAIN
    // -----------------------------
    public boolean moveTrain(String trainName) {
        Train t = getTrain(trainName);

        if (!t.hasNext()) {
            sections.put(t.current(), null);
            trains.remove(trainName);
            return true;
        }

        int next = t.next();

        // collision prevention
        if (sections.get(next) != null)
            return false;

        // passenger priority at crossover (3 <-> 7)
        if ((t.current() == 3 && next == 7) || (t.current() == 7 && next == 3)) {
            if (t.type == TrainType.FREIGHT && hasPassengerWaiting())
                return false;
            if (t.type == TrainType.PASSENGER)
                passengerPriority.add(t.name);
        }

        // perform move
        sections.put(t.current(), null);
        t.move();
        sections.put(t.current(), t.name);

        passengerPriority.remove(t.name);

        return true;
    }

    // -----------------------------
    // MOVE ALL TRAINS
    // -----------------------------
    public List<String> moveAllTrains() {
        List<String> moved = new ArrayList<>();

        List<Train> list = new ArrayList<>(trains.values());

        // priority sorting
        list.sort((a, b) -> {
            if (a.type != b.type)
                return a.type == TrainType.PASSENGER ? -1 : 1;
            return a.name.compareTo(b.name);
        });

        for (Train t : list) {
            if (trains.containsKey(t.name) && moveTrain(t.name)) {
                moved.add(t.name);
            }
        }

        return moved;
    }

    // -----------------------------
    // GETTERS
    // -----------------------------
    public int getTrainSection(String trainName) {
        return getTrain(trainName).current();
    }

    public Map<Integer, String> getSectionOccupancy() {
        return new TreeMap<>(sections);
    }

    public Set<String> getActiveTrains() {
        return new TreeSet<>(trains.keySet());
    }

    // -----------------------------
    // HELPERS
    // -----------------------------
    private Train getTrain(String name) {
        if (!trains.containsKey(name))
            throw new IllegalArgumentException("Train not found");
        return trains.get(name);
    }

    private TrainType parseType(String t) {
        if (t.equalsIgnoreCase("passenger")) return TrainType.PASSENGER;
        if (t.equalsIgnoreCase("freight")) return TrainType.FREIGHT;
        throw new IllegalArgumentException("Invalid type");
    }

    private Direction parseDirection(String d) {
        if (d.equalsIgnoreCase("south")) return Direction.SOUTH;
        if (d.equalsIgnoreCase("north")) return Direction.NORTH;
        throw new IllegalArgumentException("Invalid direction");
    }

    private boolean hasPassengerWaiting() {
        return !passengerPriority.isEmpty();
    }

    // -----------------------------
    // ROUTES (IMPORTANT)
    // -----------------------------
    private List<Integer> getRoute(TrainType type, Direction dir, int entry) {

        if (type == TrainType.FREIGHT) {
            if (dir == Direction.SOUTH) {
                if (entry == 3) return Arrays.asList(3, 7, 11);
                if (entry == 1) return Arrays.asList(1, 4);
            } else {
                if (entry == 11) return Arrays.asList(11, 7, 3);
                if (entry == 4) return Arrays.asList(4, 1);
            }
        }

        if (type == TrainType.PASSENGER) {
            if (dir == Direction.SOUTH) {
                if (entry == 1) return Arrays.asList(1, 5, 6, 8);
                if (entry == 3) return Arrays.asList(3, 6, 9);
            } else {
                if (entry == 9) return Arrays.asList(9, 6, 5, 2);
                if (entry == 10) return Arrays.asList(10, 6, 5, 2);
                if (entry == 11) return Arrays.asList(11, 9, 6, 5, 2);
                if (entry == 4) return Arrays.asList(4, 1, 2);
            }
        }

        return null;
    }
}
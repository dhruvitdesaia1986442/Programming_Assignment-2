import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // "Constant indicating train has exited system"
    private static final int OUT_OF_SYSTEM = -1;

    // "Train class stores state and route information"
    private static class Train {
        String name;                 // "Train name"
        int entry;                  // "Entry section"
        int destination;            // "Destination section"
        int currentSection;         // "Current position"
        int[] route;                // "Route path"
        int routeIndex;             // "Current index in route"
        boolean active;             // "Is train active"

        // "Constructor initializes train properties"
        Train(String name, int entry, int destination, int[] route) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.currentSection = entry;
            this.route = route;
            this.routeIndex = 0;
            this.active = true;
        }
    }

    // "Map storing section occupancy"
    private final Map<Integer, String> sections;

    // "Active trains in system"
    private final Map<String, Train> activeTrains;

    // "All trains ever added (used to track exited trains)"
    private final Set<String> allTrainNames;

    // "Constructor initializes empty track sections"
    public InterlockingImpl() {
        sections = new HashMap<>();
        activeTrains = new HashMap<>();
        allTrainNames = new HashSet<>();

        // "Initialize sections 1 to 11 as empty"
        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    // "Add train to system"
    @Override
    public void addTrain(String trainName, int entry, int destination) {

        // "Validate train name"
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException();
        }

        // "Validate sections"
        if (!sections.containsKey(entry) || !sections.containsKey(destination)) {
            throw new IllegalArgumentException();
        }

        // "Reject duplicate trains"
        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
            throw new IllegalArgumentException();
        }

        // "Check if entry section is occupied"
        if (sections.get(entry) != null) {
            throw new IllegalStateException();
        }

        // "Get valid route"
        int[] route = chooseRoute(entry, destination);
        if (route == null) {
            throw new IllegalArgumentException();
        }

        // "Create train and store"
        Train train = new Train(trainName, entry, destination, route);
        activeTrains.put(trainName, train);
        allTrainNames.add(trainName);

        // "Mark section occupied"
        sections.put(entry, trainName);
    }

    // "Move trains forward"
    @Override
    public int moveTrains(String[] trainNames) {

        // "Validate input"
        if (trainNames == null) {
            throw new IllegalArgumentException();
        }

        int moved = 0;

        // "Track already moved trains"
        Set<String> movedThisRound = new HashSet<>();

        // "Check passenger priority at crossover"
        boolean passengerPriority = false;
        for (String name : trainNames) {
            Train t = activeTrains.get(name);
            if (t != null && hasNext(t)) {
                if (isPassenger(t) && isCrossover(t)) {
                    passengerPriority = true;
                    break;
                }
            }
        }

        for (String name : trainNames) {

            // "Skip invalid or duplicate entries"
            if (name == null || movedThisRound.contains(name)) continue;

            Train t = activeTrains.get(name);

            // "Skip if train not active"
            if (t == null || !t.active) continue;

            // "Exit logic"
            if (!hasNext(t)) {
                sections.put(t.currentSection, null);
                t.currentSection = OUT_OF_SYSTEM;
                t.active = false;
                activeTrains.remove(name);

                moved++;
                movedThisRound.add(name);
                continue;
            }

            int current = t.currentSection;
            int next = nextSection(t);

            // "Check if movement is safe"
            if (!canMove(t, current, next, passengerPriority)) continue;

            // "Perform move"
            sections.put(current, null);
            t.routeIndex++;
            t.currentSection = next;
            sections.put(next, t.name);

            moved++;
            movedThisRound.add(name);
        }

        return moved;
    }

    // "Get section occupancy"
    @Override
    public String getSection(int section) {
        if (!sections.containsKey(section)) {
            throw new IllegalArgumentException();
        }
        return sections.get(section);
    }

    // "Get train position"
    @Override
    public int getTrain(String trainName) {

        // "Validate train name"
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException();
        }

        Train t = activeTrains.get(trainName);

        // "If train is active"
        if (t != null) return t.currentSection;

        // "If train exited"
        if (allTrainNames.contains(trainName)) return OUT_OF_SYSTEM;

        // "Unknown train"
        throw new IllegalArgumentException();
    }

    // -----------------------------
    // "Helper methods"
    // -----------------------------

    // "Check if train has next section"
    private boolean hasNext(Train t) {
        return t.routeIndex < t.route.length - 1;
    }

    // "Get next section"
    private int nextSection(Train t) {
        return t.route[t.routeIndex + 1];
    }

    // "Check if train is passenger"
    private boolean isPassenger(Train t) {
        return !(t.entry == 1 && t.destination == 4
              || t.entry == 3 && t.destination == 11
              || t.entry == 4 && t.destination == 3
              || t.entry == 11 && t.destination == 3);
    }

    // "Check crossover movement"
    private boolean isCrossover(Train t) {
        int current = t.currentSection;
        int next = nextSection(t);
        return (current == 3 && next == 7) || (current == 7 && next == 3);
    }

    // "Safety rules for movement"
    private boolean canMove(Train t, int current, int next, boolean passengerPriority) {

        // "Prevent collision"
        if (sections.get(next) != null) return false;

        // "Passenger priority over freight"
        if (!isPassenger(t) && isCrossover(t) && passengerPriority) return false;

        // "Prevent head-on swap"
        for (Train other : activeTrains.values()) {
            if (other == t || !hasNext(other)) continue;

            if (other.currentSection == next &&
                nextSection(other) == current) {
                return false;
            }
        }

        // "Prevent turnout conflict (section 6)"
        if (isTurnout(current, next)) {
            for (Train other : activeTrains.values()) {
                if (other == t || !hasNext(other)) continue;

                int oCurrent = other.currentSection;
                int oNext = nextSection(other);

                if (isTurnout(oCurrent, oNext)) {
                    return false;
                }
            }
        }

        return true;
    }

    // "Check turnout movement"
    private boolean isTurnout(int current, int next) {
        return (current == 6 && (next == 8 || next == 9))
                || ((current == 8 || current == 9) && next == 6)
                || (current == 10 && next == 6)
                || (current == 6 && next == 10);
    }

    // "Route selection logic"
    private int[] chooseRoute(int entry, int destination) {

        if (entry == 1 && destination == 4) return new int[]{1, 4};
        if (entry == 3 && destination == 11) return new int[]{3, 7, 11};
        if (entry == 4 && destination == 3) return new int[]{4, 1, 3};
        if (entry == 11 && destination == 3) return new int[]{11, 7, 3};

        if (entry == 1 && destination == 8) return new int[]{1, 5, 6, 8};
        if (entry == 1 && destination == 9) return new int[]{1, 5, 6, 9};
        if (entry == 3 && destination == 8) return new int[]{3, 6, 8};
        if (entry == 3 && destination == 9) return new int[]{3, 6, 9};
        if (entry == 9 && destination == 2) return new int[]{9, 6, 5, 2};
        if (entry == 10 && destination == 2) return new int[]{10, 6, 5, 2};
        if (entry == 11 && destination == 2) return new int[]{11, 9, 6, 5, 2};
        if (entry == 4 && destination == 2) return new int[]{4, 1, 2};

        return null;
    }
}
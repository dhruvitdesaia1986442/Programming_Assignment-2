import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    // Returned by getTrain when a known train has exited
    private static final int OUT_OF_SYSTEM = -1;

    // Internal train record
    private static class Train {
        String name;
        int entry;
        int destination;
        int currentSection;
        boolean active;

        Train(String name, int entry, int destination) {
            this.name = name;
            this.entry = entry;
            this.destination = destination;
            this.currentSection = entry;
            this.active = true;
        }
    }

    // Section -> occupying train name
    private final Map<Integer, String> sections;

    // Active trains currently in the system
    private final Map<String, Train> activeTrains;

    // All train names ever added, so exited trains can return -1
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
        // Validate train name
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }

        // Validate section numbers
        if (!sections.containsKey(entryTrackSection) || !sections.containsKey(destinationTrackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }

        // Reject duplicate names
        if (allTrainNames.contains(trainName) || activeTrains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }

        // Entry section must be empty
        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section occupied.");
        }

        // Entry/destination pair must be legal
        if (!isValidJourney(entryTrackSection, destinationTrackSection)) {
            throw new IllegalArgumentException("Invalid journey.");
        }

        Train train = new Train(trainName, entryTrackSection, destinationTrackSection);
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
        Set<String> movedThisRound = new HashSet<>();

        // Passenger on crossover gets priority over freight on crossover
        boolean passengerNeedsCrossover = false;
        for (String name : trainNames) {
            Train t = activeTrains.get(name);
            if (t != null && t.active) {
                Integer next = chooseNextSection(t);
                if (next != null && isPassenger(t) && isCrossoverEdge(t.currentSection, next)) {
                    passengerNeedsCrossover = true;
                    break;
                }
            }
        }

        for (String name : trainNames) {
            if (name == null || movedThisRound.contains(name)) {
                continue;
            }

            Train t = activeTrains.get(name);
            if (t == null || !t.active) {
                continue;
            }

            Integer next = chooseNextSection(t);

            // If already at destination, remove from system
            if (next == null) {
                if (t.currentSection == t.destination) {
                    sections.put(t.currentSection, null);
                    t.currentSection = OUT_OF_SYSTEM;
                    t.active = false;
                    activeTrains.remove(name);
                    movedCount++;
                    movedThisRound.add(name);
                }
                continue;
            }

            if (!canMove(t, next, passengerNeedsCrossover)) {
                continue;
            }

            sections.put(t.currentSection, null);
            t.currentSection = next;
            sections.put(next, t.name);

            movedCount++;
            movedThisRound.add(name);
        }

        return movedCount;
    }

    @Override
    public String getSection(int trackSection) {
        if (!sections.containsKey(trackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
        return sections.get(trackSection);
    }

    @Override
    public int getTrain(String trainName) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid train name.");
        }

        Train t = activeTrains.get(trainName);
        if (t != null) {
            return t.currentSection;
        }

        if (allTrainNames.contains(trainName)) {
            return OUT_OF_SYSTEM;
        }

        throw new IllegalArgumentException("Unknown train.");
    }

    // Allowed entry/destination pairs
    private boolean isValidJourney(int entry, int destination) {
        return (entry == 1 && destination == 4)
            || (entry == 1 && destination == 8)
            || (entry == 1 && destination == 9)
            || (entry == 3 && destination == 8)
            || (entry == 3 && destination == 9)
            || (entry == 3 && destination == 11)
            || (entry == 4 && destination == 2)
            || (entry == 4 && destination == 3)
            || (entry == 9 && destination == 2)
            || (entry == 10 && destination == 2)
            || (entry == 11 && destination == 2)
            || (entry == 11 && destination == 3);
    }

    // Freight-only journeys
    private boolean isPassenger(Train t) {
        return !((t.entry == 1 && t.destination == 4)
              || (t.entry == 3 && t.destination == 11)
              || (t.entry == 4 && t.destination == 3)
              || (t.entry == 11 && t.destination == 3));
    }

    // Choose the next section dynamically, one transition at a time
    private Integer chooseNextSection(Train t) {
        int current = t.currentSection;
        int destination = t.destination;

        if (current == destination) {
            return null;
        }

        switch (current) {
            case 1:
                if (destination == 4) return 4;
                if (destination == 8 || destination == 9) return 5;
                break;

            case 3:
                if (destination == 11) return 7;
                if (destination == 8 || destination == 9) return 6;
                break;

            case 4:
                if (destination == 2 || destination == 3) return 1;
                break;

            case 5:
                return 6;

            case 6:
                if (destination == 8) return 8;
                if (destination == 9) return 9;
                if (destination == 2) return 5;
                break;

            case 7:
                if (destination == 11) return 11;
                if (destination == 3) return 3;
                break;

            case 9:
                if (destination == 2) return 6;
                break;

            case 10:
                if (destination == 2) return 6;
                break;

            case 11:
                if (destination == 2) return 9;
                if (destination == 3) return 7;
                break;

            default:
                break;
        }

        return null;
    }

    // Safety rules
    private boolean canMove(Train t, int next, boolean passengerNeedsCrossover) {
        int current = t.currentSection;

        // Next section must be empty
        if (sections.get(next) != null) {
            return false;
        }

        // Freight waits if passenger needs the crossover
        if (!isPassenger(t) && isCrossoverEdge(current, next) && passengerNeedsCrossover) {
            return false;
        }

        // Prevent direct head-on swap
        for (Train other : activeTrains.values()) {
            if (other == t || !other.active) {
                continue;
            }

            Integer otherNext = chooseNextSection(other);
            if (otherNext == null) {
                continue;
            }

            if (other.currentSection == next && otherNext == current) {
                return false;
            }
        }

        // Prevent simultaneous turnout conflicts near section 6
        if (isTurnoutEdge(current, next)) {
            for (Train other : activeTrains.values()) {
                if (other == t || !other.active) {
                    continue;
                }

                Integer otherNext = chooseNextSection(other);
                if (otherNext == null) {
                    continue;
                }

                if (isTurnoutEdge(other.currentSection, otherNext)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Freight crossover edge
    private boolean isCrossoverEdge(int current, int next) {
        return (current == 3 && next == 7) || (current == 7 && next == 3);
    }

    // Turnout area edges around section 6
    private boolean isTurnoutEdge(int current, int next) {
        return (current == 6 && (next == 8 || next == 9))
            || ((current == 8 || current == 9) && next == 6)
            || (current == 10 && next == 6)
            || (current == 6 && next == 10)
            || (current == 9 && next == 6)
            || (current == 5 && next == 6)
            || (current == 6 && next == 5);
    }
}
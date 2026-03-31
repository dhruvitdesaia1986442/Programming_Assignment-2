import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterlockingImpl implements Interlocking {

    private static final int OUT_OF_SYSTEM = -1;

    private static class Train {
        String name;
        int entry;
        int destination;
        int currentSection;
        int[] route;
        int routeIndex;
        boolean active;

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

    private final Map<Integer, String> sections;
    private final Map<String, Train> trains;
    private final Set<String> knownTrainNames;

    public InterlockingImpl() {
        sections = new HashMap<Integer, String>();
        trains = new HashMap<String, Train>();
        knownTrainNames = new HashSet<String>();

        for (int i = 1; i <= 11; i++) {
            sections.put(i, null);
        }
    }

    @Override
    public void addTrain(String trainName, int entryTrackSection, int destinationTrackSection) {
        if (trainName == null || trainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Train name cannot be empty.");
        }
        if (knownTrainNames.contains(trainName) || trains.containsKey(trainName)) {
            throw new IllegalArgumentException("Duplicate train name.");
        }
        if (!sections.containsKey(entryTrackSection) || !sections.containsKey(destinationTrackSection)) {
            throw new IllegalArgumentException("Invalid section.");
        }
        if (sections.get(entryTrackSection) != null) {
            throw new IllegalStateException("Entry section is occupied.");
        }

        int[] route = chooseRoute(entryTrackSection, destinationTrackSection);
        if (route == null) {
            throw new IllegalArgumentException("Invalid route.");
        }

        Train train = new Train(trainName, entryTrackSection, destinationTrackSection, route);
        trains.put(trainName, train);
        knownTrainNames.add(trainName);
        sections.put(entryTrackSection, trainName);
    }

    @Override
    public int moveTrains(String[] trainNames) {
        if (trainNames == null) {
            throw new IllegalArgumentException("Train list cannot be null.");
        }

        int movedCount = 0;
        Set<String> movedThisRound = new HashSet<String>();

        boolean passengerNeedsCrossover = false;
        for (String name : trainNames) {
            Train t = trains.get(name);
            if (t != null && t.active && hasNext(t)) {
                int next = nextSection(t);
                if (isPassengerTrain(t) && isCrossoverMove(t.currentSection, next)) {
                    passengerNeedsCrossover = true;
                    break;
                }
            }
        }

        for (String name : trainNames) {
            if (name == null || movedThisRound.contains(name)) {
                continue;
            }

            Train train = trains.get(name);
            if (train == null || !train.active) {
                continue;
            }

            if (!hasNext(train)) {
                sections.put(train.currentSection, null);
                train.currentSection = OUT_OF_SYSTEM;
                train.active = false;
                trains.remove(name);
                movedCount++;
                movedThisRound.add(name);
                continue;
            }

            int current = train.currentSection;
            int next = nextSection(train);

            if (!canMove(train, current, next, passengerNeedsCrossover)) {
                continue;
            }

            sections.put(current, null);
            train.routeIndex++;
            train.currentSection = next;
            sections.put(next, train.name);

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
            throw new IllegalArgumentException("Unknown train.");
        }

        Train train = trains.get(trainName);
        if (train != null) {
            return train.currentSection;
        }

        if (knownTrainNames.contains(trainName)) {
            return OUT_OF_SYSTEM;
        }

        throw new IllegalArgumentException("Unknown train.");
    }

    private boolean hasNext(Train train) {
        return train.routeIndex < train.route.length - 1;
    }

    private int nextSection(Train train) {
        return train.route[train.routeIndex + 1];
    }

    private boolean isPassengerTrain(Train train) {
        return !((train.entry == 1 && train.destination == 4)
                || (train.entry == 3 && train.destination == 11)
                || (train.entry == 4 && train.destination == 3)
                || (train.entry == 11 && train.destination == 3));
    }

    private boolean canMove(Train train, int current, int next, boolean passengerNeedsCrossover) {
        if (sections.get(next) != null) {
            return false;
        }

        if (!isPassengerTrain(train) && isCrossoverMove(current, next) && passengerNeedsCrossover) {
            return false;
        }

        for (Train other : trains.values()) {
            if (other == train || !other.active || !hasNext(other)) {
                continue;
            }
            int otherCurrent = other.currentSection;
            int otherNext = nextSection(other);
            if (otherCurrent == next && otherNext == current) {
                return false;
            }
        }

        if (isTurnoutMove(current, next)) {
            for (Train other : trains.values()) {
                if (other == train || !other.active || !hasNext(other)) {
                    continue;
                }
                int otherCurrent = other.currentSection;
                int otherNext = nextSection(other);
                if (isTurnoutMove(otherCurrent, otherNext)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isCrossoverMove(int current, int next) {
        return (current == 3 && next == 7) || (current == 7 && next == 3);
    }

    private boolean isTurnoutMove(int current, int next) {
        return (current == 6 && (next == 8 || next == 9))
                || ((current == 8 || current == 9) && next == 6)
                || (current == 10 && next == 6)
                || (current == 6 && next == 10);
    }

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
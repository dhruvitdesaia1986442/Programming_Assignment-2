public interface Interlocking {

    // Add a train into the system
    void addTrain(String trainName, String trainType, int entrySection, String direction);

    // Move one train forward (one section)
    boolean moveTrain(String trainName);

    // Move all trains (used for simulation)
    java.util.List<String> moveAllTrains();

    // Get current section of a train
    int getTrainSection(String trainName);

    // Get full track occupancy
    java.util.Map<Integer, String> getSectionOccupancy();

    // Get all active trains
    java.util.Set<String> getActiveTrains();
}
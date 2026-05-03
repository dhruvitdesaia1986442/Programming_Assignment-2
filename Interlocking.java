public interface Interlocking {

    // Adds a train to the railway with a valid starting section and destination section
    void addTrain(String trainName, int entryTrackSection, int destinationTrackSection);

    // Moves the listed trains by at most one section each and returns how many moved
    int moveTrains(String[] trainNames);

    // Returns the train name currently in the given section, or null if empty
    String getSection(int trackSection);

    // Returns the current section of the train, or -1 if it has exited
    int getTrain(String trainName);
}
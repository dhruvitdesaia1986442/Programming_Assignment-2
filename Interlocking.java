public interface Interlocking {

    // Add a train with a valid entry and destination section
    void addTrain(String trainName, int entryTrackSection, int destinationTrackSection);

    // Move the listed trains by at most one section each
    int moveTrains(String[] trainNames);

    // Return the train currently occupying a section, or null
    String getSection(int trackSection);

    // Return current train section, or -1 if it exited
    int getTrain(String trainName);
}
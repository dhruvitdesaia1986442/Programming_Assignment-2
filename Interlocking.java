public interface Interlocking {

    // "This method adds a train into the railway system"
    // "trainName → unique name of the train"
    // "entryTrackSection → starting section of the train"
    // "destinationTrackSection → final section where train exits"
    void addTrain(String trainName, int entryTrackSection, int destinationTrackSection);

    // "This method moves given trains one step forward"
    // "trainNames → array of train names to be moved"
    // "Returns number of trains successfully moved"
    int moveTrains(String[] trainNames);

    // "This method returns which train is occupying a section"
    // "trackSection → section number (1–11)"
    // "Returns train name OR null if empty"
    String getSection(int trackSection);

    // "This method returns the current section of a train"
    // "trainName → name of the train"
    // "Returns section number OR -1 if train has exited"
    int getTrain(String trainName);
}
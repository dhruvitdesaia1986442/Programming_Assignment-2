public interface Interlocking {

    void addTrain(String trainName, int entryTrackSection, int destinationTrackSection);

    int moveTrains(String[] trainNames);

    String getSection(int trackSection);

    int getTrain(String trainName);
}
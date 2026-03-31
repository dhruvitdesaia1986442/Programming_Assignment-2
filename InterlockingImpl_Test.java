import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class InterlockingImpl_Test {

    @Test
    public void testAddTrain() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 1, "south");

        assertEquals(1, interlocking.getTrainSection("T1"));
    }

    @Test
    public void testGetSectionOccupancy() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 1, "south");

        Map<Integer, String> occupancy = interlocking.getSectionOccupancy();
        assertEquals("T1", occupancy.get(1));
    }

    @Test
    public void testMoveTrainForward() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 1, "south");

        boolean moved = interlocking.moveTrain("T1");

        assertTrue(moved);
        assertEquals(5, interlocking.getTrainSection("T1"));
    }

    @Test
    public void testDuplicateTrainNameRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 1, "south");

        try {
            interlocking.addTrain("T1", "freight", 3, "south");
            fail("Expected duplicate train name error.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testOccupiedEntryRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 1, "south");

        try {
            interlocking.addTrain("T2", "freight", 1, "south");
            fail("Expected occupied entry error.");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testInvalidTrainTypeRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", "invalidType", 1, "south");
            fail("Expected invalid train type error.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidDirectionRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", "passenger", 1, "east");
            fail("Expected invalid direction error.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testUnknownTrainRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.getTrainSection("UNKNOWN");
            fail("Expected unknown train error.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testTrainExitRemovesTrain() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", "freight", 1, "south");

        assertTrue(interlocking.moveTrain("F1")); // 1 -> 4
        assertTrue(interlocking.moveTrain("F1")); // exit

        Set<String> active = interlocking.getActiveTrains();
        assertFalse(active.contains("F1"));
    }

    @Test
    public void testMoveAllTrains() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");
        interlocking.addTrain("F1", "freight", 3, "south");

        List<String> moved = interlocking.moveAllTrains();
        assertEquals(2, moved.size());
    }
}
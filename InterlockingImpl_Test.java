import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
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
            assertTrue(true);
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
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidTrainTypeRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", "invalidType", 1, "south");
            fail("Expected invalid train type error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidDirectionRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", "passenger", 1, "east");
            fail("Expected invalid direction error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testUnknownTrainRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.getTrainSection("UNKNOWN");
            fail("Expected unknown train error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testTrainExitRemovesTrain() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", "freight", 1, "south");

        assertTrue(interlocking.moveTrain("F1"));
        assertTrue(interlocking.moveTrain("F1"));

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

    @Test
    public void testNullTrainTypeRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", null, 1, "south");
            fail("Expected null train type error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNullDirectionRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", "passenger", 1, null);
            fail("Expected null direction error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEmptyTrainNameRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("", "passenger", 1, "south");
            fail("Expected empty train name error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNorthboundPassengerFromNine() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 9, "north");

        assertEquals(9, interlocking.getTrainSection("T1"));
        assertTrue(interlocking.moveTrain("T1"));
        assertEquals(6, interlocking.getTrainSection("T1"));
    }

    @Test
    public void testNorthboundPassengerFromTen() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 10, "north");

        assertEquals(10, interlocking.getTrainSection("T1"));
        assertTrue(interlocking.moveTrain("T1"));
        assertEquals(6, interlocking.getTrainSection("T1"));
    }

    @Test
    public void testNorthboundPassengerFromEleven() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", "passenger", 11, "north");

        assertEquals(11, interlocking.getTrainSection("T1"));
        assertTrue(interlocking.moveTrain("T1"));
        assertEquals(9, interlocking.getTrainSection("T1"));
    }

    @Test
    public void testNorthboundFreightFromEleven() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", "freight", 11, "north");

        assertEquals(11, interlocking.getTrainSection("F1"));
        assertTrue(interlocking.moveTrain("F1"));
        assertEquals(7, interlocking.getTrainSection("F1"));
    }

    @Test
    public void testNorthboundFreightFromFour() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", "freight", 4, "north");

        assertEquals(4, interlocking.getTrainSection("F1"));
        assertTrue(interlocking.moveTrain("F1"));
        assertEquals(1, interlocking.getTrainSection("F1"));
    }

    @Test
    public void testInvalidFreightEntryRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("F1", "freight", 9, "south");
            fail("Expected invalid freight entry error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidPassengerEntryRejected() {
        InterlockingImpl interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("P1", "passenger", 2, "south");
            fail("Expected invalid passenger entry error.");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testBlockedMoveBecauseNextSectionOccupied() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");
        interlocking.addTrain("P2", "passenger", 3, "south");

        assertTrue(interlocking.moveTrain("P1"));
        assertTrue(interlocking.moveTrain("P2"));

        assertFalse(interlocking.moveTrain("P1"));
        assertEquals(5, interlocking.getTrainSection("P1"));
    }

    @Test
    public void testMoveAllTrainsWithSingleTrain() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");

        List<String> moved = interlocking.moveAllTrains();
        assertEquals(1, moved.size());
        assertTrue(moved.contains("P1"));
    }

    @Test
    public void testPassengerEventuallyExitsSystem() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");

        assertTrue(interlocking.moveTrain("P1"));
        assertTrue(interlocking.moveTrain("P1"));
        assertTrue(interlocking.moveTrain("P1"));
        assertTrue(interlocking.moveTrain("P1"));

        assertFalse(interlocking.getActiveTrains().contains("P1"));
        assertNull(interlocking.getSectionOccupancy().get(8));
    }

    @Test
    public void testFreightEventuallyExitsSystemNorthbound() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", "freight", 11, "north");

        assertTrue(interlocking.moveTrain("F1"));
        assertTrue(interlocking.moveTrain("F1"));
        assertTrue(interlocking.moveTrain("F1"));

        assertFalse(interlocking.getActiveTrains().contains("F1"));
        assertNull(interlocking.getSectionOccupancy().get(3));
    }

    @Test
    public void testGetActiveTrainsAfterAdd() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");
        interlocking.addTrain("F1", "freight", 3, "south");

        Set<String> active = interlocking.getActiveTrains();
        assertTrue(active.contains("P1"));
        assertTrue(active.contains("F1"));
        assertEquals(2, active.size());
    }

    @Test
    public void testSectionOccupancyAfterMultipleMoves() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 1, "south");

        interlocking.moveTrain("P1");
        interlocking.moveTrain("P1");

        Map<Integer, String> occupancy = interlocking.getSectionOccupancy();
        assertNull(occupancy.get(1));
        assertNull(occupancy.get(5));
        assertEquals("P1", occupancy.get(6));
    }

    @Test
    public void testFreightBlockedByPassengerPriorityAtCrossover() {
        InterlockingImpl interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", "passenger", 11, "north");
        interlocking.addTrain("F1", "freight", 3, "south");

        assertTrue(interlocking.moveTrain("P1"));
        assertFalse(interlocking.moveTrain("F1"));
        assertEquals(3, interlocking.getTrainSection("F1"));
    }
}
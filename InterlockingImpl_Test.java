import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingImpl_Test {

    @Test
    public void testAddTrainAndGetTrain() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);
        assertEquals(1, i.getTrain("T1"));
        assertEquals("T1", i.getSection(1));
    }

    @Test
    public void testMoveSingleTrain() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        assertEquals(1, i.moveTrains(new String[]{"T1"}));
        assertEquals(4, i.getTrain("T1"));
        assertNull(i.getSection(1));
        assertEquals("T1", i.getSection(4));
    }

    @Test
    public void testExitSystem() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        assertEquals(1, i.moveTrains(new String[]{"T1"}));
        assertEquals(1, i.moveTrains(new String[]{"T1"}));
        assertEquals(-1, i.getTrain("T1"));
        assertNull(i.getSection(4));
    }

    @Test
    public void testDuplicateTrainRejected() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        try {
            i.addTrain("T1", 3, 11);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testOccupiedEntryRejected() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        try {
            i.addTrain("T2", 1, 8);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidRouteRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.addTrain("T1", 1, 2);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMoveTrainsNullRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.moveTrains(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidSectionRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.getSection(99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testUnknownTrainRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.getTrain("UNKNOWN");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMoveIgnoresUnknownTrainName() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        assertEquals(0, i.moveTrains(new String[]{"UNKNOWN"}));
        assertEquals(1, i.getTrain("T1"));
    }

    @Test
    public void testMoveDuplicateNamesOnlyOnce() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        assertEquals(1, i.moveTrains(new String[]{"T1", "T1"}));
        assertEquals(4, i.getTrain("T1"));
    }

    @Test
    public void testExitedTrainNoLongerMoves() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        i.moveTrains(new String[]{"T1"});
        i.moveTrains(new String[]{"T1"});

        assertEquals(0, i.moveTrains(new String[]{"T1"}));
    }

    @Test
    public void testOneToEightRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 1, 8);

        i.moveTrains(new String[]{"P1"});
        assertEquals(5, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(8, i.getTrain("P1"));
    }

    @Test
    public void testThreeToElevenRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 3, 11);

        i.moveTrains(new String[]{"F1"});
        assertEquals(7, i.getTrain("F1"));

        i.moveTrains(new String[]{"F1"});
        assertEquals(11, i.getTrain("F1"));
    }

    @Test
    public void testFourToThreeRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 4, 3);

        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});

        assertEquals(3, i.getTrain("F1"));
    }

    @Test
    public void testElevenToTwoRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 11, 2);

        i.moveTrains(new String[]{"P1"});
        assertEquals(9, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(2, i.getTrain("P1"));
    }

    @Test
    public void testHeadOnSwapBlocked() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 3, 11);
        i.addTrain("B", 11, 3);

        i.moveTrains(new String[]{"A"});
        int moved = i.moveTrains(new String[]{"A", "B"});

        assertTrue(moved <= 1);
    }
}
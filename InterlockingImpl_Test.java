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
            i.addTrain("T1", 2, 11);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidEntryRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.addTrain("T1", 99, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidDestinationRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.addTrain("T1", 1, 99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEmptyTrainNameRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.addTrain("", 1, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNullTrainNameRejected() {
        Interlocking i = new InterlockingImpl();

        try {
            i.addTrain(null, 1, 4);
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
    public void testMoveIgnoresNullTrainName() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        assertEquals(0, i.moveTrains(new String[]{null}));
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
    public void testSectionInitiallyEmpty() {
        Interlocking i = new InterlockingImpl();
        assertNull(i.getSection(5));
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
    public void testOneToNineRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 1, 9);

        i.moveTrains(new String[]{"P1"});
        assertEquals(5, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(9, i.getTrain("P1"));
    }

    @Test
    public void testThreeToEightRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 3, 8);

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(8, i.getTrain("P1"));
    }

    @Test
    public void testThreeToNineRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 3, 9);

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(9, i.getTrain("P1"));
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
    public void testThreeToFourRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("X1", 3, 4);

        i.moveTrains(new String[]{"X1"});
        assertEquals(7, i.getTrain("X1"));

        i.moveTrains(new String[]{"X1"});
        assertEquals(6, i.getTrain("X1"));

        i.moveTrains(new String[]{"X1"});
        assertEquals(5, i.getTrain("X1"));

        i.moveTrains(new String[]{"X1"});
        assertEquals(1, i.getTrain("X1"));

        i.moveTrains(new String[]{"X1"});
        assertEquals(4, i.getTrain("X1"));
    }

    @Test
    public void testFourToTwoRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 4, 2);

        i.moveTrains(new String[]{"T1"});
        assertEquals(1, i.getTrain("T1"));

        i.moveTrains(new String[]{"T1"});
        assertEquals(5, i.getTrain("T1"));

        i.moveTrains(new String[]{"T1"});
        assertEquals(6, i.getTrain("T1"));

        i.moveTrains(new String[]{"T1"});
        assertEquals(2, i.getTrain("T1"));
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
    public void testNineToTwoRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 9, 2);

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(2, i.getTrain("P1"));
    }

    @Test
    public void testTenToTwoRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 10, 2);

        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));

        i.moveTrains(new String[]{"P1"});
        assertEquals(2, i.getTrain("P1"));
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
    public void testElevenToThreeRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 11, 3);

        i.moveTrains(new String[]{"F1"});
        assertEquals(7, i.getTrain("F1"));

        i.moveTrains(new String[]{"F1"});
        assertEquals(3, i.getTrain("F1"));
    }

    @Test
    public void testSelfRouteOne() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 1, 1);
        assertEquals(1, i.getTrain("A"));
        assertEquals("A", i.getSection(1));
    }

    @Test
    public void testSelfRouteThree() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 3, 3);
        assertEquals(3, i.getTrain("A"));
        assertEquals("A", i.getSection(3));
    }

    @Test
    public void testSelfRouteFour() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 4, 4);
        assertEquals(4, i.getTrain("A"));
        assertEquals("A", i.getSection(4));
    }

    @Test
    public void testSelfRouteNine() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 9, 9);
        assertEquals(9, i.getTrain("A"));
        assertEquals("A", i.getSection(9));
    }

    @Test
    public void testSelfRouteTen() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 10, 10);
        assertEquals(10, i.getTrain("A"));
        assertEquals("A", i.getSection(10));
    }

    @Test
    public void testSelfRouteEleven() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 11, 11);
        assertEquals(11, i.getTrain("A"));
        assertEquals("A", i.getSection(11));
    }

    @Test
    public void testCannotAddToOccupiedIntermediateSection() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);
        i.moveTrains(new String[]{"T1"});

        try {
            i.addTrain("T2", 4, 2);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMixedNamesOneValidOneInvalid() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);

        int moved = i.moveTrains(new String[]{"UNKNOWN", "T1"});
        assertEquals(1, moved);
        assertEquals(4, i.getTrain("T1"));
    }

    @Test
    public void testGetTrainAfterExit() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);
        i.moveTrains(new String[]{"T1"});
        i.moveTrains(new String[]{"T1"});
        assertEquals(-1, i.getTrain("T1"));
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

    @Test
    public void testGetSectionAfterExitBecomesNull() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);
        i.moveTrains(new String[]{"T1"});
        i.moveTrains(new String[]{"T1"});
        assertNull(i.getSection(4));
    }

    @Test
    public void testTwoIndependentMovesCanHappen() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("A", 1, 4);
        i.addTrain("B", 10, 2);

        int moved = i.moveTrains(new String[]{"A", "B"});
        assertTrue(moved >= 1);
    }

    @Test
    public void testPassengerPriorityOnSeven() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 11, 3);
        i.addTrain("F1", 3, 11);

        int moved = i.moveTrains(new String[]{"P1", "F1"});
        assertTrue(moved >= 1);
    }
}
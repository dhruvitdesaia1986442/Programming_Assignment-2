import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingImpl_Test {

    @Test
    public void testAddTrainAndGetTrain() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        assertEquals(1, interlocking.getTrain("T1"));
        assertEquals("T1", interlocking.getSection(1));
    }

    @Test
    public void testMoveSingleTrain() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        int moved = interlocking.moveTrains(new String[]{"T1"});
        assertEquals(1, moved);
        assertEquals(4, interlocking.getTrain("T1"));
        assertNull(interlocking.getSection(1));
        assertEquals("T1", interlocking.getSection(4));
    }

    @Test
    public void testExitSystem() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(-1, interlocking.getTrain("T1"));
        assertNull(interlocking.getSection(4));
    }

    @Test
    public void testDuplicateTrainRejected() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        try {
            interlocking.addTrain("T1", 3, 11);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testOccupiedEntryRejected() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        try {
            interlocking.addTrain("T2", 1, 8);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidRouteRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", 1, 2);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetSectionInvalidRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.getSection(99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testUnknownTrainRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.getTrain("UNKNOWN");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNorthboundPassengerFromNine() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 9, 2);

        assertEquals(9, interlocking.getTrain("T1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(6, interlocking.getTrain("T1"));
    }

    @Test
    public void testNorthboundPassengerFromTen() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 10, 2);

        assertEquals(10, interlocking.getTrain("T1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(6, interlocking.getTrain("T1"));
    }

    @Test
    public void testNorthboundPassengerFromEleven() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 11, 2);

        assertEquals(11, interlocking.getTrain("T1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(9, interlocking.getTrain("T1"));
    }

    @Test
    public void testNorthboundFreightFromEleven() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", 11, 3);

        assertEquals(11, interlocking.getTrain("F1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"F1"}));
        assertEquals(7, interlocking.getTrain("F1"));
    }

    @Test
    public void testFreightFromFourToThree() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("F1", 4, 3);

        assertEquals(4, interlocking.getTrain("F1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"F1"}));
        assertEquals(1, interlocking.getTrain("F1"));
    }

    @Test
    public void testPassengerRouteOneToEight() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", 1, 8);

        assertEquals(1, interlocking.getTrain("P1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"P1"}));
        assertEquals(5, interlocking.getTrain("P1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"P1"}));
        assertEquals(6, interlocking.getTrain("P1"));
    }

    @Test
    public void testPassengerRouteOneToNine() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", 1, 9);

        assertEquals(1, interlocking.getTrain("P1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"P1"}));
        assertEquals(5, interlocking.getTrain("P1"));
    }

    @Test
    public void testPassengerRouteThreeToEight() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", 3, 8);

        assertEquals(3, interlocking.getTrain("P1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"P1"}));
        assertEquals(6, interlocking.getTrain("P1"));
    }

    @Test
    public void testPassengerRouteThreeToNine() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", 3, 9);

        assertEquals(3, interlocking.getTrain("P1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"P1"}));
        assertEquals(6, interlocking.getTrain("P1"));
    }

    @Test
    public void testMoveTrainsNullRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.moveTrains(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidEntrySectionRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", 99, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidDestinationSectionRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("T1", 1, 99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEmptyTrainNameRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain("", 1, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNullTrainNameRejected() {
        Interlocking interlocking = new InterlockingImpl();

        try {
            interlocking.addTrain(null, 1, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMoveIgnoresUnknownTrainName() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        int moved = interlocking.moveTrains(new String[]{"UNKNOWN"});
        assertEquals(0, moved);
        assertEquals(1, interlocking.getTrain("T1"));
    }

    @Test
    public void testMoveIgnoresNullTrainName() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        int moved = interlocking.moveTrains(new String[]{null});
        assertEquals(0, moved);
        assertEquals(1, interlocking.getTrain("T1"));
    }

    @Test
    public void testMoveDuplicateNamesOnlyOnce() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        int moved = interlocking.moveTrains(new String[]{"T1", "T1"});
        assertEquals(1, moved);
        assertEquals(4, interlocking.getTrain("T1"));
    }

    @Test
    public void testBlockedBecauseNextOccupied() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);
        interlocking.addTrain("T2", 3, 11);

        int moved = interlocking.moveTrains(new String[]{"T2"});
        assertEquals(1, moved);
        assertEquals(7, interlocking.getTrain("T2"));

        moved = interlocking.moveTrains(new String[]{"T2"});
        assertEquals(1, moved);
        assertEquals(11, interlocking.getTrain("T2"));

        moved = interlocking.moveTrains(new String[]{"T1"});
        assertEquals(0, moved);
        assertEquals(1, interlocking.getTrain("T1"));
    }

    @Test
    public void testPassengerAndFreightConflictScenario() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("P1", 11, 2);
        interlocking.addTrain("F1", 3, 11);

        int moved = interlocking.moveTrains(new String[]{"P1", "F1"});
        assertTrue(moved >= 1);
    }

    @Test
    public void testExitAfterDestinationReachedInLaterRound() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 3, 11);

        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(7, interlocking.getTrain("T1"));

        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(11, interlocking.getTrain("T1"));

        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(-1, interlocking.getTrain("T1"));
        assertNull(interlocking.getSection(11));
    }

    @Test
    public void testExitedTrainNoLongerMoves() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));

        int moved = interlocking.moveTrains(new String[]{"T1"});
        assertEquals(0, moved);
    }

    @Test
    public void testSectionBecomesNullAfterMove() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        interlocking.moveTrains(new String[]{"T1"});
        assertNull(interlocking.getSection(1));
        assertEquals("T1", interlocking.getSection(4));
    }

    @Test
    public void testFourToTwoRoute() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 4, 2);

        assertEquals(4, interlocking.getTrain("T1"));
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(1, interlocking.getTrain("T1"));
    }

    @Test
    public void testCannotAddToOccupiedIntermediateSection() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);
        interlocking.moveTrains(new String[]{"T1"});

        try {
            interlocking.addTrain("T2", 4, 2);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMixedNamesOneValidOneInvalid() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        int moved = interlocking.moveTrains(new String[]{"UNKNOWN", "T1"});
        assertEquals(1, moved);
        assertEquals(4, interlocking.getTrain("T1"));
    }

    @Test
    public void testSectionInitiallyEmpty() {
        Interlocking interlocking = new InterlockingImpl();
        assertNull(interlocking.getSection(5));
    }

    @Test
    public void testGetTrainReturnsMinusOneAfterExit() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        interlocking.moveTrains(new String[]{"T1"});
        interlocking.moveTrains(new String[]{"T1"});

        assertEquals(-1, interlocking.getTrain("T1"));
    }
}
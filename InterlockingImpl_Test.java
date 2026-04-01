import static org.junit.Assert.*;
import org.junit.Test;

public class InterlockingImpl_Test {

    @Test
    public void testAddTrainAndGetTrain() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);
        assertEquals(1, i.getTrain("t1"));
        assertEquals("t1", i.getSection(1));
    }

    @Test
    public void testMoveSingleTrain() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        int moved = i.moveTrains(new String[]{"t1"});
        assertEquals(1, moved);
        assertEquals(4, i.getTrain("t1"));
        assertNull(i.getSection(1));
        assertEquals("t1", i.getSection(4));
    }

    @Test
    public void testExitSystem() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(-1, i.getTrain("t1"));
        assertNull(i.getSection(4));
    }

    @Test
    public void testDuplicateTrainRejected() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        try {
            i.addTrain("t1", 3, 11);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testOccupiedEntryRejected() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        try {
            i.addTrain("t2", 1, 8);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidRouteRejected() {
        InterlockingImpl i = new InterlockingImpl();

        try {
            i.addTrain("t1", 1, 2);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetSectionInvalidRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.getSection(99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testUnknownTrainRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.getTrain("missing");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNorthboundPassengerRouteFromNine() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 9, 2);

        assertEquals(9, i.getTrain("t1"));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(6, i.getTrain("t1"));
    }

    @Test
    public void testNorthboundPassengerRouteFromTen() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 10, 2);

        assertEquals(10, i.getTrain("t1"));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(6, i.getTrain("t1"));
    }

    @Test
    public void testNorthboundPassengerRouteFromEleven() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 11, 2);

        assertEquals(11, i.getTrain("t1"));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(9, i.getTrain("t1"));
    }

    @Test
    public void testNorthboundFreightRouteFromEleven() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("f1", 11, 3);

        assertEquals(11, i.getTrain("f1"));
        assertEquals(1, i.moveTrains(new String[]{"f1"}));
        assertEquals(7, i.getTrain("f1"));
    }

    @Test
    public void testFreightRouteFromFourToThree() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("f1", 4, 3);

        assertEquals(4, i.getTrain("f1"));
        assertEquals(1, i.moveTrains(new String[]{"f1"}));
        assertEquals(1, i.getTrain("f1"));
    }

    @Test
    public void testPassengerRouteOneToEight() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 1, 8);

        assertEquals(1, i.getTrain("p1"));
        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(5, i.getTrain("p1"));
        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(6, i.getTrain("p1"));
    }

    @Test
    public void testPassengerRouteOneToNine() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 1, 9);

        assertEquals(1, i.getTrain("p1"));
        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(5, i.getTrain("p1"));
    }

    @Test
    public void testPassengerRouteThreeToEight() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 3, 8);

        assertEquals(3, i.getTrain("p1"));
        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(6, i.getTrain("p1"));
    }

    @Test
    public void testPassengerRouteThreeToNine() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 3, 9);

        assertEquals(3, i.getTrain("p1"));
        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(6, i.getTrain("p1"));
    }

    @Test
    public void testMoveTrainsNullRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.moveTrains(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidEntrySectionRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.addTrain("t1", 99, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testInvalidDestinationSectionRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.addTrain("t1", 1, 99);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEmptyTrainNameRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.addTrain("", 1, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testNullTrainNameRejected() {
        InterlockingImpl i = new InterlockingImpl();
        try {
            i.addTrain(null, 1, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMoveIgnoresUnknownTrainName() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        int moved = i.moveTrains(new String[]{"missing"});
        assertEquals(0, moved);
        assertEquals(1, i.getTrain("t1"));
    }

    @Test
    public void testMoveIgnoresNullTrainName() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        int moved = i.moveTrains(new String[]{null});
        assertEquals(0, moved);
        assertEquals(1, i.getTrain("t1"));
    }

    @Test
    public void testMoveDuplicateNamesOnlyOnce() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        int moved = i.moveTrains(new String[]{"t1", "t1"});
        assertEquals(1, moved);
        assertEquals(4, i.getTrain("t1"));
    }

    @Test
    public void testBlockedBecauseNextOccupied() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);
        i.addTrain("t2", 3, 11);

        assertEquals(1, i.moveTrains(new String[]{"t2"}));
        assertEquals(11, i.getTrain("t2"));

        int moved = i.moveTrains(new String[]{"t1"});
        assertEquals(0, moved);
        assertEquals(1, i.getTrain("t1"));
    }

    @Test
    public void testHeadOnSwapPreventionScenario() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 3, 8);
        i.addTrain("p2", 9, 2);

        int moved = i.moveTrains(new String[]{"p1", "p2"});
        assertEquals(1, moved);
    }

    @Test
    public void testTurnoutConflictPreventionScenario() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 3, 8);
        i.addTrain("p2", 10, 2);

        int moved = i.moveTrains(new String[]{"p1", "p2"});
        assertEquals(1, moved);
    }

    @Test
    public void testFreightPassengerPriorityScenario() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("p1", 11, 2);
        i.addTrain("f1", 3, 11);

        assertEquals(1, i.moveTrains(new String[]{"p1"}));
        assertEquals(9, i.getTrain("p1"));

        int moved = i.moveTrains(new String[]{"p1", "f1"});
        assertTrue(moved >= 1);
    }

    @Test
    public void testExitAfterDestinationReachedInLaterRound() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 3, 11);

        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(7, i.getTrain("t1"));

        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(11, i.getTrain("t1"));

        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(-1, i.getTrain("t1"));
        assertNull(i.getSection(11));
    }

    @Test
    public void testExitedTrainNoLongerMoves() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));

        int moved = i.moveTrains(new String[]{"t1"});
        assertEquals(0, moved);
    }

    @Test
    public void testSectionBecomesNullAfterMove() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        i.moveTrains(new String[]{"t1"});
        assertNull(i.getSection(1));
        assertEquals("t1", i.getSection(4));
    }

    @Test
    public void testFourToTwoRoute() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 4, 2);

        assertEquals(4, i.getTrain("t1"));
        assertEquals(1, i.moveTrains(new String[]{"t1"}));
        assertEquals(1, i.getTrain("t1"));
    }

    @Test
    public void testCannotAddToOccupiedIntermediateSection() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);
        i.moveTrains(new String[]{"t1"});

        try {
            i.addTrain("t2", 4, 2);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMixedNamesOneValidOneInvalid() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        int moved = i.moveTrains(new String[]{"missing", "t1"});
        assertEquals(1, moved);
        assertEquals(4, i.getTrain("t1"));
    }

    @Test
    public void testSectionInitiallyEmpty() {
        InterlockingImpl i = new InterlockingImpl();
        assertNull(i.getSection(5));
    }

    @Test
    public void testGetTrainReturnsMinusOneAfterExit() {
        InterlockingImpl i = new InterlockingImpl();
        i.addTrain("t1", 1, 4);

        i.moveTrains(new String[]{"t1"});
        i.moveTrains(new String[]{"t1"});

        assertEquals(-1, i.getTrain("t1"));
    }
}
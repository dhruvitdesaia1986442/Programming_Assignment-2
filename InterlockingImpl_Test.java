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
    public void testTrainWaitsAtDestinationBeforeExit() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        // Reach destination section
        assertEquals(1, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(4, interlocking.getTrain("T1"));

        // Wait at destination
        assertEquals(0, interlocking.moveTrains(new String[]{"T1"}));
        assertEquals(4, interlocking.getTrain("T1"));

        // Exit on next call
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
    public void testSectionInitiallyEmpty() {
        Interlocking interlocking = new InterlockingImpl();
        assertNull(interlocking.getSection(5));
    }

    @Test
    public void testExitedTrainNoLongerMoves() {
        Interlocking interlocking = new InterlockingImpl();
        interlocking.addTrain("T1", 1, 4);

        interlocking.moveTrains(new String[]{"T1"});
        interlocking.moveTrains(new String[]{"T1"});
        interlocking.moveTrains(new String[]{"T1"});

        int moved = interlocking.moveTrains(new String[]{"T1"});
        assertEquals(0, moved);
    }

    @Test
    public void testTransitionOneToFour() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 1, 4);
        assertEquals(1, i.moveTrains(new String[]{"T1"}));
        assertEquals(4, i.getTrain("T1"));
    }

    @Test
    public void testTransitionOneToFive() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 1, 8);
        assertEquals(1, i.moveTrains(new String[]{"P1"}));
        assertEquals(5, i.getTrain("P1"));
    }

    @Test
    public void testTransitionThreeToSix() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 3, 8);
        assertEquals(1, i.moveTrains(new String[]{"P1"}));
        assertEquals(6, i.getTrain("P1"));
    }

    @Test
    public void testTransitionThreeToSeven() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 3, 11);
        assertEquals(1, i.moveTrains(new String[]{"F1"}));
        assertEquals(7, i.getTrain("F1"));
    }

    @Test
    public void testTransitionFourToOne() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 4, 3);
        assertEquals(1, i.moveTrains(new String[]{"F1"}));
        assertEquals(1, i.getTrain("F1"));
    }

    @Test
    public void testTransitionFiveToSix() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 1, 8);
        i.moveTrains(new String[]{"P1"});
        assertEquals(5, i.getTrain("P1"));
        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));
    }

    @Test
    public void testTransitionSixToEight() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 1, 8);
        i.moveTrains(new String[]{"P1"});
        i.moveTrains(new String[]{"P1"});
        i.moveTrains(new String[]{"P1"});
        assertEquals(8, i.getTrain("P1"));
    }

    @Test
    public void testTransitionSixToNine() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 3, 9);
        i.moveTrains(new String[]{"P1"});
        i.moveTrains(new String[]{"P1"});
        assertEquals(9, i.getTrain("P1"));
    }

    @Test
    public void testTransitionSevenToEleven() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 3, 11);
        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});
        assertEquals(11, i.getTrain("F1"));
    }

    @Test
    public void testTransitionSevenToThree() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 11, 3);
        i.moveTrains(new String[]{"F1"});
        i.moveTrains(new String[]{"F1"});
        assertEquals(3, i.getTrain("F1"));
    }

    @Test
    public void testTransitionNineToSix() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 9, 2);
        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));
    }

    @Test
    public void testTransitionTenToSix() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 10, 2);
        i.moveTrains(new String[]{"P1"});
        assertEquals(6, i.getTrain("P1"));
    }

    @Test
    public void testTransitionElevenToSeven() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 11, 3);
        i.moveTrains(new String[]{"F1"});
        assertEquals(7, i.getTrain("F1"));
    }

    @Test
    public void testFourToTwoRoute() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("T1", 4, 2);

        i.moveTrains(new String[]{"T1"}); // 4 -> 1
        i.moveTrains(new String[]{"T1"}); // 1 -> 5
        i.moveTrains(new String[]{"T1"}); // 5 -> 6
        i.moveTrains(new String[]{"T1"}); // 6 -> 2

        assertEquals(2, i.getTrain("T1"));
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
    public void testRouteFourToThreeReachesThree() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("F1", 4, 3);

        i.moveTrains(new String[]{"F1"}); // 4 -> 1
        i.moveTrains(new String[]{"F1"}); // 1 -> 5
        i.moveTrains(new String[]{"F1"}); // 5 -> 6
        i.moveTrains(new String[]{"F1"}); // 6 -> 7
        i.moveTrains(new String[]{"F1"}); // 7 -> 3

        assertEquals(3, i.getTrain("F1"));
    }

    @Test
    public void testRouteElevenToTwoReachesTwo() {
        Interlocking i = new InterlockingImpl();
        i.addTrain("P1", 11, 2);

        i.moveTrains(new String[]{"P1"}); // 11 -> 7
        i.moveTrains(new String[]{"P1"}); // 7 -> 6
        i.moveTrains(new String[]{"P1"}); // 6 -> 2

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
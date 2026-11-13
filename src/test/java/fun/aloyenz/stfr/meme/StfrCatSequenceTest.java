package fun.aloyenz.stfr.meme;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StfrCatSequenceTest {
    @Test void requiresShutdownFailureConditionAndNearbyGraphicalMonitor() {
        assertFalse(StfrCatSequence.eligible(true,6,0,0,true));
        assertTrue(StfrCatSequence.eligible(true,5,0,10000,true));
        assertTrue(StfrCatSequence.eligible(true,6,1,0,true));
        assertFalse(StfrCatSequence.eligible(false,5,0,0,true));
        assertFalse(StfrCatSequence.eligible(true,5,0,10000.01,true));
        assertFalse(StfrCatSequence.eligible(true,5,0,0,false));
    }
    @Test void twoClicksThenOneShortScareWithNoDoubleHandAdvance() {
        StfrCatSequence s=new StfrCatSequence();
        assertEquals(StfrCatSequence.WARNING,s.stage());
        assertFalse(s.click(100));
        assertEquals(StfrCatSequence.THREAT,s.stage());
        assertFalse(s.click(100));
        assertFalse(s.click(104));
        assertTrue(s.click(105));
        assertEquals(StfrCatSequence.SCARE,s.stage());
        assertFalse(s.click(115));
        assertFalse(s.expire(122));
        assertTrue(s.expire(123));
        assertEquals(StfrCatSequence.IDLE,s.stage());
        assertFalse(s.click(200));
        assertFalse(s.expire(200));
    }
    @Test void savedSequenceResumesWithoutReplayingSound() {
        StfrCatSequence s=StfrCatSequence.restore(2,0,100);
        assertTrue(s.click(200));
        StfrCatSequence loaded=StfrCatSequence.restore(s.stage(),s.until(),s.lastClick());
        assertFalse(loaded.click(210));
        assertFalse(loaded.expire(217));
        assertTrue(loaded.expire(218));
        assertEquals(0,StfrCatSequence.restore(99,0,0).stage());
    }
}

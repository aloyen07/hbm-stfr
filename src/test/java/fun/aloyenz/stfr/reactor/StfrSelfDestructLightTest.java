package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.client.lighting.StfrFlashClock;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructLight;

class StfrSelfDestructLightTest {
    @Test void countdownDoesNotLightUpAndOfflineCoreCannotEmitSteadyLight() {
        for(int tick=0;tick<2405;tick++)assertEquals(0,StfrSelfDestructLight.intensity(tick,1,true));
        assertEquals(0,StfrSelfDestructLight.intensity(2484,1,false));
        assertEquals(0,StfrSelfDestructLight.intensity(3740,1,true));
        assertEquals(.125F,StfrSelfDestructLight.intensity(2700,.25F,true));
        assertEquals(.5F,StfrSelfDestructLight.intensity(3000,1,true));
        assertEquals(.7F,StfrSelfDestructLight.intensity(3320,1,true));
        assertEquals(1,StfrSelfDestructLight.intensity(3700,1,true));
    }
    @Test void exactlyThirtyOfflineLaserHitsWithoutChangingTheSoundCadence() {
        int hits=0;
        for(int tick=0;tick<=3740;tick++) {
            if(StfrSelfDestructLight.ignition(true,tick))hits++;
            assertFalse(StfrSelfDestructLight.ignition(false,tick));
        }
        assertEquals(30,hits);
    }
    @Test void aHalfTickFlashExpiresByClientTimeEvenBeforeTheNextWorldTick() {
        long start=1_000_000_000L;
        long until=StfrFlashClock.deadline(start,StfrSelfDestructLight.IGNITION_MS);
        assertTrue(StfrFlashClock.active(start+24_999_999,until));
        assertFalse(StfrFlashClock.active(start+25_000_000,until));
        assertEquals(start,StfrFlashClock.now(StfrReactor.RunState.SELF_DESTRUCT,start,0));
        assertEquals(100_000_000,StfrFlashClock.now(StfrReactor.RunState.OVERLOAD,start,2));
    }
    @Test void sixDistinctRotorEventsAndOnePulseForEachRepeatedSoundEvent() {
        Set<Integer> ticks=new HashSet<>();
        for(int i=0;i<6;i++)ticks.add(StfrSelfDestructLight.rotorTick(i));
        assertEquals(6,ticks.size());
        assertEquals(3515,Collections.min(ticks));assertEquals(3689,Collections.max(ticks));
        assertEquals(150,StfrSelfDestructLight.pulse(3320).milliseconds);
        assertEquals(100,StfrSelfDestructLight.pulse(3525).milliseconds);
        assertEquals(500,StfrSelfDestructLight.pulse(3730).milliseconds);
        assertNull(StfrSelfDestructLight.pulse(3740));
        for(int tick=3345;tick<3730;tick+=30)assertNotNull(StfrSelfDestructLight.pulse(tick));
    }
}

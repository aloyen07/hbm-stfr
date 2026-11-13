package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;
import fun.aloyenz.stfr.sound.StfrMusicPolicy;

class StfrMeltdownSequenceTest {
    @Test void exactlyThreeVisualPulsesBeforeUnchangedNuclearDeadline() {
        int count=0;
        for(int tick=0;tick<StfrFailureTimeline.DETONATION;tick++) {
            int stage=StfrFailureTimeline.visualBlastStage(tick);
            if(stage!=0)assertEquals(++count,stage);
            assertFalse(StfrFailureTimeline.detonationDue(tick));
        }
        assertEquals(3,count);
        assertEquals(3,StfrFailureTimeline.visualBlastStage(1360));
        assertEquals(1398,StfrFailureTimeline.DETONATION);
        assertTrue(StfrFailureTimeline.detonationDue(1398));
    }
    @Test void OnlyMeltdownAndDetonationSuppressMusic() {
        for(StfrReactor.RunState state:StfrReactor.RunState.values())
            assertEquals(state!=StfrReactor.RunState.MELTDOWN && state!=StfrReactor.RunState.DETONATION,
                StfrMusicPolicy.allowed(state),state.name());
    }
}

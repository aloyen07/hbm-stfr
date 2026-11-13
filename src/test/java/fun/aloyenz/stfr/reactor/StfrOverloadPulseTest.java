package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrOverloadPulse;

class StfrOverloadPulseTest {
    @Test void chanceIsExactlyOnePercentAndNeverAppliesDuringRecovery() {
        int count=0;
        for(int sample=0;sample<10000;sample++) {
            double roll=sample/10000D;
            if(StfrOverloadPulse.triggers(StfrReactor.RunState.OVERLOAD,false,roll))count++;
            assertFalse(StfrOverloadPulse.triggers(StfrReactor.RunState.OVERLOAD,true,roll));
        }
        assertEquals(100,count);
        assertTrue(StfrOverloadPulse.triggers(StfrReactor.RunState.OVERLOAD,false,0));
        assertFalse(StfrOverloadPulse.triggers(StfrReactor.RunState.OVERLOAD,false,.01));
        for(StfrReactor.RunState state:StfrReactor.RunState.values())
            if(state!=StfrReactor.RunState.OVERLOAD)assertFalse(StfrOverloadPulse.triggers(state,false,0));
    }
    @Test void twentyMinutesOfServerTicksHaveIndependentTrials() {
        Random random=new Random(42);int count=0;
        for(int tick=0;tick<20*60*20;tick++)
            if(StfrOverloadPulse.triggers(StfrReactor.RunState.OVERLOAD,false,random.nextDouble()))count++;
        assertTrue(count>150 && count<330,"Pulse count: "+count);
    }
    @Test void flashStopsAfterExactlyTwoWorldTicksWithoutFadeTail() {
        long until=100+StfrOverloadPulse.FLASH_TICKS;
        assertTrue(StfrOverloadPulse.active(100,until));
        assertTrue(StfrOverloadPulse.active(101,until));
        assertFalse(StfrOverloadPulse.active(102,until));
        assertFalse(StfrOverloadPulse.active(200,until));
    }
}

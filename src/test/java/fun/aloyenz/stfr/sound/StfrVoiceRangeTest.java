package fun.aloyenz.stfr.sound;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class StfrVoiceRangeTest {
    @Test void centeredVoiceStaysFullThroughThirtyTwoBlocks() {
        for(double d:new double[]{0,1,16,31.999,32}) {
            assertTrue(StfrVoiceRange.centered(d));assertEquals(1,StfrVoiceRange.gain(d));
        }
    }
    @Test void directionalVoiceFadesSmoothlyToSixtyFourWithoutBoundaryJump() {
        assertFalse(StfrVoiceRange.centered(32.001));
        assertEquals(1,StfrVoiceRange.gain(32.001),.0001);
        assertEquals(.96875,StfrVoiceRange.gain(33),1e-6);
        assertEquals(.5,StfrVoiceRange.gain(48),1e-6);
        assertTrue(StfrVoiceRange.gain(63.99)>0);
        assertEquals(0,StfrVoiceRange.gain(64));assertEquals(0,StfrVoiceRange.gain(128));
        assertEquals(0,StfrVoiceRange.gain(Double.NaN));
    }
    @Test void aRemoteRelayDeliversOneCopyEvenFarFromTheReactor() {
        BlockPos reactor=new BlockPos(0,64,0),mainframe=new BlockPos(100,64,0),alarm=new BlockPos(200,64,0);
        assertEquals(alarm,StfrVoiceRange.closest(new Vec3d(205.5,64.5,.5),Arrays.asList(reactor,mainframe,alarm,alarm)));
        assertEquals(mainframe,StfrVoiceRange.closest(new Vec3d(102.5,64.5,.5),Arrays.asList(reactor,mainframe,alarm)));
        assertNull(StfrVoiceRange.closest(new Vec3d(264.5,64.5,.5),Arrays.asList(reactor,mainframe,alarm)));
    }
}

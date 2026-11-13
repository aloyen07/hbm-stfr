package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrLiftMotion;
import fun.aloyenz.stfr.reactor.physics.StfrVortex;

class StfrLiftMotionTest {
    @Test void bothNormalAndChargedDebrisReachReleaseHeightAndFallWithoutRecapture() {
        for(boolean charging:new boolean[]{false,true}) {
            Vec3d pos=new Vec3d(4,64,0),v=Vec3d.ZERO;
            boolean released=false;int transitions=0;
            for(int tick=0;tick<400;tick++) {
                boolean next=StfrLiftMotion.debrisReleased(released,pos.y,82);
                if(next && !released) { transitions++;v=new Vec3d(v.x,0,v.z); }
                released=next;
                v=released?StfrLiftMotion.falling(v):StfrVortex.velocity(pos,v,charging);
                if(released)assertTrue(v.y<0,"Gravity must continue below the capture zone");
                pos=pos.add(v);
            }
            assertEquals(1,transitions);assertTrue(released);assertTrue(pos.y<64);
        }
    }
    @Test void entityContinuesAboveCaptureCubeThenReentersOnlyInsideIt() {
        assertTrue(StfrLiftMotion.liftEntity(65,82,true,false));
        assertTrue(StfrLiftMotion.liftEntity(80,82,false,true));
        assertFalse(StfrLiftMotion.liftEntity(82,82,true,true));
        assertFalse(StfrLiftMotion.liftEntity(80,82,false,false));
        assertTrue(StfrLiftMotion.liftEntity(65,82,true,false));
    }
    @Test void debrisReleaseBoundaryIsInclusiveAndSurvivesFallingBackBelowIt() {
        assertFalse(StfrLiftMotion.debrisReleased(false,81.999,82));
        assertTrue(StfrLiftMotion.debrisReleased(false,82,82));
        assertTrue(StfrLiftMotion.debrisReleased(true,10,82));
    }
}

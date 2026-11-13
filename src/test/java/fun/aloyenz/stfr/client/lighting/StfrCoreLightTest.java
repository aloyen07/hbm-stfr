package fun.aloyenz.stfr.client.lighting;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.client.lighting.StfrLightExposure;

class StfrCoreLightTest {
    @Test void cubemapAxesAndSeamsUseOpenGlConvention() {
        double[][] axes={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for(int i=0;i<6;i++) {
            Vec3d d=StfrPointShadow.direction(i,0,0);
            assertEquals(axes[i][0],d.x,1e-8);assertEquals(axes[i][1],d.y,1e-8);assertEquals(axes[i][2],d.z,1e-8);
        }
        assertTrue(StfrPointShadow.direction(0,-1,0).distanceTo(StfrPointShadow.direction(4,1,0))<1e-8);
        assertTrue(StfrPointShadow.direction(2,0,-1).distanceTo(StfrPointShadow.direction(5,0,-1))<1e-8);
    }
    @Test void surfaceRadianceChangesExposureEvenWithoutLookingAtTheCore() {
        assertEquals(1,StfrLightExposure.target(0,.75F));
        assertTrue(StfrLightExposure.target(.25F,.75F)<StfrLightExposure.target(.05F,.75F));
        assertEquals(1,StfrLightExposure.target(1,0));
        assertTrue(StfrLightExposure.target(100,.75F)>=.25F);
        assertEquals(1,StfrLightExposure.target(-1,.75F));
    }
    @Test void adaptationIsSmoothAndIndependentOfFrameRate() {
        float fast=1,slow=1;
        for(int i=0;i<120;i++)fast=StfrLightExposure.approach(fast,.3F,1D/120);
        for(int i=0;i<30;i++)slow=StfrLightExposure.approach(slow,.3F,1D/30);
        assertEquals(fast,slow,1e-5);
        assertTrue(fast>.3F && fast<1);
        assertTrue(StfrLightExposure.approach(.3F,1,.1)<1);
    }
}

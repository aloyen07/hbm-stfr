package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.client.lighting.StfrLightExposure;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructPalette;

class StfrSelfDestructPaletteTest {
    @Test void energyWarmsTheRedCoreThroughYellowToWhiteMonotonically() {
        float green=0,blue=0;
        for(int i=0;i<=100;i++) {
            var c=StfrSelfDestructPalette.color(3000,i/100F);
            assertEquals(1,c.red);assertTrue(c.green>=green);assertTrue(c.blue>=blue);
            assertTrue(c.green<=1.00001 && c.blue<=1.00001);
            green=c.green;blue=c.blue;
        }
        var red=StfrSelfDestructPalette.color(2405,0);
        assertTrue(red.green<.2 && red.blue<.05);
        var yellow=StfrSelfDestructPalette.color(3000,.6F);
        assertEquals(1,yellow.green,1e-5);assertTrue(yellow.blue<.05);
        var white=StfrSelfDestructPalette.color(3000,1);
        assertEquals(1,white.green,1e-5);assertEquals(1,white.blue,1e-5);
    }
    @Test void finaleBecomesWhiteEvenWithAnUnusuallyLongChargeConfiguration() {
        var white=StfrSelfDestructPalette.color(3700,0);
        assertEquals(1,white.green,1e-5);assertEquals(1,white.blue,1e-5);
        var body=StfrSelfDestructPalette.color(3000,.3F);var star=body.brighter();
        assertTrue(star.green>body.green && star.blue>body.blue);
    }
    @Test void hiddenOrBehindCameraSourcesDoNotCreateAnExposureStar() {
        assertTrue(StfrLightExposure.onScreen(0,0,0,1));
        assertFalse(StfrLightExposure.onScreen(1.01F,0,0,1));
        assertFalse(StfrLightExposure.onScreen(0,1.01F,0,1));
        assertFalse(StfrLightExposure.onScreen(0,0,0,-1));
        assertFalse(StfrLightExposure.onScreen(0,0,2,1));
        assertFalse(StfrLightExposure.onScreen(Float.NaN,0,0,1));
        assertEquals(1,StfrLightExposure.target(0,.75F));
        assertTrue(StfrLightExposure.approach(.25F,1,1)>.95F);
    }
}

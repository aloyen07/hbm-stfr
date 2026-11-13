package fun.aloyenz.stfr.client.lighting;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.client.render.StfrClientVisuals;
import fun.aloyenz.stfr.client.render.StfrFakeCoreVisuals;

class StfrLightingRenderContractTest {
    @Test void clearColorQueryMeetsLwjglTwoBufferRequirement() {
        var buffer=StfrEntityShadow.clearColorBuffer();
        assertTrue(buffer.isDirect());
        assertTrue(buffer.remaining()>=16);
    }
    @Test void exposurePassPrecedesCoreShellAndRayRendering() throws Exception {
        var lighting=StfrAccidentLighting.class.getMethod("render",RenderWorldLastEvent.class).getAnnotation(SubscribeEvent.class);
        var core=StfrClientVisuals.class.getMethod("render",RenderWorldLastEvent.class).getAnnotation(SubscribeEvent.class);
        var preview=StfrFakeCoreVisuals.class.getMethod("render",RenderWorldLastEvent.class).getAnnotation(SubscribeEvent.class);
        assertEquals(EventPriority.HIGHEST,lighting.priority());
        assertTrue(lighting.priority().ordinal()<core.priority().ordinal());
        assertTrue(lighting.priority().ordinal()<preview.priority().ordinal());
    }
}

package fun.aloyenz.stfr.api.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Posted on the render thread each world frame, even when built-in effects are disabled.
 * Adapters may independently suppress built-in components; no OptiFine types are required. */
public final class StfrCoreLightRenderEvent extends Event {
    public final List<StfrCoreLight> lights;
    public final float partialTicks;
    public boolean renderLighting=true,renderStar=true,renderExposure=true;
    public StfrCoreLightRenderEvent(List<StfrCoreLight> lights,float partialTicks) {
        this.lights=Collections.unmodifiableList(new ArrayList<>(lights));this.partialTicks=partialTicks;
    }
}

package fun.aloyenz.stfr.client.render;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrClientEffectsConfig;
import fun.aloyenz.stfr.api.client.StfrCoreLight;
import fun.aloyenz.stfr.client.lighting.StfrFlashClock;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Client-timed flashes expire without a stop packet or a meltdown fade tail. */
public final class StfrCoreFlashes {
    private static World world;
    private static final class Flash {
        final long until;final float intensity;final int radius;final StfrReactor.RunState stage;
        Flash(long until,float intensity,int radius,StfrReactor.RunState stage) { this.until=until;this.intensity=intensity;this.radius=radius;this.stage=stage; }
    }
    private static final Map<BlockPos,Flash> UNTIL=new HashMap<>();
    private static boolean ready() {
        World current=Minecraft.getMinecraft().world;
        if(current!=world) { UNTIL.clear();world=current; }
        return world!=null;
    }
    public static void receive(int dimension,BlockPos center,float intensity,int milliseconds,int radius,int stage) {
        if(!ready() || world.provider.getDimension()!=dimension || !Float.isFinite(intensity))return;
        if(stage<0 || stage>=StfrReactor.RunState.values().length)return;
        StfrReactor.RunState mode=StfrReactor.RunState.values()[stage];
        if(mode!=StfrReactor.RunState.OVERLOAD && mode!=StfrReactor.RunState.SELF_DESTRUCT)return;
        long now=clock(mode);
        Flash previous=UNTIL.get(center);
        float strength=Math.max(0,Math.min(2.5F,intensity));
        if(previous!=null && StfrFlashClock.active(clock(previous.stage),previous.until))strength=Math.max(strength,previous.intensity);
        UNTIL.put(center,new Flash(StfrFlashClock.deadline(now,milliseconds),strength,
            radius>0?Math.min(StfrClientEffectsConfig.radius,radius):StfrClientEffectsConfig.radius,mode));
    }
    private static long clock(StfrReactor.RunState mode) {
        return StfrFlashClock.now(mode,System.nanoTime(),world.getTotalWorldTime());
    }
    public static List<StfrCoreLight> lights(float partial) {
        List<StfrCoreLight> lights=new ArrayList<>();
        if(!ready())return lights;
        UNTIL.entrySet().removeIf(e -> !StfrFlashClock.active(clock(e.getValue().stage),e.getValue().until));
        for(Map.Entry<BlockPos,Flash> entry:UNTIL.entrySet()) {
            Flash f=entry.getValue();
            lights.add(new StfrCoreLight(world.provider.getDimension(),entry.getKey(),f.stage,world.getTotalWorldTime()+partial,
                StfrClientEffectsConfig.intensity*f.intensity,f.radius,false,true));
        }
        return lights;
    }
}

package fun.aloyenz.stfr.sound;

import java.util.Collection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Frozen playback decision: full voice to 32, directional linear fade from 32 to 64. */
public final class StfrVoiceRange {
    public static final int INNER=32,OUTER=64;
    private StfrVoiceRange() {}
    public static BlockPos closest(Vec3d listener,
            Collection<BlockPos> sources) {
        BlockPos closest=null;double best=OUTER*OUTER;
        for(BlockPos source:sources) {
            double distance=listener.squareDistanceTo(new Vec3d(source).add(.5,.5,.5));
            if(distance<best) { best=distance;closest=source; }
        }
        return closest;
    }
    public static float gain(double distance) {
        if(!Double.isFinite(distance) || distance>=OUTER)return 0;
        return distance<=INNER?1:(float)((OUTER-distance)/(OUTER-INNER));
    }
    public static boolean centered(double distance) { return distance<=INNER && gain(distance)>0; }
}

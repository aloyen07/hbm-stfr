package fun.aloyenz.stfr.api.client;

import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Client snapshot. Time is in world ticks; intensity includes lifecycle fade. */
public final class StfrCoreLight {
    public final int dimension;
    public final boolean preview;
    public final boolean flash;
    public final BlockPos center;
    public final StfrReactor.RunState stage;
    public final double time;
    public final float intensity,radius,red,green,blue;
    public StfrCoreLight(int dimension,BlockPos center,StfrReactor.RunState stage,double time,float intensity,float radius) {
        this(dimension,center,stage,time,intensity,radius,false);
    }
    public StfrCoreLight(int dimension,BlockPos center,StfrReactor.RunState stage,double time,float intensity,float radius,boolean preview) {
        this(dimension,center,stage,time,intensity,radius,preview,false);
    }
    public StfrCoreLight(int dimension,BlockPos center,StfrReactor.RunState stage,double time,float intensity,float radius,boolean preview,boolean flash) {
        this(dimension,center,stage,time,intensity,radius,preview,flash,1,.88F,.68F);
    }
    public StfrCoreLight(int dimension,BlockPos center,StfrReactor.RunState stage,double time,float intensity,float radius,
            boolean preview,boolean flash,float red,float green,float blue) {
        this.flash=flash;
        this.preview=preview;
        this.dimension=dimension;this.center=center;this.stage=stage;this.time=time;
        this.intensity=intensity;this.radius=radius;this.red=red;this.green=green;this.blue=blue;
    }
}

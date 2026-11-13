package fun.aloyenz.stfr.reactor.physics;

import net.minecraft.util.math.Vec3d;

/** Debris release is permanent; living entities may be captured again only inside the cube. */
public final class StfrLiftMotion {
    private StfrLiftMotion() {}
    public static boolean debrisReleased(boolean released,double y,double height) { return released || y>=height; }
    public static Vec3d falling(Vec3d previous) { return new Vec3d(previous.x*.98,previous.y-.04,previous.z*.98); }
    public static boolean liftEntity(double y,double height,boolean inCube,boolean captured) { return y<height && (inCube || captured); }
}

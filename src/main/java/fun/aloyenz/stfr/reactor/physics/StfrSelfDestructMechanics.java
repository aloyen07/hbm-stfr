package fun.aloyenz.stfr.reactor.physics;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fun.aloyenz.stfr.structure.StfrBlockRole;

/** Scripted hatch timing and radial shell launch, independent of world mutation. */
public final class StfrSelfDestructMechanics {
    private StfrSelfDestructMechanics() {}
    public static boolean accessHatch(StfrBlockRole role) {
        return role==StfrBlockRole.HATCH || role==StfrBlockRole.HATCH_FREE
            || role==StfrBlockRole.HATCH_FIXED;
    }
    /** Some side panels close the passage when the vanilla OPEN property is true. */
    public static boolean lockedHatchOpen(StfrBlockRole role,boolean referenceOpen) {
        return role==StfrBlockRole.HATCH_FREE?!referenceOpen:
            role==StfrBlockRole.HATCH_FIXED?referenceOpen:false;
    }
    public static boolean closeHatches(int tick,boolean formingOffline) { return tick==(formingOffline?2455:2405); }
    public static boolean hatchesHandedOff(int tick,boolean formingOffline) { return tick>=(formingOffline?2455:2405); }
    public static Vec3d shellVelocity(BlockPos offset,Random random) {
        Vec3d direction=new Vec3d(offset).normalize();
        if(direction.lengthSquared()<.01)direction=new Vec3d(0,1,0);
        return direction.scale(.75+random.nextDouble()*.5).add(
            (random.nextDouble()-.5)*.25,(random.nextDouble()-.5)*.25,(random.nextDouble()-.5)*.25);
    }
}

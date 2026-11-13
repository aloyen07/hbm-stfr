package fun.aloyenz.stfr.reactor.physics;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

/** Surface motion uses the same negative rotation about the axis as the rotor renderer. */
public final class StfrRotorPhysics {

	public static final int CONTACT_COOLDOWN_TICKS = 6;

	private StfrRotorPhysics() {}

	public static Vec3d impulse(Vec3d radial, EnumFacing axis, int velocity) {
		if (velocity <= 0) return Vec3d.ZERO;
		Vec3d tangent = new Vec3d(axis.getDirectionVec()).crossProduct(radial).scale(-1);
		double radius = tangent.length();
		if (radius < 1E-6) return Vec3d.ZERO;
		double strength = Math.min(1.5, .34 * velocity / 100D * Math.min(1, radius / 2));
		return tangent.scale(strength / radius);
	}
}

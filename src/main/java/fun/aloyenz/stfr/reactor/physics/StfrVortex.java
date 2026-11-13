package fun.aloyenz.stfr.reactor.physics;

import net.minecraft.util.math.Vec3d;

/** Negative rotation around UP, matching the rotor renderer. */
public final class StfrVortex {
	private StfrVortex() {}
	public static Vec3d velocity(Vec3d relative, Vec3d previous, boolean charging) {
		double radius = Math.max(1, Math.sqrt(relative.x * relative.x + relative.z * relative.z));
		double force = charging ? .055 : .025;
		double x = previous.x * .94 - relative.z / radius * force - relative.x * .001;
		double z = previous.z * .94 + relative.x / radius * force - relative.z * .001;
		double horizontal = Math.sqrt(x*x+z*z);
		if (horizontal > .7) { x *= .7/horizontal; z *= .7/horizontal; }
		return new Vec3d(x, charging ? .28 : .18, z);
	}
}

package fun.aloyenz.stfr.reactor.physics;

/** Camera angles in degrees, derived from actual rotor speed and distance. */
public final class StfrRotorVibration {
	private StfrRotorVibration() {}

	public static double amplitude(double speed, double distance) {
		double load = Math.max(0, Math.min(1, (speed - 100) / 600D));
		double attenuation = Math.max(0, 1 - Math.max(0, distance) / 80D);
		return .45D * load * attenuation * attenuation;
	}

	public static double phase(double speed, double time) {
		return time * (.8D + Math.min(700, Math.max(0, speed)) / 350D);
	}

	/** A transient impact masks vibration without consuming its independent source. */
	public static double backgroundWeight(double impactAmplitude) {
		return Math.max(0, 1 - Math.max(0, impactAmplitude) / .5D);
	}
}

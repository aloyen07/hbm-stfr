package fun.aloyenz.stfr.reactor;

/** Slow correction of the suppression needed to hold the normal temperature. */
final class StfrTemperatureControl {

	private boolean initialized;
	private double balance;

	int desired(double temperature, double change, int current) {
		return desired(temperature, change, current,
				StfrCoreSimulation.CORE_TEMP_NOMINAL * 1.11D);
	}

	int desired(double temperature, double change, int current, double target) {
		if (!initialized) {
			balance = current;
			initialized = true;
		}
		double error = temperature - target;
		double correction = error / 1_000_000D + change / 250_000D;
		// Learn the steady suppression instead of retaining the manual setting
		// captured at arming. Freeze integration against actuator saturation.
		double requested = balance + correction;
		if (requested >= 0 && requested <= 100
				|| requested > 100 && error < 0
				|| requested < 0 && error > 0)
			balance = Math.max(0, Math.min(100, balance + error / 4_000_000D));
		return (int) Math.round(Math.max(0, Math.min(100, balance + correction)));
	}
}

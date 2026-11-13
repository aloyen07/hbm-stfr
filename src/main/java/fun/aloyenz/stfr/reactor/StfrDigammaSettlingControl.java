package fun.aloyenz.stfr.reactor;

/** Temporary control while the compressed plasma returns to ordinary fusion. */
final class StfrDigammaSettlingControl {

	private final StfrTemperatureControl temperature = new StfrTemperatureControl();
	private double previousTemperature;
	private boolean pressureRecovery;

	static final class Settings {

		final int suppression, fuel, permeability, vent;

		Settings(int suppression, int fuel, int permeability, int vent) {
			this.suppression = suppression;
			this.fuel = fuel;
			this.permeability = permeability;
			this.vent = vent;
		}
	}

	Settings tick(
			double coreTemperature,
			double corePressure,
			int suppression,
			int fuel,
			int permeability,
			int vent,
			int savedFuel,
			int savedPermeability,
			int savedVent) {
		double change = previousTemperature == 0D ? 0D : coreTemperature - previousTemperature;
		previousTemperature = coreTemperature;
		int wantedSuppression = temperature.desired(coreTemperature, change, suppression);
		if (corePressure < 2_000_000D) pressureRecovery = true;
		else if (corePressure >= 2_500_000D) pressureRecovery = false;
		int wantedFuel = pressureRecovery ? Math.min(100, savedFuel + 25) : savedFuel;
		int wantedPermeability =
				pressureRecovery ? Math.max(0, savedPermeability - 15) : savedPermeability;
		return new Settings(
				approach(suppression, wantedSuppression, 12),
				approach(fuel, wantedFuel, 2),
				approach(permeability, wantedPermeability, 5),
				approach(vent, savedVent, 5));
	}

	private static int approach(int value, int target, int step) {
		return value < target
				? Math.min(target, value + step)
				: value > target ? Math.max(target, value - step) : value;
	}
}

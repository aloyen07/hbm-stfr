package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.check;
import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.near;
import static fun.aloyenz.stfr.reactor.StfrPhysicsSimulationTest.*;

import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.PilotControls;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.Storage;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;

/** Long-running autopilot regulation and shutdown simulation scenarios. */
final class StfrAutopilotSimulationScenarios {
	private StfrAutopilotSimulationScenarios() {}

	static void checkAutopilotTwentyMinutes() {
		// Recreate the beginning of the latest logged pilot run: DIGAMMA
		// ONLINE, 35 MK, 3.6 MPa, RF 95, four deuterium capsules.
		NBTTagCompound plasma = new NBTTagCompound();
		plasma.setString("fuel", "DEUTERIUM");
		plasma.setDouble("coreFuel", 330_000D);
		plasma.setDouble("coreHelium4", 120_000D);
		plasma.setDouble("caseFuel", 300_000D);
		plasma.setDouble("caseHelium4", 100_000D);
		plasma.setDouble("coreTemperature", 35_081_481D);
		plasma.setDouble("caseTemperature", 150D);
		plasma.setDouble("rotation", 120_000_000D);
		plasma.setDouble("shield", 98.5D);
		plasma.setBoolean("exists", true);
		PilotControls reactor = new PilotControls();
		reactor.core.read(plasma);
		StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor);
		StfrCoreSimulation core = reactor.core();
		Storage sink = new Storage();
		double minimumTemperature = Double.POSITIVE_INFINITY, maximumTemperature = 0D;
		double minimumPressure = Double.POSITIVE_INFINITY,
				maximumPressure = 0D,
				maximumCasePressure = 0D;
		double minimumFuel = Double.POSITIVE_INFINITY;
		int longestPowerLoss = 0, powerLoss = 0;
		TemperatureDipStats dips = new TemperatureDipStats();
		for (int tick = 0; tick < 24_000; tick++) {
			reactor.tick = tick;
			pilot.tick(reactor, tick);
			sink.water = 10_000_000;
			sink.steam = 0;
			core.setDigammaPhysics(true, 1D);
			core.tick(
					sink,
					4,
					reactor.fuelInputLevel(),
					reactor.suppression(),
					reactor.offset(),
					reactor.permeability(),
					reactor.lasers(),
					reactor.vent());
			core.inject(
					StfrCoreSimulation.Fuel.DEUTERIUM,
					4
							* reactor.fuelInputLevel()
							* 24D
							/ 10_000D
							* StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			powerLoss = core.reactionPower() > 0D ? 0 : powerLoss + 1;
			longestPowerLoss = Math.max(longestPowerLoss, powerLoss);
			dips.sample(tick, core.coreTemperature());
			if (tick >= 600) {
				minimumTemperature = Math.min(minimumTemperature, core.coreTemperature());
				maximumTemperature = Math.max(maximumTemperature, core.coreTemperature());
				minimumPressure = Math.min(minimumPressure, core.pressure());
				maximumPressure = Math.max(maximumPressure, core.pressure());
				maximumCasePressure = Math.max(maximumCasePressure, core.casePressure());
				minimumFuel = Math.min(minimumFuel, core.coreFuel());
			}
		}
		System.out.println(
				"Digamma pilot 20 min: temperature="
						+ minimumTemperature
						+ ".."
						+ maximumTemperature
						+ ", pressure="
						+ minimumPressure
						+ ".."
						+ maximumPressure
						+ ", casePressure="
						+ maximumCasePressure
						+ ", minimumFuel="
						+ minimumFuel
				+ ", longestPowerLoss="
				+ longestPowerLoss + ", below31M=" + dips);
		check(dips.lastTick < 1_000 && dips.longest < 100,
				"Digamma temperature may dip below 31 MK only during initial settling");
		check(
				longestPowerLoss == 0
						&& minimumTemperature > 25_000_000D
						&& maximumTemperature < StfrCoreSimulation.CORE_TEMP_SAFE_MAX
						&& minimumPressure >= 1_000_000D
						&& maximumPressure < 5_000_000D
						&& maximumCasePressure < 95_156D
						&& minimumFuel > 50_000D,
				"The actual autopilot must preserve fusion for twenty minutes");
		checkAutopilotShutdown(reactor, pilot, true);
	}

	static void checkLoggedAutopilotTwentyMinutes() {
		StfrAutopilotStressScenarios.checkLoggedAutopilotTwentyMinutes();
	}

	static void checkLowPressureAutopilotTwentyMinutes() {
		checkLowPressureAutopilotScenario("33 MK run", 33_212_909D, 108_000D,
				250_000D, 82_959_208D, 53, 27, 60, 60, 4);
		// Fresh run: 38.75 MK, 1.06 MPa, fuel 57, suppression 52,
		// permeability about 62 and vent about 63; see run/logs/latest.log.
		for (int capsules : new int[] {4, 6})
			checkLowPressureAutopilotScenario("fresh 39 MK run / " + capsules,
					38_751_635D, 106_000D, 500_000D, 82_787_100D,
					57, 52, 62, 63, capsules);
	}

	static void checkAshLoadedAutopilotTwentyMinutes() {
		StfrAutopilotStressScenarios.checkAshLoadedAutopilotTwentyMinutes();
	}

	static void checkTieredAutopilots() {
		near(StfrAutopilot.operatingTemperatureTarget(3, 24_000),
				StfrCoreSimulation.CORE_TEMP_NOMINAL * 1.11D,
				"Third-tier setpoint stays fixed");
		near(StfrAutopilot.operatingTemperatureTarget(2, 2_400), 41_000_000D,
				"Second tier reaches the top of its planned cycle");
		near(StfrAutopilot.operatingTemperatureTarget(2, 7_200), 34_000_000D,
				"Second tier reaches the bottom of its planned cycle");
		near(StfrAutopilot.operatingTemperatureTarget(1, 25_200), 45_000_000D,
				"First tier schedules a short overshoot after twenty minutes");
		for (boolean digamma : new boolean[] {false, true})
		for (int level : new int[] {1, 2}) for (int capsules : new int[] {4, 6}) {
			NBTTagCompound plasma = new NBTTagCompound();
			plasma.setString("fuel", "DEUTERIUM");
			plasma.setDouble("coreFuel", digamma ? 330_000D : 157_094D);
			plasma.setDouble("coreHelium4", digamma ? 120_000D : 634_000D);
			plasma.setDouble("caseFuel", 300_000D);
			plasma.setDouble("caseHelium4", digamma ? 100_000D : 130_000D);
			plasma.setDouble("coreTemperature", digamma ? 35_081_481D : 40_217_096D);
			plasma.setDouble("caseTemperature", digamma ? 150D : 330D);
			plasma.setDouble("rotation", digamma ? 120_000_000D : 83_059_731D);
			plasma.setDouble("shield", digamma ? 98.5D : 100D);
			plasma.setBoolean("exists", true);
			PilotControls reactor = new PilotControls();
			reactor.digamma = digamma;
			reactor.fuel = digamma ? 46 : 57;
			reactor.suppression = digamma ? 95 : 68;
			reactor.offset = digamma ? 60 : 100;
			reactor.lasers = digamma ? 10 : 0;
			reactor.permeability = digamma ? 15 : 30;
			reactor.vent = digamma ? 15 : 58;
			reactor.core.read(plasma);
			StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor, level);
			Storage sink = new Storage();
			double minTemperature = Double.POSITIVE_INFINITY, maxTemperature = 0D;
			double preOvershoot = 0D, overshoot = 0D;
			int warningTicks = 0, powerLoss = 0, laserTicks = 0;
			int duration = level == 1 ? 53_000 : 24_000;
			for (int tick = 0; tick < duration; tick++) {
				reactor.tick = tick;
				pilot.tick(reactor, tick);
				if (reactor.lasers > 0) laserTicks++;
				sink.water = 10_000_000;
				sink.steam = 0;
				reactor.core.setDigammaPhysics(digamma, 1D);
				reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
						reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
				reactor.core.inject(StfrCoreSimulation.Fuel.DEUTERIUM,
						capsules * reactor.fuel * 24D / 10_000D
							* StfrCoreSimulation.PLASMA_UNITS_PER_MB);
				double temperature = reactor.core.coreTemperature();
				if (tick >= 4_800) {
					minTemperature = Math.min(minTemperature, temperature);
					maxTemperature = Math.max(maxTemperature, temperature);
				}
				if (tick >= 20_000 && tick < 24_000)
					preOvershoot = Math.max(preOvershoot, temperature);
				if (tick >= 24_000 && tick < 28_000)
					overshoot = Math.max(overshoot, temperature);
				if (tick >= (digamma ? 1_000 : 0)
						&& (temperature < StfrCoreSimulation.CORE_TEMP_SAFE_MIN
								|| temperature > StfrCoreSimulation.CORE_TEMP_SAFE_MAX)) warningTicks++;
				if (reactor.core.reactionPower() <= 0D) powerLoss++;
				check(reactor.core.pressure() >= 1_000_000D
						&& reactor.core.pressure() < 5_000_000D
						&& reactor.core.casePressure() < (digamma ? 95_156D : 81_156D)
						&& reactor.core.caseTemperature() < 1_000D
						&& reactor.core.shield() > 70D,
						"Tier " + level + ", " + capsules + ", digamma=" + digamma
							+ " capsules must avoid physical damage at tick " + tick
							+ " (pressure=" + reactor.core.pressure() + ", casePressure="
							+ reactor.core.casePressure() + ", temp=" + temperature + ")");
			}
			System.out.println("Tier " + level + ", " + capsules + " capsules, digamma="
					+ digamma + ": temp="
					+ minTemperature + ".." + maxTemperature + ", preOvershoot="
					+ preOvershoot + ", overshoot=" + overshoot + ", warnings="
					+ warningTicks + ", powerLoss=" + powerLoss + ", lasers=" + laserTicks);
			check(warningTicks == 0 && powerLoss == 0 && laserTicks < 240,
					"Lower tiers must not trigger routine alarms or depend on lasers");
			if (level == 1)
				check(overshoot > preOvershoot + (digamma ? 1_000_000D : 2_000_000D),
						"First tier must actually overshoot after twenty minutes");
			else check(maxTemperature - minTemperature > 4_000_000D,
						"Second tier must actually traverse a broad temperature band");
		}
	}

	static void checkLowPressureAutopilotScenario(String name,
			double startTemperature, double coreFuel, double caseFuel, double rotation,
			int fuel, int suppression, int permeability, int vent, int capsules) {
		StfrAutopilotStressScenarios.checkLowPressureAutopilotScenario(name, startTemperature, coreFuel, caseFuel, rotation, fuel, suppression, permeability, vent, capsules);
	}

	static void checkAutopilotShutdown(
			PilotControls reactor, StfrAutopilot.Simulation pilot, boolean digamma) {
		checkAutopilotShutdown(reactor, pilot, digamma, 4);
	}

	static void checkAutopilotShutdown(
			PilotControls reactor, StfrAutopilot.Simulation pilot, boolean digamma,
			int capsules) {
		Storage sink = new Storage();
		int stopTick = -1;
		for (int tick = 0; tick < 2_400; tick++) {
			reactor.tick = 24_000L + tick;
			if (pilot.shutdownTick(reactor, reactor.tick)) {
				stopTick = tick;
				break;
			}
			sink.water = 10_000_000;
			sink.steam = 0;
			reactor.core.setDigammaPhysics(digamma, 1D);
			reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
					reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
			reactor.core.inject(StfrCoreSimulation.Fuel.DEUTERIUM,
					capsules * reactor.fuel * 24D / 10_000D
							* StfrCoreSimulation.PLASMA_UNITS_PER_MB);
		}
		System.out.println((digamma ? "Digamma" : "Normal") + " pilot shutdown: tick="
				+ stopTick + ", stress=" + reactor.core.shieldStress() + ", shield="
				+ reactor.core.shield() + ", rpm=" + reactor.core.rotation() / 1000D);
		check(stopTick >= 0 && stopTick < 2_400
				&& StfrEmergencyRules.nominalShutdownAllowed(
					reactor.core.shield(), reactor.core.shieldStress()),
				"Autopilot must request nominal shutdown within two minutes in both modes");
	}

	static void checkHighStressShutdown(boolean digamma) {
		NBTTagCompound plasma = new NBTTagCompound();
		plasma.setString("fuel", "DEUTERIUM");
		plasma.setDouble("coreFuel", 330_000D);
		plasma.setDouble("coreHelium4", 120_000D);
		plasma.setDouble("caseFuel", 200_000D);
		plasma.setDouble("coreTemperature", 35_000_000D);
		plasma.setDouble("caseTemperature", 150D);
		plasma.setDouble("rotation", 120_000_000D);
		plasma.setDouble("shield", 100D);
		plasma.setBoolean("exists", true);
		PilotControls reactor = new PilotControls();
		reactor.digamma = digamma;
		reactor.fuel = 80;
		reactor.suppression = 60;
		reactor.offset = 100;
		reactor.lasers = 0;
		reactor.permeability = 15;
		reactor.vent = 15;
		reactor.core.read(plasma);
		reactor.core.setDigammaPhysics(digamma, 1D);
		Storage sink = new Storage();
		sink.water = 10_000_000;
		reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
				reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
		check(reactor.core.shieldStress() > 20D,
				"High-stress shutdown case must begin above the nominal safety limit");
		checkAutopilotShutdown(reactor, new StfrAutopilot.Simulation(reactor), digamma);
	}

	static void checkLaserEmergencyGate() {
		PilotControls reactor = new PilotControls();
		reactor.lasers = 0;
		reactor.suppression = 40;
		NBTTagCompound plasma = new NBTTagCompound();
		plasma.setString("fuel", "DEUTERIUM");
		plasma.setDouble("coreFuel", 200_000D);
		plasma.setDouble("coreTemperature", 35_000_000D);
		plasma.setDouble("rotation", 92_000_000D);
		plasma.setDouble("shield", 100D);
		plasma.setBoolean("exists", true);
		reactor.core.read(plasma);
		StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor);
		for (int second = 1; second <= 5; second++) {
			plasma.setDouble("coreTemperature", 35_000_000D - second * 500_000D);
			reactor.core.read(plasma);
			pilot.tick(reactor, second * 20L);
			check(reactor.lasers == 0, "Normal temperature decline must not fire lasers");
		}
		for (int second = 1; second <= 5; second++) {
			plasma.setDouble("coreTemperature", 19_000_000D - second * 500_000D);
			reactor.core.read(plasma);
			pilot.tick(reactor, (second + 5) * 20L);
		}
		check(reactor.lasers > 0 && reactor.lasers <= 20,
				"Lasers assist only after sustained severe cooling and full suppression release");
	}

	static void checkAutopilotTemperature(int capsules, double startingTemperature) {
		StfrAutopilotStressScenarios.checkAutopilotTemperature(capsules, startingTemperature);
	}
}

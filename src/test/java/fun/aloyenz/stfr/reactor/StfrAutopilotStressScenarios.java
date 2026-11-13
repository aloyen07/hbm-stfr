package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrAutopilotSimulationScenarios.*;
import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.check;
import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.near;
import static fun.aloyenz.stfr.reactor.StfrPhysicsSimulationTest.*;

import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.PilotControls;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.Storage;

/** Low-pressure, retained-ash and temperature stress scenarios. */
final class StfrAutopilotStressScenarios {
	private StfrAutopilotStressScenarios() {}

	static void checkLoggedAutopilotTwentyMinutes() {
		// The 2026-10-05 log engaged at 38.8 MK, 1.39 MPa, fuel 50,
		// suppression 68 and offset 100. Recreate those controls and pressure.
		NBTTagCompound plasma = new NBTTagCompound();
		plasma.setString("fuel", "DEUTERIUM");
		plasma.setDouble("coreFuel", 150_000D);
		plasma.setDouble("caseFuel", 200_000D);
		plasma.setDouble("coreTemperature", 38_763_309D);
		plasma.setDouble("caseTemperature", 150D);
		plasma.setDouble("rotation", 83_232_999D);
		plasma.setDouble("shield", 100D);
		plasma.setBoolean("exists", true);
		PilotControls reactor = new PilotControls();
		reactor.digamma = false;
		reactor.fuel = 50;
		reactor.suppression = 68;
		reactor.offset = 100;
		reactor.lasers = 0;
		reactor.permeability = 42;
		reactor.vent = 100;
		reactor.core.read(plasma);
		StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor);
		Storage sink = new Storage();
		double minimumTemperature = Double.POSITIVE_INFINITY;
		double maximumTemperature = 0D;
		double minimumPressure = Double.POSITIVE_INFINITY;
		double maximumPressure = 0D;
		double maximumCasePressure = 0D;
		double minimumShield = 100D;
		int longestPowerLoss = 0, powerLoss = 0;
		int laserTicks = 0;
		TemperatureDipStats dips = new TemperatureDipStats();
		for (int tick = 0; tick < 24_000; tick++) {
			reactor.tick = tick;
			pilot.tick(reactor, tick);
			if (reactor.lasers() > 0) laserTicks++;
			sink.water = 10_000_000;
			sink.steam = 0;
			reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
					reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
			reactor.core.inject(StfrCoreSimulation.Fuel.DEUTERIUM,
					4 * reactor.fuel * 24D / 10_000D * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			powerLoss = reactor.core.reactionPower() > 0D ? 0 : powerLoss + 1;
			longestPowerLoss = Math.max(longestPowerLoss, powerLoss);
			dips.sample(tick, reactor.core.coreTemperature());
			if (tick >= 600) {
				minimumTemperature = Math.min(minimumTemperature, reactor.core.coreTemperature());
				maximumTemperature = Math.max(maximumTemperature, reactor.core.coreTemperature());
				minimumPressure = Math.min(minimumPressure, reactor.core.pressure());
				maximumPressure = Math.max(maximumPressure, reactor.core.pressure());
				maximumCasePressure = Math.max(maximumCasePressure, reactor.core.casePressure());
				minimumShield = Math.min(minimumShield, reactor.core.shield());
			}
		}
		System.out.println("Logged pilot 20 min: temperature=" + minimumTemperature + ".."
				+ maximumTemperature + ", pressure=" + minimumPressure + ".."
				+ maximumPressure + ", casePressure=" + maximumCasePressure
				+ ", shield=" + minimumShield + ", powerLoss=" + longestPowerLoss
				+ ", laserTicks=" + laserTicks + ", below31M=" + dips);
		check(dips.episodes <= 2 && dips.ticks < 200,
				"Normal temperature may dip below 31 MK only during initial settling");
		check(minimumTemperature > 25_000_000D
				&& maximumTemperature < StfrCoreSimulation.CORE_TEMP_SAFE_MAX
				&& minimumPressure > 1_000_000D && maximumPressure < 5_000_000D
				&& maximumCasePressure < 95_156D && minimumShield > 70D
				&& longestPowerLoss == 0 && laserTicks < 240,
				"Log-based autopilot must hold twenty minutes with rare laser use");
		StfrAutopilotSimulationScenarios.checkAutopilotShutdown(reactor, pilot, false);
	}

	static void checkAshLoadedAutopilotTwentyMinutes() {
		// Reconstruct 16:56:07 from the live log. Pressure includes retained
		// helium, so a healthy gauge reading can hide depleted deuterium.
		for (int capsules : new int[] {4, 6}) {
			NBTTagCompound plasma = new NBTTagCompound();
			plasma.setString("fuel", "DEUTERIUM");
			plasma.setDouble("coreFuel", 157_094D);
			plasma.setDouble("coreHelium4", 634_000D);
			plasma.setDouble("caseFuel", 300_000D);
			plasma.setDouble("caseHelium4", 130_000D);
			plasma.setDouble("coreTemperature", 40_217_096D);
			plasma.setDouble("caseTemperature", 330D);
			plasma.setDouble("rotation", 83_059_731D);
			plasma.setDouble("shield", 100D);
			plasma.setBoolean("exists", true);
			PilotControls reactor = new PilotControls();
			reactor.digamma = false;
			reactor.fuel = 57;
			reactor.suppression = 68;
			reactor.offset = 100;
			reactor.lasers = 0;
			reactor.permeability = 30;
			reactor.vent = 58;
			reactor.core.read(plasma);
			StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor);
			Storage sink = new Storage();
			double minimumTemperature = Double.POSITIVE_INFINITY;
			double maximumTemperature = 0D;
			double maximumPressure = 0D;
			double maximumCasePressure = 0D;
			double maximumCaseTemperature = 0D;
			double minimumShield = 100D;
			int powerLoss = 0;
			int laserTicks = 0;
			for (int tick = 0; tick < 24_000; tick++) {
				reactor.tick = tick;
				pilot.tick(reactor, tick);
				if (reactor.lasers > 0) laserTicks++;
				sink.water = 10_000_000;
				sink.steam = 0;
				reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
						reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
				reactor.core.inject(StfrCoreSimulation.Fuel.DEUTERIUM,
						capsules * reactor.fuel * 24D / 10_000D
							* StfrCoreSimulation.PLASMA_UNITS_PER_MB);
				minimumTemperature = Math.min(minimumTemperature, reactor.core.coreTemperature());
				maximumTemperature = Math.max(maximumTemperature, reactor.core.coreTemperature());
				maximumPressure = Math.max(maximumPressure, reactor.core.pressure());
				maximumCasePressure = Math.max(maximumCasePressure, reactor.core.casePressure());
				maximumCaseTemperature = Math.max(maximumCaseTemperature,
						reactor.core.caseTemperature());
				minimumShield = Math.min(minimumShield, reactor.core.shield());
				if (reactor.core.reactionPower() <= 0D) powerLoss++;
			}
			System.out.println("Ash-loaded " + capsules + " capsule pilot: temperature="
					+ minimumTemperature + ".." + maximumTemperature + ", pressure="
					+ maximumPressure + ", powerLoss=" + powerLoss
					+ ", maxCasePressure=" + maximumCasePressure + ", laserTicks=" + laserTicks);
			check(minimumTemperature >= 31_000_000D && powerLoss == 0
					&& maximumTemperature < StfrCoreSimulation.CORE_TEMP_SAFE_MAX
					&& maximumPressure < 5_000_000D
					&& maximumCasePressure < 81_156D
					&& maximumCaseTemperature < 1_000D
					&& minimumShield > 70D && laserTicks < 240,
					"Ash-loaded logged reactor must sustain reaction without case damage");
		}
	}

	static void checkLowPressureAutopilotScenario(String name,
			double startTemperature, double coreFuel, double caseFuel, double rotation,
			int fuel, int suppression, int permeability, int vent, int capsules) {
		NBTTagCompound plasma = new NBTTagCompound();
		plasma.setString("fuel", "DEUTERIUM");
		plasma.setDouble("coreFuel", coreFuel);
		plasma.setDouble("caseFuel", caseFuel);
		plasma.setDouble("coreTemperature", startTemperature);
		plasma.setDouble("caseTemperature", 150D);
		plasma.setDouble("rotation", rotation);
		plasma.setDouble("shield", 100D);
		plasma.setBoolean("exists", true);
		PilotControls reactor = new PilotControls();
		reactor.digamma = false;
		reactor.fuel = fuel;
		reactor.suppression = suppression;
		reactor.offset = 100;
		reactor.lasers = 0;
		reactor.permeability = permeability;
		reactor.vent = vent;
		reactor.core.read(plasma);
		StfrAutopilot.Simulation pilot = new StfrAutopilot.Simulation(reactor);
		Storage sink = new Storage();
		double maxTemperature = 0D, maxCasePressure = 0D, maxCaseTemperature = 0D;
		double minShield = 100D, minTemperature = Double.POSITIVE_INFINITY;
		int laserTicks = 0;
		TemperatureDipStats dips = new TemperatureDipStats();
		for (int tick = 0; tick < 24_000; tick++) {
			reactor.tick = tick;
			pilot.tick(reactor, tick);
			if (reactor.lasers > 0) laserTicks++;
			sink.water = 10_000_000;
			sink.steam = 0;
			reactor.core.tick(sink, 4, reactor.fuel, reactor.suppression,
					reactor.offset, reactor.permeability, reactor.lasers, reactor.vent);
			reactor.core.inject(StfrCoreSimulation.Fuel.DEUTERIUM,
					capsules * reactor.fuel * 24D / 10_000D
							* StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			maxTemperature = Math.max(maxTemperature, reactor.core.coreTemperature());
			minTemperature = Math.min(minTemperature, reactor.core.coreTemperature());
			maxCasePressure = Math.max(maxCasePressure, reactor.core.casePressure());
			maxCaseTemperature = Math.max(maxCaseTemperature, reactor.core.caseTemperature());
			minShield = Math.min(minShield, reactor.core.shield());
			dips.sample(tick, reactor.core.coreTemperature());
			check(reactor.core.pressure() >= 1_000_000D
					&& reactor.core.pressure() < 5_000_000D
					&& reactor.core.casePressure() < 81_156D
					&& reactor.core.caseTemperature() < 1_000D
					&& reactor.core.shield() > 70D,
					name + ": autopilot must avoid physical damage at tick " + tick
						+ " (temp=" + reactor.core.coreTemperature()
						+ ", pressure=" + reactor.core.pressure()
						+ ", casePressure=" + reactor.core.casePressure()
						+ ", fuel=" + reactor.fuel + ", suppression=" + reactor.suppression
						+ ", permeability=" + reactor.permeability + ")");
		}
		System.out.println(name + " pilot 20 min: temp=" + minTemperature + ".."
				+ maxTemperature + ", casePressure=" + maxCasePressure
				+ ", caseTemperature=" + maxCaseTemperature + ", shield=" + minShield
				+ ", laserTicks=" + laserTicks + ", below31M=" + dips);
		check(dips.episodes <= 2 && dips.ticks < 600,
				"Low-pressure startup must not repeatedly dip below 31 MK");
		check(maxTemperature < StfrCoreSimulation.CORE_TEMP_SAFE_MAX
				&& minTemperature > 30_000_000D && laserTicks < 240,
				name + ": autopilot must preserve fusion without severe overshoot");
		StfrAutopilotSimulationScenarios.checkAutopilotShutdown(reactor, pilot, false, capsules);
	}

	static void checkAutopilotTemperature(int capsules, double startingTemperature) {
		StfrCoreSimulation core = new StfrCoreSimulation();
		core.simulateOnline();
		core.eventTemperature(startingTemperature);
		if (startingTemperature > 50_000_000) {
			NBTTagCompound slowSpin = core.write();
			slowSpin.setDouble("rotation", 10_000_000D);
			core.read(slowSpin);
		}
		StfrTemperatureControl control = new StfrTemperatureControl();
		StfrFuelFeed[] feeds = new StfrFuelFeed[capsules];
		for (int i = 0; i < capsules; i++) feeds[i] = new StfrFuelFeed();
		Storage sink = new Storage();
		int suppression = 0, offset = 100, lasers = 0;
		double previous = startingTemperature, min = Double.POSITIVE_INFINITY, max = 0, burned = 0;
		for (int tick = 0; tick < 20_000; tick++) {
			if (tick % 20 == 0) {
				double temperature = core.coreTemperature();
				double change = temperature - previous;
				int desired = control.desired(temperature, change, suppression);
				previous = temperature;
				boolean emergency =
						temperature >= StfrCoreSimulation.CORE_TEMP_SAFE_MAX - 1_000_000D;
				if (emergency) lasers = 0;
				else if (temperature + change * 3D
						< StfrCoreSimulation.CORE_TEMP_NOMINAL + 250_000D) {
					desired = Math.min(desired, Math.max(0, suppression - 20));
					lasers = Math.min(100, lasers + 20);
				} else lasers = Math.max(0, lasers - 10);
				int target = emergency ? 100 : desired, limit = emergency ? 15 : 12;
				suppression += Math.max(-limit, Math.min(limit, target - suppression));
				double error = 92_000 - core.rotation() / 1000D;
				if (Math.abs(error) >= 1000)
					offset =
							Math.max(
									-100,
									Math.min(
											100,
											offset
													+ Math.max(
															-5,
															Math.min(
																	5,
																	(int)
																			Math.round(
																					error
																							/ 1000)))));
			}
			sink.water = 10_000_000;
			sink.steam = 0;
			core.tick(sink, 6, 42, suppression, offset, 32, lasers, 100);
			burned += core.reactionRate() / StfrCoreSimulation.PLASMA_UNITS_PER_MB;
			for (StfrFuelFeed feed : feeds)
				feed.tick(core, 42D * 24D / 10_000D, StfrCoreSimulation.Fuel.DEUTERIUM, () -> 1);
			if (tick >= 16_000) {
				min = Math.min(min, core.coreTemperature());
				max = Math.max(max, core.coreTemperature());
			}
		}
		double target = StfrCoreSimulation.CORE_TEMP_NOMINAL * 1.11D;
		System.out.println(
				"Autopilot "
						+ capsules
						+ " capsules, initial="
						+ startingTemperature
						+ ": final band="
						+ min
						+ ".."
						+ max
						+ ", suppress="
						+ suppression);
		check(
				min > target - 1_000_000 && max < target + 1_000_000,
				"Autopilot holds its operating temperature instead of the emergency boundary");
		check(
				core.shield() > 95 && core.pressure() >= 1_000_000,
				"Temperature regulation retains plasma confinement");
		check(
				lasers == 0,
				"The working temperature is held by fusion rather than sustained laser heating");
		near(
				sink.helium
						+ (core.coreHelium4() + core.caseHelium4())
								/ StfrCoreSimulation.PLASMA_UNITS_PER_MB
						+ core.write().getDouble("productRemainder"),
				burned * StfrConfig.deuteriumHeliumYield,
				"Regulation and smooth feed preserve physical fusion products");
	}
}

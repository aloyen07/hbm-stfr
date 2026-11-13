package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.check;
import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.near;
import static fun.aloyenz.stfr.reactor.StfrPhysicsSimulationTest.*;

import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.Storage;

/** Fuel throughput, sustained fusion and smooth-flow simulation scenarios. */
final class StfrFuelSimulationScenarios {
	private StfrFuelSimulationScenarios() {}

	static void checkSustainedFusion(int capsules) {
		StfrCoreSimulation core = new StfrCoreSimulation();
		core.simulateOnline();
		NBTTagCompound startup = core.write();
		startup.setDouble("caseTemperature", 20);
		startup.setDouble("coreTemperature", 38D * 1_111_232D);
		core.read(startup);
		double[] demand = new double[capsules];
		int injected = 36;
		double burned = 0;
		Storage sink = new Storage();
		for (int tick = 0; tick < 10_000; tick++) {
			sink.water = 10_000_000;
			sink.steam = 0; // external coolant loop
			core.tick(sink, 6, 73, 50, 100, 32, 0, 100);
			burned += core.reactionRate() / StfrCoreSimulation.PLASMA_UNITS_PER_MB;
			for (int slot = 0; slot < capsules; slot++) {
				demand[slot] += 73D * 24D / 10_000D;
				int amount = (int) demand[slot];
				demand[slot] -= amount;
				core.injectPhysical(StfrCoreSimulation.Fuel.DEUTERIUM, amount);
				injected += amount;
			}
			check(
					core.coreTemperature() > StfrCoreSimulation.CORE_TEMP_NOMINAL,
					"Capsule-fed fusion stays above the working threshold: "
							+ capsules
							+ " slots, tick "
							+ tick);
			check(
					core.casePressure() < 80_000,
					"Properly vented ash does not create critical case pressure");
			check(
					core.pressure() < 5_000_000 && core.shield() > 95,
					"Normal ash flow preserves confinement");
		}
		check(
				core.coreTemperature() < StfrCoreSimulation.CORE_TEMP_SAFE_MAX,
				"RF control reaches the working temperature range");
		check(
				core.reactionPower() > 0 && sink.steam > 0,
				"Sustained fusion generates power and coolant steam");
		double physicalHelium =
				sink.helium
						+ (core.coreHelium4() + core.caseHelium4())
								/ StfrCoreSimulation.PLASMA_UNITS_PER_MB
						+ core.write().getDouble("productRemainder");
		near(
				physicalHelium,
				burned * StfrConfig.deuteriumHeliumYield,
				"All burned fuel is accounted for as retained or exported helium");
		check(
				physicalHelium <= injected * StfrConfig.deuteriumHeliumYield + 1E-7,
				"Thermal density calibration cannot create physical helium");
		System.out.println(
				"Capsule-fed fusion ("
						+ capsules
						+ " slots): core="
						+ core.coreTemperature()
						+ ", casePressure="
						+ core.casePressure()
						+ ", helium="
						+ sink.helium
						+ " mB");
	}

	static void checkFuelFeed() {
		StfrCoreSimulation core = new StfrCoreSimulation();
		StfrFuelFeed feed = new StfrFuelFeed();
		int[] tank = {100};
		double demand = 42D * 24D / 10_000D;
		for (int tick = 0; tick < 500; tick++) {
			double before = core.coreFuel();
			feed.tick(
					core,
					demand,
					StfrCoreSimulation.Fuel.DEUTERIUM,
					() -> {
						if (tank[0] == 0) return 0;
						tank[0]--;
						return 1;
					});
			near(
					(core.coreFuel() - before) / StfrCoreSimulation.PLASMA_UNITS_PER_MB,
					demand,
					"Smooth injection at every tick");
			near(
					core.coreFuel() / StfrCoreSimulation.PLASMA_UNITS_PER_MB + feed.storedMb(),
					100 - tank[0],
					"Reserved fuel plus plasma equals capsule drain");
			if (tick == 247) {
				StfrFuelFeed reloaded = new StfrFuelFeed();
				reloaded.read(feed.write());
				feed = reloaded;
			}
		}
		double stored = feed.storedMb();
		feed.tick(
				core,
				0,
				StfrCoreSimulation.Fuel.NONE,
				() -> {
					throw new AssertionError("Stopped feed cannot drain capsule");
				});
		near(feed.storedMb(), stored, "Stopped feed retains physical inventory");
		feed.tick(
				core,
				demand,
				StfrCoreSimulation.Fuel.TRITIUM,
				() -> {
					tank[0]--;
					return 1;
				});
		for (int tick = 0; tick < 20; tick++)
			feed.tick(
					core,
					demand,
					StfrCoreSimulation.Fuel.TRITIUM,
					() -> {
						tank[0]--;
						return 1;
					});
		check(
				core.amount(StfrCoreSimulation.Fuel.TRITIUM, false) > 0,
				"Changing capsules preserves old fuel and feeds the new substance");
		near(
				core.coreFuel() / StfrCoreSimulation.PLASMA_UNITS_PER_MB + feed.storedMb(),
				100 - tank[0],
				"Capsule swap conserves both fuels");
		double drained = 100 - tank[0];
		for (int tick = 0; tick < 20; tick++)
			feed.tick(
					core,
					demand,
					StfrCoreSimulation.Fuel.NONE,
					() -> {
						throw new AssertionError("No fuel cannot drain capsule");
					});
		near(
				core.coreFuel() / StfrCoreSimulation.PLASMA_UNITS_PER_MB,
				drained,
				"Empty capsule feeds only the previously reserved remainder");
		check(feed.storedMb() == 0, "Buffer cannot create fuel when empty");
	}

	static void checkSmoothFlow() {
		StfrCoreSimulation core = new StfrCoreSimulation();
		core.ignite();
		NBTTagCompound state = core.write();
		state.setDouble("coreHelium4", 100 * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
		core.read(state);
		Storage sink = new Storage();
		core.tick(sink, 4, 0, 100, 0, 0, 0, 0);
		double before = core.coreHelium4();
		core.tick(sink, 4, 0, 100, 0, 100, 0, 100);
		near(
				(before - core.coreHelium4()) / StfrCoreSimulation.PLASMA_UNITS_PER_MB,
				.1,
				"Opening permeability ramps instead of dumping two mB immediately");
		StfrCoreSimulation restored = new StfrCoreSimulation();
		restored.read(core.write());
		for (int tick = 0; tick < 2000; tick++) {
			core.tick(sink, 4, 0, 100, 0, 100, 0, 100);
			restored.tick(new Storage(), 4, 0, 100, 0, 100, 0, 100);
		}
		near(restored.coreHelium4(), core.coreHelium4(), "Flow ramp survives reload");
		double total =
				sink.helium
						+ (core.coreHelium4() + core.caseHelium4())
								/ StfrCoreSimulation.PLASMA_UNITS_PER_MB
						+ core.write().getDouble("productRemainder");
		near(total, 100, "Smooth core/case/output transfers conserve all helium");
	}
}

package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.check;
import static fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.near;
import static fun.aloyenz.stfr.reactor.StfrPhysicsSimulationTest.*;

import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.Storage;

/** Digamma matter, resonance and pressure-relief simulation scenarios. */
final class StfrDigammaSimulationScenarios {
	private StfrDigammaSimulationScenarios() {}

	static void checkDigammaMatter() {
		StfrDigammaMatter matter = new StfrDigammaMatter();
		near(matter.acceptPolonium(10), 6, "Core polonium hard limit uses HBM mB");
		near(matter.convert(0), 0, "Cold reaction cannot convert polonium");
		near(matter.poloniumMb(), 6, "Cold reaction preserves polonium");
		matter.clear();
		double injector = 2, converted = 0;
		int exported = 0;
		for (int tick = 0; tick < 600; tick++) {
			double accepted = matter.acceptPolonium(Math.min(injector, 2D / 600D));
			injector -= accepted;
			converted += matter.convert(4000);
			exported += matter.takeYaronite(100);
		}
		near(converted, 2, "Two HBM mB convert over 600 full-power ticks");
		near(
				injector + matter.poloniumMb() + matter.yaroniteMb() + exported,
				2,
				"Metered injector and fractional product conserve matter");
		near(matter.poloniumMb(), 0, "Metered feed does not accumulate polonium at full power");
		matter.clear();
		matter.acceptPolonium(6);
		for (int tick = 0; tick < 2400; tick++) matter.convert(4000);
		near(matter.yaroniteMb(), 4, "Full output stops at configured yaronite limit");
		near(matter.poloniumMb(), 2, "Blocked conversion retains unprocessed polonium");
		NBTTagCompound old = new NBTTagCompound();
		old.setDouble("poloniumMb", 100.5);
		old.setDouble("yaroniteMb", 6.5);
		matter.read(old);
		near(
				matter.poloniumMb() + matter.legacyPoloniumMb(),
				100.5,
				"Old polonium above the new limit is reserved");
		near(
				matter.yaroniteMb() + matter.legacyYaroniteMb(),
				6.5,
				"Old yaronite above the new limit is reserved");
		StfrDigammaMatter loaded = new StfrDigammaMatter();
		NBTTagCompound saved = new NBTTagCompound();
		matter.write(saved);
		loaded.read(saved);
		near(
				loaded.poloniumMb() + loaded.legacyPoloniumMb(),
				100.5,
				"Legacy polonium reserve survives reload");
		near(
				loaded.yaroniteMb() + loaded.legacyYaroniteMb(),
				6.5,
				"Legacy yaronite reserve survives reload");
		near(loaded.takeYaronite(100), 4, "Legacy core product exports first");
		loaded.feedLegacyYaronite();
		near(loaded.takeYaronite(100), 2, "Legacy whole product exports after core product");
		near(
				loaded.yaroniteMb() + loaded.legacyYaroniteMb(),
				.5,
				"Legacy fractional product remains available for later conversion");
	}

	static void checkDigammaTransition(
			double initialCoreFuel, int oldSuppression, int oldOffset) {
		// The first case uses the earlier save. The second has the low plasma
		// density and RF setting seen immediately after the player's new run.
		check(
				StfrReactor.digammaSuppression(74, 0) == 87,
				"Digamma's doubled reactivity preserves the pre-transition reaction rate");
		StfrCoreSimulation core = new StfrCoreSimulation();
		NBTTagCompound state = new NBTTagCompound();
		state.setString("fuel", "DEUTERIUM");
		state.setDouble("coreFuel", initialCoreFuel);
		state.setDouble("caseFuel", 308_196D);
		state.setDouble("coreTemperature", 111_186_782D);
		state.setDouble("caseTemperature", 124D);
		state.setDouble("rotation", 92_000_000D);
		state.setDouble("shield", 100D);
		state.setBoolean("exists", true);
		core.read(state);
		Storage sink = new Storage();
		for (int tick = 0; tick < 460; tick++) {
			sink.water = 10_000_000;
			sink.steam = 0;
			int refill = StfrReactor.destabilizerRefillLevel(core.intrinsicPressure());
			core.tick(sink, 4, refill, 100, oldOffset, 0, 0, 100);
			core.inject(
					StfrCoreSimulation.Fuel.DEUTERIUM,
					4 * refill * 24D / 10_000D * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
		}
		check(
				core.caseFuel() < 35_000D,
				"Final digamma phase vents accumulated case gas before normal operation");
		check(
				core.intrinsicPressure() >= 2_400_000D,
				"The overloaded rotor phase restores the core's own pressure before releasing"
					+ " compression");
		core.eventTemperature(StfrCoreSimulation.CORE_TEMP_NOMINAL);
		double minimumTemperature = Double.POSITIVE_INFINITY;
		double maximumTemperature = 0D;
		double minimumPressure = Double.POSITIVE_INFINITY;
		StfrDigammaSettlingControl digammaControl = new StfrDigammaSettlingControl();
		int digammaSuppression = StfrReactor.digammaSuppression(oldSuppression, 0);
		int digammaFuel = 42, digammaPermeability = 38, digammaVent = 100;
		for (int tick = 0; tick < 1_000; tick++) {
			if (tick % 20 == 0) {
				StfrDigammaSettlingControl.Settings next =
						digammaControl.tick(
								core.coreTemperature(),
								core.pressure(),
								digammaSuppression,
								digammaFuel,
								digammaPermeability,
								digammaVent,
								42,
								38,
								100);
				digammaSuppression = next.suppression;
				digammaFuel = next.fuel;
				digammaPermeability = next.permeability;
				digammaVent = next.vent;
			}
			sink.water = 10_000_000;
			sink.steam = 0;
			core.setDigammaPhysics(true, 1D);
			core.tick(
					sink,
					4,
					digammaFuel,
					digammaSuppression,
					oldOffset,
					digammaPermeability,
					0,
					digammaVent);
			core.inject(
					StfrCoreSimulation.Fuel.DEUTERIUM,
					4 * digammaFuel * 24D / 10_000D * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			minimumTemperature = Math.min(minimumTemperature, core.coreTemperature());
			maximumTemperature = Math.max(maximumTemperature, core.coreTemperature());
			minimumPressure = Math.min(minimumPressure, core.pressure());
		}
		check(
				minimumTemperature > StfrCoreSimulation.CORE_TEMP_NOMINAL * .8D
						&& maximumTemperature < StfrCoreSimulation.CORE_TEMP_SAFE_MAX
						&& minimumPressure >= 1_400_000D
						&& core.reactionPower() > 0D,
				"The cleared case sustains 50 seconds of digamma operation: minimum="
						+ minimumTemperature
						+ ", maximum="
						+ maximumTemperature
						+ ", pressure="
						+ minimumPressure
						+ ", final="
						+ core.coreTemperature()
						+ ", power="
						+ core.reactionPower());
		System.out.println(
				"Digamma transition: initialFuel="
						+ initialCoreFuel
						+ ", oldRF="
						+ oldSuppression
						+ ", finalRF="
						+ digammaSuppression
						+ ", minimum="
						+ minimumTemperature
						+ ", final="
						+ core.coreTemperature()
						+ ", maximum="
						+ maximumTemperature
						+ ", minPressure="
						+ minimumPressure
						+ ", power="
						+ core.reactionPower());
	}

	static void checkResonanceCaseRelief() {
		StfrCoreSimulation core = new StfrCoreSimulation();
		NBTTagCompound state = new NBTTagCompound();
		state.setString("fuel", "DEUTERIUM");
		state.setDouble("coreFuel", 300_000D);
		state.setDouble("caseFuel", 300_000D);
		state.setDouble("coreTemperature", 111_000_000D);
		state.setDouble("caseTemperature", 150D);
		state.setDouble("rotation", 120_000_000D);
		state.setDouble("shield", 100D);
		state.setBoolean("exists", true);
		core.read(state);
		Storage sink = new Storage();
		int vent = 0;
		double maximumCasePressure = 0D;
		for (int tick = 0; tick < 1_000; tick++) {
			int desired = StfrReactor.resonanceVentTarget(core.casePressure(), 0);
			vent += Math.max(-2, Math.min(2, desired - vent));
			sink.water = 10_000_000;
			sink.steam = 0;
			core.tick(sink, 4, 20, 60, 100, 20, 0, vent);
			core.inject(
					StfrCoreSimulation.Fuel.DEUTERIUM,
					4 * 20D * 24D / 10_000D * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			maximumCasePressure = Math.max(maximumCasePressure, core.casePressure());
		}
		check(
				maximumCasePressure < 70_000D,
				"The resonance case vent prevents a long stage-4 wait from reaching blowout"
					+ " pressure: "
						+ maximumCasePressure);
		core.read(state);
		double withoutRelief = 0D;
		for (int tick = 0; tick < 1_000; tick++) {
			sink.water = 10_000_000;
			sink.steam = 0;
			core.tick(sink, 4, 20, 60, 100, 20, 0, 0);
			core.inject(
					StfrCoreSimulation.Fuel.DEUTERIUM,
					4 * 20D * 24D / 10_000D * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			withoutRelief = Math.max(withoutRelief, core.casePressure());
		}
		check(
				withoutRelief > 95_156D,
				"The unvented stage-4 case reaches the physical explosion threshold: "
						+ withoutRelief);
		System.out.println(
				"Resonance case pressure: relieved="
						+ maximumCasePressure
						+ ", unvented="
						+ withoutRelief);
	}
}

package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.render.misc.EnumSymbol;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.StfrPhysicsAcceptanceTest.PilotControls;

	public final class StfrPhysicsSimulationTest {
	static final class TemperatureDipStats {
		int ticks, longest, current, episodes, firstTick = -1, lastTick = -1;
		void sample(int tick, double temperature) {
			if (temperature < 31_000_000D) {
				if (firstTick < 0) firstTick = tick;
				lastTick = tick;
				ticks++;
				if (current++ == 0) episodes++;
				longest = Math.max(longest, current);
			} else current = 0;
		}
		public String toString() {
			return ticks + " ticks (" + episodes + " dips, longest " + longest
					+ " ticks, first=" + firstTick + ", last=" + lastTick + ")";
		}
	}

	@Test
	void simulationScenarios() {
		Fluids.WATER = new FluidType("STFR_TEST_WATER", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.STEAM = new FluidType("STFR_TEST_STEAM", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.WATER.addTraits(new FT_Heatable().addStep(200, 1, Fluids.STEAM, 100));
		for (int capsules : new int[] {4, 6}) checkSustainedFusion(capsules);
		checkFuelFeed();
		checkSmoothFlow();
		for (int capsules : new int[] {4, 6})
			for (double start : new double[] {35_000_000D, 61_300_000D})
				checkAutopilotTemperature(capsules, start);
		checkDigammaTransition(262_304D, 74, 77);
		checkDigammaTransition(100_000D, 55, 100);
		checkResonanceCaseRelief();
		checkDigammaMatter();
		checkAutopilotTwentyMinutes();
		checkLoggedAutopilotTwentyMinutes();
		checkLowPressureAutopilotTwentyMinutes();
		checkAshLoadedAutopilotTwentyMinutes();
		checkTieredAutopilots();
		checkHighStressShutdown(false);
		checkHighStressShutdown(true);
		checkLaserEmergencyGate();
		System.out.println("STFR physics and maintenance acceptance checks passed");
	}

	static void checkDigammaMatter() {
		StfrDigammaSimulationScenarios.checkDigammaMatter();
	}

	static void checkDigammaTransition(
			double initialCoreFuel, int oldSuppression, int oldOffset) {
		StfrDigammaSimulationScenarios.checkDigammaTransition(initialCoreFuel, oldSuppression, oldOffset);
	}

	static void checkResonanceCaseRelief() {
		StfrDigammaSimulationScenarios.checkResonanceCaseRelief();
	}

	static void checkAutopilotTwentyMinutes() {
		StfrAutopilotSimulationScenarios.checkAutopilotTwentyMinutes();
	}

	static void checkLoggedAutopilotTwentyMinutes() {
		StfrAutopilotSimulationScenarios.checkLoggedAutopilotTwentyMinutes();
	}

	static void checkLowPressureAutopilotTwentyMinutes() {
		StfrAutopilotSimulationScenarios.checkLowPressureAutopilotTwentyMinutes();
	}

	static void checkAshLoadedAutopilotTwentyMinutes() {
		StfrAutopilotSimulationScenarios.checkAshLoadedAutopilotTwentyMinutes();
	}

	static void checkTieredAutopilots() {
		StfrAutopilotSimulationScenarios.checkTieredAutopilots();
	}

	static void checkLowPressureAutopilotScenario(String name,
			double startTemperature, double coreFuel, double caseFuel, double rotation,
			int fuel, int suppression, int permeability, int vent, int capsules) {
		StfrAutopilotSimulationScenarios.checkLowPressureAutopilotScenario(name, startTemperature, coreFuel, caseFuel, rotation, fuel, suppression, permeability, vent, capsules);
	}

	static void checkAutopilotShutdown(
			PilotControls reactor, StfrAutopilot.Simulation pilot, boolean digamma) {
		StfrAutopilotSimulationScenarios.checkAutopilotShutdown(reactor, pilot, digamma);
	}

	static void checkAutopilotShutdown(
			PilotControls reactor, StfrAutopilot.Simulation pilot, boolean digamma,
			int capsules) {
		StfrAutopilotSimulationScenarios.checkAutopilotShutdown(reactor, pilot, digamma, capsules);
	}

	static void checkHighStressShutdown(boolean digamma) {
		StfrAutopilotSimulationScenarios.checkHighStressShutdown(digamma);
	}

	static void checkLaserEmergencyGate() {
		StfrAutopilotSimulationScenarios.checkLaserEmergencyGate();
	}

	static void checkSustainedFusion(int capsules) {
		StfrFuelSimulationScenarios.checkSustainedFusion(capsules);
	}

	static void checkFuelFeed() {
		StfrFuelSimulationScenarios.checkFuelFeed();
	}

	static void checkSmoothFlow() {
		StfrFuelSimulationScenarios.checkSmoothFlow();
	}

	static void checkAutopilotTemperature(int capsules, double startingTemperature) {
		StfrAutopilotSimulationScenarios.checkAutopilotTemperature(capsules, startingTemperature);
	}
}

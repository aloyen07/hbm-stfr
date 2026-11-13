package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrAutopilot.*;

import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;

/** Shared control laws for live and headless autopilots. */
final class StfrAutopilotRegulation {
	private StfrAutopilotRegulation() {}

	static void regulate(StfrReactor reactor, int level, long tick, State state) {
		regulate(new LiveControls(reactor), level, tick, state);
	}

	static void regulate(Controls reactor, int level, long tick, State state) {
		if (tick % 20 != 0) return;
		double signedRpm = signedRpm(reactor);
		double rpm = Math.abs(signedRpm);
		double temperature = reactor.autopilotTelemetry(reactor.core().coreTemperature(), 2);
		double temperatureChange =
				state.previousTemperature == 0D ? 0D : temperature - state.previousTemperature;
		state.previousTemperature = temperature;
		double measuredPressure = reactor.core().pressure();
		double pressure = reactor.autopilotTelemetry(
				measuredPressure > 0D ? measuredPressure : reactor.core().intrinsicPressure(), 4);
		double pressureChange = state.previousPressure == 0D ? 0D : pressure - state.previousPressure;
		state.previousPressure = pressure;
		int fuel = 0, suppress = 0, offset = 0, laser = 0, vent = 0, permeate = 0;
		String reason = "hold";

		// OFFSET is a signed actuator and must retain its entire -100..100
		// range.  Apply a bounded change each pass, but never impose a second
		// artificial limit on the resulting setting.
		double rpmError = 92_000D - signedRpm;
		if (Math.abs(rpmError) >= 1_000D) {
			offset = Math.max(-5, Math.min(5, (int) Math.round(rpmError / 1_000D)));
			reason = rpmError > 0D ? "rpm_low" : "rpm_overshoot";
		}

		// The useful operating point is just to the right of the reactivity
		// peak (about 31.5 MK), not merely below the failure limit of 47 MK.
		double temperatureTarget = operatingTemperatureTarget(level,
				Math.max(0L, tick - state.engagedTick));
		if (reactor.digammaActive() && level < 3)
			temperatureTarget += 1_500_000D;
		double temperatureError = temperature - temperatureTarget;
		int normalSuppression =
				state.temperatureControl.desired(
						temperature, temperatureChange, reactor.suppression(),
						temperatureTarget);
		double projectedTemperature = temperature + Math.max(0D, temperatureChange) * 6D;
		if (temperature >= 43_000_000D || projectedTemperature >= 45_000_000D) {
			suppress = approachDelta(reactor.suppression(), 100, 15);
			laser = -reactor.lasers();
			reason = "temperature_emergency";
		} else {
			int desiredSuppression = normalSuppression;
			if (projectedTemperature >= 42_000_000D && temperatureChange > 0D)
				desiredSuppression = Math.max(desiredSuppression, 85);
			boolean coolingFast = temperatureChange < -200_000D
					&& temperature + temperatureChange * 6D < 35_000_000D;
			if (coolingFast)
				desiredSuppression = Math.min(desiredSuppression,
						temperature + temperatureChange * 6D < 32_000_000D ? 0 : 30);
			if (temperature < 20_000_000D && temperatureChange < -250_000D)
				state.recoveringReaction = true;
			if (temperature >= 24_000_000D) state.recoveringReaction = false;
			boolean losingReaction = state.recoveringReaction;
			if (losingReaction) {
				desiredSuppression = 0;
				state.laserRecoveryTicks++;
				if (state.laserRecoveryTicks >= 3 && reactor.suppression() == 0
						&& (temperature < 12_000_000D || reactor.core().reactionPower() <= 0D))
					laser = approachDelta(reactor.lasers(), 20, 10);
				reason = "reaction_recovery";
			} else {
				state.laserRecoveryTicks = 0;
				reason = temperatureError > 0D ? "temperature_high" : "temperature_low";
			}
			if (!losingReaction || reactor.lasers() > 20)
				laser = approachDelta(reactor.lasers(), 0, 20);
			if (reactor.digammaActive()) desiredSuppression = Math.min(99, desiredSuppression);
			suppress =
					approachDelta(
							reactor.suppression(), desiredSuppression,
							losingReaction ? 30 : coolingFast ? 15 : 12);
		}

		// Fuel input and permeability control pressure, not temperature. A
		// fusion core below 1 MPa loses field rigidity catastrophically.
		if (reactor.digammaActive()) {
			// Pressure includes helium ash. Maintain both fuel fraction and
			// a clear case instead of restoring pre-transition flow knobs.
			double ash = reactor.core().coreHelium4();
			double fuelAmount = reactor.core().coreFuel();
			double ashFraction = ash / Math.max(1D, ash + fuelAmount);
			// Six live capsules can supply 50% more than four. Regulate the
			// actual plasma reserve rather than assuming a fixed feed setting.
			int targetFuel = StfrReactor.clamp(
					(int) Math.round(90D + (200_000D - fuelAmount) / 5_000D), 40, 100);
			int targetPermeability = ashFraction > .50D ? 45 : 35;
			if (pressure < 1_800_000D) {
				targetFuel = 100;
				targetPermeability = 0;
			} else if (pressure >= 3_200_000D) {
				targetPermeability = 60;
				if (fuelAmount >= 180_000D)
					targetFuel = Math.min(targetFuel, pressure >= 4_000_000D ? 30 : 65);
			}
			boolean urgentPressure = pressure >= 4_200_000D
					|| pressure >= 3_800_000D && pressureChange > 50_000D;
			if (urgentPressure) targetPermeability = 100;
			fuel = approachDelta(reactor.fuelInputLevel(), targetFuel,
					urgentPressure ? 12 : 5);
			permeate = approachDelta(reactor.permeability(), targetPermeability,
					urgentPressure ? 12 : 5);
			vent = approachDelta(reactor.vent(), 100, 10);
			reason = "digamma_matter_balance";
		} else {
			// Hold the core near the middle of the safe pressure band. The
			// previous controller alternated between closing all outlets and
			// restoring their original settings, creating a large limit cycle.
			// Pressure also counts helium ash. The logged failure had >3 MPa
			// while usable deuterium fell below 70k, so pressure alone cannot
			// decide whether to feed or purge the core.
			double plasmaFuel = reactor.core().coreFuel();
			double plasmaAsh = reactor.core().coreHelium4();
			double ashFraction = plasmaAsh / Math.max(1D, plasmaAsh + plasmaFuel);
			boolean fuelStarved = plasmaFuel < 180_000D || ashFraction > .65D;
			int targetFuel = reactor.fuelInputLevel();
			int targetPermeability = reactor.permeability();
			int targetVent = reactor.vent();
			if (pressure < 2_400_000D) {
				if (reactor.fuelInputLevel() < 50 && projectedTemperature < 38_000_000D)
					targetFuel = Math.min(50, reactor.fuelInputLevel() + 2);
				else if (pressure < 1_300_000D && temperature < 32_000_000D
						&& temperatureChange < 0D)
					targetFuel = Math.min(65, reactor.fuelInputLevel() + 2);
				targetPermeability = pressure < 1_300_000D
						? 0 : Math.max(0, reactor.permeability() - 3);
				targetVent = pressure < 1_300_000D
						? 0 : Math.max(0, reactor.vent() - 3);
				reason = "pressure_low";
			} else if (pressure > 3_200_000D) {
				if (pressure > 4_200_000D)
					targetFuel = Math.max(50, reactor.fuelInputLevel() - 3);
				targetPermeability = Math.min(100, reactor.permeability() + 3);
				targetVent = Math.min(100, reactor.vent() + 3);
				reason = "pressure_high";
			} else {
				targetFuel = reactor.fuelInputLevel();
			}
			if (fuelStarved && pressure < 3_400_000D) {
				targetFuel = 100;
				// Remove ash while replenishing the plasma. Vent the case first,
				// otherwise the extra material can rupture its shell.
				targetVent = 100;
				if (pressure > 3_200_000D)
					targetPermeability = Math.max(targetPermeability, 45);
				reason = "fuel_starved";
			}
			if (pressure >= 3_400_000D) {
				targetFuel = pressure >= 3_800_000D ? 0 : 35;
				targetPermeability = 100;
				targetVent = 100;
				reason = "pressure_relief";
			}
			fuel = approachDelta(reactor.fuelInputLevel(), targetFuel,
					pressure >= 3_400_000D ? 20 : 5);
			permeate = approachDelta(reactor.permeability(), targetPermeability,
					pressure >= 3_400_000D ? 15 : pressure < 1_300_000D ? 60 : 3);
			vent = approachDelta(reactor.vent(), targetVent,
					pressure >= 3_400_000D ? 15 : pressure < 1_300_000D ? 30 : 3);
		}
		// The case can blow out before the core itself fails. Relieve it
		// independently of the core pressure loop, without feeding more gas in.
		if (reactor.core().casePressure() >= 65_000D) {
			vent = approachDelta(reactor.vent(), 100, 15);
			permeate = approachDelta(reactor.permeability(), 0, 10);
			fuel = Math.min(fuel, 0);
			reason = "case_pressure_relief";
		}
		if (reactor.core().caseTemperature() >= 900D) {
			suppress = approachDelta(reactor.suppression(), 100, 15);
			fuel = approachDelta(reactor.fuelInputLevel(), 0, 10);
			laser = -reactor.lasers();
			reason = "case_temperature_relief";
		}
		// Lasers are used only to arrest an imminent loss of reaction; normal
		// temperature regulation remains on RF suppression.
		apply(
				reactor,
				level,
				reason,
				fuel,
				suppress,
				offset,
				laser,
				vent,
				permeate,
				state.recordActions);
	}

	static double operatingTemperatureTarget(int level, long elapsedTicks) {
		if (level == 2) {
			// An eight-minute, deliberately broad but smooth operating cycle.
			return 37_500_000D
					+ 3_500_000D * Math.sin(2D * Math.PI * (elapsedTicks % 9_600L) / 9_600D);
		}
		if (level == 1 && elapsedTicks >= 24_000L) {
			long phase = (elapsedTicks - 24_000L) % 24_000L;
			if (phase < 2_400L)
				return 35_000_000D
						+ 10_000_000D * Math.sin(Math.PI * phase / 2_400D);
		}
		return StfrCoreSimulation.CORE_TEMP_NOMINAL * 1.11D;
	}

	static boolean calmForShutdown(
			Controls reactor, int level, long tick, boolean recordActions) {
		if (StfrEmergencyRules.nominalShutdownAllowed(
				reactor.core().shield(), reactor.core().shieldStress())) return true;
		if (tick % 5 != 0) return false;
		// Pressure stress grows with density, while spin stress grows with
		// speed. Both must be reduced together; braking alone can even reverse
		// the rotor before the pressure contribution becomes safe.
		double signedRpm = signedRpm(reactor);
		int targetOffset = StfrReactor.clamp(
				(int) Math.round(50D + (65_000D - signedRpm) / 100D), -100, 100);
		int offset = approachDelta(reactor.offset(), targetOffset, 10);
		double pressure = reactor.core().pressure();
		int fuel = 0, permeability = 0, vent = 0;
		if (pressure > 1_650_000D) {
			fuel = approachDelta(reactor.fuelInputLevel(), 0, 10);
			permeability = approachDelta(reactor.permeability(), 100, 10);
			vent = approachDelta(reactor.vent(), 100, 10);
		} else if (pressure < 1_300_000D) {
			fuel = approachDelta(reactor.fuelInputLevel(), 70, 5);
			permeability = approachDelta(reactor.permeability(), 0, 10);
			vent = approachDelta(reactor.vent(), 0, 10);
		}
		apply(reactor, level, "shutdown_calm", fuel, 0, offset,
				-reactor.lasers(), vent, permeability, recordActions);
		return false;
	}

	static void apply(
			StfrReactor reactor,
			int level,
			String reason,
			int fuel,
			int suppress,
			int offset,
			int laser,
			int vent,
			int permeate) {
		apply(
				new LiveControls(reactor),
				level,
				reason,
				fuel,
				suppress,
				offset,
				laser,
				vent,
				permeate,
				true);
	}

	static void apply(
			Controls reactor,
			int level,
			String reason,
			int fuel,
			int suppress,
			int offset,
			int laser,
			int vent,
			int permeate,
			boolean recordActions) {
		if (fuel == 0 && suppress == 0 && offset == 0 && laser == 0 && vent == 0 && permeate == 0)
			return;
		if (recordActions)
			LOG.info(
					"STFR autopilot L{} {} at {}: dFuel={}, dSuppress={}, dOffset={}, dLaser={},"
						+ " dVent={}, dPermeability={}; fuel={}, suppress={}, offset={}, rpm={},"
						+ " temp={}, pressure={}, coreFuel={}, casePressure={}, caseTemp={},"
						+ " vent={}, permeability={}, shield={}, stress={}, rigidity={}",
					level,
					reason,
					reactor.center(),
					fuel,
					suppress,
					offset,
					laser,
					vent,
					permeate,
					reactor.fuelInputLevel(),
					reactor.suppression(),
					reactor.offset(),
					rpm(reactor),
					Math.round(reactor.core().coreTemperature()),
					Math.round(reactor.core().pressure()),
					Math.round(reactor.core().coreFuel()),
					Math.round(reactor.core().casePressure()),
					Math.round(reactor.core().caseTemperature()),
					reactor.vent(),
					reactor.permeability(),
					reactor.core().shield(),
					reactor.core().shieldStress(),
					reactor.core().rigidityStress());
		reactor.autopilotAdjust(fuel, suppress, offset, laser, vent, permeate);
	}

	static double rpm(StfrReactor reactor) {
		return Math.abs(reactor.autopilotTelemetry(reactor.core().rotation(), 3)) / 1000D;
	}

	static double rpm(Controls reactor) {
		return Math.abs(reactor.autopilotTelemetry(reactor.core().rotation(), 3)) / 1000D;
	}

	static double signedRpm(Controls reactor) {
		return reactor.autopilotTelemetry(reactor.core().rotation(), 3) / 1000D;
	}

	static int approachDelta(int current, int target, int step) {
		return target > current
				? Math.min(step, target - current)
				: target < current ? -Math.min(step, current - target) : 0;
	}
}

package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrAnnouncementController.*;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.reactor.states.StfrSectionState;
import fun.aloyenz.stfr.sound.STFRSounds;

/** Operating warning thresholds and alarm cadence. */
final class StfrOperatingWarnings {
	private StfrOperatingWarnings() {}

	static void checkOperatingWarnings(StfrReactor r, World world) {
		int coolant = r.resources.water().getFill();
		double leakThreshold = r.core.coolingRate() * 10D / 40D;
		r.coolantIntakeLeaking =
				coolant == 0
						|| r.previousCoolant >= 0
								&& leakThreshold >= 2D
								&& r.previousCoolant - coolant >= leakThreshold;
		r.previousCoolant = coolant;
		StfrAnnouncementController.warn(r, world,
				25,
				r.coolantIntakeLeaking,
				!r.coolantIntakeLeaking,
				"operation.coolant_leak",
				Severity.WARNING);
		boolean groundingFault = r.componentStatuses.values().stream().anyMatch(s -> !s.grounded());
		boolean shieldingFault =
				r.componentStatuses.values().stream().anyMatch(s -> !s.shieldIntact());
		StfrAnnouncementController.warn(r, world,
				26,
				groundingFault,
				!groundingFault,
				"operation.grounding_fault",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				27,
				shieldingFault,
				!shieldingFault,
				"operation.radiation_shield_fault",
				Severity.WARNING);
		double rpm = Math.abs(r.core.rotation()) / 1000D;
		StfrAnnouncementController.warn(r, world, 0, rpm <= 81_000, rpm >= 82_000, "warn.spin_low", Severity.NOTICE);
		StfrAnnouncementController.warn(r, world, 1, rpm >= 150_000, rpm <= 149_500, "warn.spin_high", Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				2,
				r.fuelRemaining <= 6_000,
				r.fuelRemaining >= 6_001,
				"warn.fuel_low",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				3,
				r.fuelRemaining <= 3_000,
				r.fuelRemaining >= 3_001,
				"warn.fuel_critical",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				4,
				r.core.coreTemperature() < StfrCoreSimulation.CORE_TEMP_SAFE_MIN,
				r.core.coreTemperature() >= StfrCoreSimulation.CORE_TEMP_SAFE_MIN + 25_000D,
				"warn.temp_low",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				5,
				r.core.coreTemperature() > StfrCoreSimulation.CORE_TEMP_SAFE_MAX,
				r.core.coreTemperature() <= StfrCoreSimulation.CORE_TEMP_SAFE_MAX - 25_000D,
				"warn.temp_high",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				6,
				r.resources.water().getFill() <= 2_500_000,
				r.resources.water().getFill() >= 2_500_000,
				"warn.coolant_low",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				7,
				r.resources.water().getFill() <= 200_000,
				r.resources.water().getFill() >= 200_000,
				"warn.coolant_critical",
				Severity.CRITICAL);
		StfrAnnouncementController.warn(r, world,
				8,
				r.core.pressure() <= 1_000_000,
				r.core.pressure() >= 1_100_000,
				"warn.pressure_critical",
				Severity.CRITICAL);
		StfrAnnouncementController.warn(r, world,
				9,
				r.core.pressure() <= 150_000,
				r.core.pressure() >= 155_000,
				"warn.pressure_low",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				10,
				r.core.pressure() >= 5_000_000,
				r.core.pressure() <= 4_950_000,
				"warn.pressure_high",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				11,
				r.core.casePressure() >= 70_000,
				r.core.casePressure() <= 65_000,
				"warn.case_pressure_high",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				12,
				r.core.casePressure() >= 80_000,
				r.core.casePressure() <= 75_000,
				"warn.case_pressure_critical",
				Severity.CRITICAL);
		StfrAnnouncementController.warn(r, world,
				13,
				r.core.shieldStress() >= 85,
				r.core.shieldStress() <= 83,
				"warn.shield_stress_high",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				14,
				r.core.shieldStress() >= 100,
				r.core.shieldStress() <= 98,
				"warn.shield_stress_critical",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				15,
				r.core.shield() <= 75,
				r.core.shield() >= 77,
				"warn.shield_75",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				16,
				r.core.shield() <= 50,
				r.core.shield() >= 52,
				"warn.shield_50",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				17,
				r.core.shield() <= 25,
				r.core.shield() >= 27,
				"warn.shield_25",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				18,
				r.core.shield() <= 0,
				r.core.shield() >= 2,
				"warn.shield_critical",
				Severity.CRITICAL);
		StfrAnnouncementController.warn(r, world,
				19,
				r.core.caseTemperature() >= 950,
				r.core.caseTemperature() <= 940,
				"warn.case_temp_high",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				20,
				r.core.caseTemperature() >= 1010,
				r.core.caseTemperature() <= 1000,
				"warn.case_temp_critical",
				Severity.CRITICAL);
		StfrAnnouncementController.warn(r, world,
				21,
				r.core.steamBlocked(),
				!r.core.steamBlocked(),
				"warn.steam_blocked",
				Severity.NOTICE);
		int functionalSections = 6 - StfrSectionState.damaged(r.structuralMask);
		StfrAnnouncementController.warn(r, world,
				22,
				functionalSections == 0,
				functionalSections > 0,
				"warn.turbines_lost",
				Severity.NOTICE);
		StfrAnnouncementController.warn(r, world,
				23,
				functionalSections > 0 && functionalSections < 6,
				functionalSections == 0 || functionalSections == 6,
				"warn.turbines_damaged",
				Severity.NOTICE);
		boolean breached = !r.caseIntact(world);
		StfrAnnouncementController.warn(r, world, 24, breached, !breached, "warn.case_breach", Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				28,
				r.digammaActive && r.poloniumMb() > StfrConfig.safeCorePoloniumMb,
				!r.digammaActive || r.poloniumMb() <= StfrConfig.safeCorePoloniumMb,
				"warn.polonium_core_high",
				Severity.WARNING);
		StfrAnnouncementController.warn(r, world,
				29,
				r.digammaActive && r.yaroniteMb() > StfrConfig.safeCoreYaroniteMb,
				!r.digammaActive || r.yaroniteMb() <= StfrConfig.safeCoreYaroniteMb,
				"warn.yaronite_core_high",
				Severity.WARNING);
		r.warningsInitialized = true;
	}

	static void warn(
			StfrReactor r,
			World world,
			int bit,
			boolean trigger,
			boolean clear,
			String event,
			Severity severity) {
		int mask = 1 << bit;
		if (trigger && (r.warningFlags & mask) == 0) {
			if (r.warningsInitialized || severity == Severity.CRITICAL)
				StfrAnnouncementController.announce(r, world, event, severity, null);
			r.warningFlags |= mask;
		} else if (clear) r.warningFlags &= ~mask;
	}

	static void tickOperatingAlarms(StfrReactor r, World world) {
		if (!r.core.exists()) return;
		long tick = r.uptimeTicks;
		double rpm = Math.abs(r.core.rotation()) / 1000D;
		if (r.runState == RunState.ONLINE && rpm <= 81_000 && tick % 24 == 20)
			StfrAnnouncementController.playAlarmRepeated(r, world, STFRSounds.NOTE_GUITAR, SoundCategory.RECORDS, 1F, 0F, 3);
		if (rpm >= 150_000 && r.phase(tick + 9, 24, 2, 6, 10, 14))
			StfrAnnouncementController.playAlarm(r, world, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.RECORDS, 1F, 2F);
		if (r.core.shieldStress() >= 85 && r.phase(tick, 24, 2, 6, 10, 14))
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.NOTE_GUITAR, SoundCategory.RECORDS, 1F, 2F);
		if (r.core.coreTemperature() > StfrCoreSimulation.CORE_TEMP_SAFE_MAX
				&& r.phase(tick + 7, 24, 2, 6, 10, 14))
			StfrAnnouncementController.playAlarm(r, world, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.RECORDS, 1F, 2F);
		if (r.runState == RunState.ONLINE
				&& r.core.coreTemperature() < StfrCoreSimulation.CORE_TEMP_SAFE_MIN
				&& r.phase(tick + 7, 80, 0, 20, 40))
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_3, SoundCategory.AMBIENT, 8F, 0.9F);
		// Original alarm_high_pressure_tick: (uptime + 5) % 24 at 2/6/10/14.
		if (r.core.pressure() >= 5_000_000 && r.phase(tick + 5, 24, 2, 6, 10, 14))
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.NOTE_BIT, SoundCategory.RECORDS, 1F, 2F);
		if (r.core.casePressure() >= 70_000 && (tick % 20 == 2 || tick % 20 == 8)) {
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_3, SoundCategory.RECORDS, 1F, 2F);
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_3, SoundCategory.RECORDS, 1F, 1.97F);
		}
		if (r.core.caseTemperature() >= 1010 && tick % 8 == 0) {
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_2, SoundCategory.AMBIENT, .4F, 1.3F);
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_2, SoundCategory.AMBIENT, .4F, 1.51F);
		}
		if (r.core.shieldStress() >= 100 && r.core.shield() <= 95 && tick % 40 == 0)
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM, SoundCategory.RECORDS, 8F, 1F);
		if (r.runState == RunState.ONLINE && r.fuelRemaining <= 3_000 && (tick + 20) % 50 == 0) {
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_BEEP, SoundCategory.RECORDS, 8F, .61F);
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_BEEP, SoundCategory.RECORDS, 8F, .60F);
		}
		boolean structureFault =
				r.wrongBlocks > 0
						|| r.blockedChamberAir > 0
						|| r.missingPorts > 0
						|| r.workingTurbines < 6
						|| r.sealFailures() > 0;
		if (r.runState == RunState.ONLINE
				&& structureFault
				&& r.phase(tick + 13, 24, 2, 6, 10, 14)) {
			StfrAnnouncementController.playAlarm(r, world, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.RECORDS, 1F, 1.8F);
			StfrAnnouncementController.playAlarm(r, world, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.RECORDS, 1F, 1.81F);
		}
		if (r.core.shieldStress() >= 100 && tick % 80 == 0)
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.TENSION, SoundCategory.AMBIENT, 4F, .7F);
	}
}

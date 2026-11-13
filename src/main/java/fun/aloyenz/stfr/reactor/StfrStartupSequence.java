package fun.aloyenz.stfr.reactor;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.sound.STFRSounds;

/** Startup preparation and ignition clocks. */
final class StfrStartupSequence {
	private StfrStartupSequence() {}
	static void tickStartup(StfrReactor r,World world) {
		StfrRunController.tickStartupAlarm(r, world);
		// TERF increments this timeout only while the breakers are absent.
		if (!r.keyOn) {
			StfrRunController.abortStartup(r, world, false);
		} else if (!r.breakersOn && r.lockState == LockState.LOCKED) {
			// The datapack resets E when an already engaged breaker drops.
			if (r.announcedBreakers) r.stateTicks = 0;
			if (++r.stateTicks >= 2400) StfrRunController.abortStartup(r, world, "start.abort_breakers", true);
		} else if (!r.breakersOn) {
			// Locking the access hatches is part of preparation and must not
			// consume the operator's 120-second breaker window.
			r.stateTicks = 0;
		}
		return;
	}
	static void tickStartupConfirmed(StfrReactor r,World world) {
		StfrRunController.tickStartupAlarm(r, world);
		int startupTick = ++r.stateTicks;
		long startupSupplied = r.resources.takeEnergy(1_000_000);
		if (startupSupplied < 1_000_000) {
			r.startupPowerShortage++;
			if (r.startupPowerShortage == 1 || r.startupPowerShortage == 20)
				StfrRunController.diagnoseStartupPower(r, world, startupSupplied);
		} else r.startupPowerShortage = 0;
		if (r.startupPowerShortage >= 20) {
			if (!r.core.exists()) {
				StfrRunController.abortStartup(r, world, "start.abort_power", true);
				return;
			}
			r.enter(RunState.STARTUP_FAILURE);
			r.failure.startup(world, "STARTUP_POWER_LOSS");
			r.announcedState = r.runState;
			return;
		}
		r.rotorVelocity =
				Math.min(100, Math.max(0, (startupTick - 200) / 2))
						+ Math.min(200, Math.max(0, (startupTick - 763) * 3))
						- Math.min(200, Math.max(0, (startupTick - 860) * 3))
						+ (startupTick >= 860 ? 100 : 0);
		r.rotorAngleUnits += r.rotorVelocity;
		// This startup window aborts only on complete stabilizer loss.
		if (startupTick >= 29 && startupTick <= 822 && r.workingTurbines == 0) {
			r.enter(RunState.OFFLINE);
			r.lockState = LockState.UNLOCKING;
			StfrAnnouncementController.announce(r, world, "start.stabilization_loss", Severity.WARNING, null);
			r.announcedState = RunState.OFFLINE;
			return;
		}
		if (startupTick >= 1030) {
			r.enter(RunState.ONLINE);
		} else {
			if (startupTick == 30) {
				StfrAnnouncementController.playRepeated(r, world,
						STFRSounds.STARTUP_CORE,
						SoundCategory.BLOCKS,
						3.0F,
						1.0F,
						5);
				StfrAnnouncementController.playRepeated(r, world,
						STFRSounds.HEAVY_STARTUP,
						SoundCategory.BLOCKS,
						4.0F,
						1.0F,
						2);
			}
			if (startupTick >= 100 && (startupTick - 100) % 160 == 0)
				world.playSound(
						null,
						r.center,
						STFRSounds.SYSTEM_NOISE,
						SoundCategory.AMBIENT,
						3.0F,
						1.0F);
			if (startupTick >= 365 && startupTick <= 545)
				r.particles(
						world, EnumParticleTypes.CLOUD, r.center, 1, 0.0, 0.2, 0.0, 0.0);
			if (startupTick >= 395 && startupTick <= 545)
				world.playSound(
						null,
						r.center,
						SoundEvents.BLOCK_SAND_BREAK,
						SoundCategory.AMBIENT,
						3.0F,
						0.0F);
			if (startupTick == 540)
				r.particles(
						world, EnumParticleTypes.CLOUD, r.center, 24, 0.2, 0.2, 0.2, 0.01);
			if (startupTick >= 615 && startupTick <= 832)
				r.particles(
						world,
						EnumParticleTypes.SPELL_WITCH,
						r.center,
						10,
						0.3,
						0.3,
						0.3,
						0.0);
			// Leave room for the full systems-ready voice before the shield announcement.
			if (startupTick == 220)
				StfrAnnouncementController.announce(r, world,
						r.workingTurbines == 6 ? "start.systems" : "start.systems_error",
						r.workingTurbines == 6 ? Severity.INFO : Severity.WARNING,
						r.workingTurbines == 6
								? STFRSounds.START_SYSTEMS
								: STFRSounds.START_SYSTEMS_ERROR);
			if (startupTick == 350)
				StfrAnnouncementController.announce(r, world, "start.shield", Severity.INFO, STFRSounds.START_SHIELD);
			if (startupTick >= 545) r.core.startupShieldTick();
			if (startupTick == 585)
				StfrAnnouncementController.announce(r, world,
						"start.injecting",
						Severity.NOTICE,
						STFRSounds.START_INJECTING);
			if (startupTick == 763) {
				StfrAnnouncementController.announce(r, world, "start.lasers", Severity.NOTICE, STFRSounds.START_LASERS);
				world.playSound(
						null,
						r.center,
						STFRSounds.MOTOR_RAMPUP,
						SoundCategory.AMBIENT,
						4.0F,
						1.0F);
			}
			if (startupTick >= 823 && startupTick <= 860) {
				// Datapack: minecraft:block.note_block.snare every tick while
				// the power lasers ignite the injected core.
				world.playSound(
						null,
						r.center,
						SoundEvents.BLOCK_NOTE_SNARE,
						SoundCategory.AMBIENT,
						4.0F,
						2.0F);
				StfrRotorController.ignitionBursts(r, world, startupTick);
				r.core.startupLaserClick();
			}
			if (startupTick == 844) {
				StfrAnnouncementController.playRepeated(r, world,
						STFRSounds.BEACON_ACTIVATE,
						SoundCategory.AMBIENT,
						8.0F,
						2.0F,
						2);
			}
			if (startupTick == 848) {
				for (EnumFacing side : EnumFacing.VALUES)
					r.particles(
							world,
							EnumParticleTypes.CLOUD,
							r.center.offset(side, 3),
							2,
							0.0,
							0.0,
							0.0,
							0.1);
			}
			if (startupTick == 860) {
				r.core.ignite();
				StfrAnnouncementController.playRepeated(r, world,
						STFRSounds.BEACON_ACTIVATE,
						SoundCategory.AMBIENT,
						8.0F,
						0.0F,
						6);
				world.playSound(
						null,
						r.center,
						STFRSounds.BEACON_POWER_SELECT,
						SoundCategory.AMBIENT,
						8.0F,
						0.0F);
				StfrAnnouncementController.playRepeated(r, world,
						STFRSounds.BEACON_AMBIENT,
						SoundCategory.AMBIENT,
						8.0F,
						0.0F,
						4);
				r.particles(
						world,
						EnumParticleTypes.FIREWORKS_SPARK,
						r.center,
						1000,
						0.0,
						0.0,
						0.0,
						0.4);
				if (r.workingStabilizers() == 0) {
					r.enter(RunState.STARTUP_FAILURE);
					r.failure.startup(world, "STARTUP_STABILIZATION_LOSS");
					r.announcedState = r.runState;
					return;
				}
				StfrAnnouncementController.announce(r, world, "start.ignition", Severity.NOTICE, STFRSounds.START_IGNITION);
			}
		}
		return;
	}
}

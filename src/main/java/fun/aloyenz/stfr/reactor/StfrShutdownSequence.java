package fun.aloyenz.stfr.reactor;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.sound.STFRSounds;

/** Nominal shutdown timing and capsule release. */
final class StfrShutdownSequence {
	private StfrShutdownSequence() {}
	static void tickShutdown(StfrReactor r,World world) {
		if (!r.dirtyShutdown && r.core.exists()
				&& (r.stateTicks % 20 == 0 || r.stateTicks == 469)
				&& !r.caseIntact(world)) {
			StfrDirtyShutdown.begin(r, world);
			return;
		}
		StfrRunController.tickStartupAlarm(r, world);
		int shutdownTick = ++r.stateTicks;
		r.rotorVelocity = shutdownTick <= 465 ? 100 : 0;
		r.rotorAngleUnits += r.rotorVelocity;
		if (!r.dirtyShutdown && r.keyOn && shutdownTick >= 20 && shutdownTick <= 240) {
			r.enter(RunState.ONLINE);
			return;
		}
		if (shutdownTick == 160)
			StfrAnnouncementController.announce(r, world,
					"shutdown.stabilizing",
					Severity.INFO,
					STFRSounds.SHUTDOWN_STABILIZING);
		if (shutdownTick == 240)
			StfrAnnouncementController.announce(r, world, "shutdown.cooling", Severity.INFO, STFRSounds.SHUTDOWN_COOLING);
		if (shutdownTick == 445)
			StfrAnnouncementController.announce(r, world, "shutdown.shield", Severity.INFO, STFRSounds.SHUTDOWN_SHIELD);
		if (shutdownTick == 460)
			StfrAnnouncementController.announce(r, world,
					"shutdown.breakers",
					Severity.NOTICE,
					STFRSounds.SHUTDOWN_BREAKERS);
		if (shutdownTick == 465) {
			StfrLogicNetwork.openBreakers(world, r);
			r.breakersOn = false;
			world.playSound(
					null,
					r.center,
					STFRSounds.BREAKER_SWITCH,
					SoundCategory.BLOCKS,
					8.0F,
					1.0F);
			world.playSound(
					null,
					r.center,
					STFRSounds.BREAKER_SWITCH,
					SoundCategory.BLOCKS,
					8.0F,
					0.8F);
		}
		if (shutdownTick == 470) {
			r.refresh(world);
			if (!r.dirtyShutdown && r.emergency.mechanismBreach(world)) {
				r.emergency.start(world, RunState.SHUTDOWN_FAILURE);
				return;
			}
			r.core.deactivate();
			if (r.dirtyShutdown) StfrDirtyShutdown.coreDestroyed(r);
			if (r.digammaActive) StfrDigammaController.finishDigammaShutdown(r, world);
			world.playSound(
					null,
					r.center,
					STFRSounds.REACTOR_SHUTDOWN,
					SoundCategory.AMBIENT,
					10.0F,
					1.0F);
			StfrAnnouncementController.playRepeated(r, world, STFRSounds.HEAVY_SHUTDOWN, SoundCategory.AMBIENT, 3.0F, 0.8F, 5);
			StfrAnnouncementController.playRepeated(r, world,
					STFRSounds.BEACON_DEACTIVATE,
					SoundCategory.AMBIENT,
					10.0F,
					0.0F,
					7);
			r.particles(
					world, EnumParticleTypes.END_ROD, r.center, 1000, 0.5, 0.5, 0.5, 0.1);
			r.particles(world, EnumParticleTypes.FLAME, r.center, 1000, 0.5, 0.5, 0.5, 0.1);
			r.particles(
					world,
					EnumParticleTypes.FIREWORKS_SPARK,
					r.center,
					100,
					0.5,
					0.5,
					0.5,
					0.3);
			r.particles(world, EnumParticleTypes.LAVA, r.center, 100, 0.5, 0.5, 0.5, 0.3);
			r.particles(
					world,
					EnumParticleTypes.SMOKE_LARGE,
					r.center,
					100,
					0.5,
					0.5,
					0.5,
					0.01);
		}
		if (shutdownTick >= 470 && shutdownTick <= 870)
			r.particles(world, EnumParticleTypes.END_ROD, r.center, 1, 1.5, 1.5, 1.5, 0.0);
		if (shutdownTick == 800)
			StfrAnnouncementController.announce(r, world,
					r.sealFailures() > 0 ? "shutdown.vacuuming_fail" : "shutdown.vacuuming",
					r.sealFailures() > 0 ? Severity.WARNING : Severity.INFO,
					r.sealFailures() > 0 ? null : STFRSounds.SHUTDOWN_VACUUMING);
		if (shutdownTick == 810 && r.sealFailures() == 0)
			world.playSound(
					null, r.center, STFRSounds.VACUUM, SoundCategory.AMBIENT, 3.0F, 1.0F);
		// Keep the port's existing LOCKED/UNLOCKING hatch state machine.
		// It owns the physical animation and safety checks for this structure.
		if (shutdownTick == 1100 && r.lockState == LockState.LOCKED)
			r.lockState = LockState.UNLOCKING;
		if (shutdownTick == 1120) {
			StfrAnnouncementController.announce(r, world,
					"shutdown.complete",
					Severity.INFO,
					STFRSounds.SHUTDOWN_COMPLETE);
			r.shutdownCompletionAnnounced = true;
		}
		if (shutdownTick >= 1140) r.enter(RunState.OFFLINE);
		return;
	}
}

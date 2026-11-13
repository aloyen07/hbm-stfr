package fun.aloyenz.stfr.reactor;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.structure.StfrElectrodes;

final class StfrReactorStatus {

	private StfrReactorStatus() {}

	static void serviceNotice(StfrReactor r, World world, String event, boolean warning) {
		StfrAnnouncementController.announce(r, world, event, warning ? Severity.WARNING : Severity.NOTICE, null);
	}

	static void serviceAlarm(StfrReactor r, World world, long uptime) {
		long tick = (uptime + 7) % 24;
		if (tick == 2 || tick == 6 || tick == 10 || tick == 14)
			StfrAnnouncementController.playAlarm(r, world, SoundEvents.BLOCK_NOTE_FLUTE, SoundCategory.RECORDS, 1F, 1F);
	}

	static boolean catastrophic(StfrReactor r) {
		return r.runState == RunState.STARTUP_FAILURE
				|| r.runState == RunState.MELTDOWN
				|| r.runState == RunState.DETONATION
				|| r.emergencyState();
	}

	static boolean terminal(StfrReactor r) {
		return r.runState == RunState.MELTDOWN
				|| r.runState == RunState.DETONATION
				|| r.runState == RunState.UNDERLOAD
				|| r.runState == RunState.SELF_DESTRUCT;
	}

	static boolean unstable(StfrReactor r) {
		return !r.emergency.selfPending()
				&& !(r.runState == RunState.SELF_DESTRUCT && r.stateTicks < 2405)
				&& r.runState != RunState.DETONATION
				&& (r.catastrophic()
						|| r.core.exists()
								&& (r.core.shield() <= 25
										|| r.core.shieldStress() >= 100
										|| r.core.pressure() >= 5_000_000
										|| r.core.caseTemperature() >= 1010));
	}

	static boolean restabilize(StfrReactor r, World world) {
		return r.emergencyState() ? r.emergency.confirm(world) : r.failure.recover(world);
	}

	static void eventSound(
			StfrReactor r,
			World world,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch) {
		if (sound != null) StfrAnnouncementController.playAlarm(r, world, sound, category, volume, pitch);
	}

	static void prepareDestruction(StfrReactor r, World world) {
		r.rotorVelocity = 0;
		StfrRotorController.syncRotorBlocks(r, world, false);
		StfrRotorController.syncElectrodes(r, world, false);
	}

	static boolean liftsDebris(StfrReactor r, BlockPos local) {
		if (r.liftBoxFirst == null || r.liftBoxSecond == null) return true;
		return local.getX() >= Math.min(r.liftBoxFirst.getX(), r.liftBoxSecond.getX())
				&& local.getX() <= Math.max(r.liftBoxFirst.getX(), r.liftBoxSecond.getX())
				&& local.getY() >= Math.min(r.liftBoxFirst.getY(), r.liftBoxSecond.getY())
				&& local.getY() <= Math.max(r.liftBoxFirst.getY(), r.liftBoxSecond.getY())
				&& local.getZ() >= Math.min(r.liftBoxFirst.getZ(), r.liftBoxSecond.getZ())
				&& local.getZ() <= Math.max(r.liftBoxFirst.getZ(), r.liftBoxSecond.getZ());
	}

	static int workingStabilizers(StfrReactor r) {
		int count = 0;
		for (StfrComponentStatus status : r.componentStatuses.values())
			if (StfrEmergencyRules.turbineAvailable(
					status.supercriticalFailures,
					r.mainElectrodeEnabled(status.direction),
					status.secondaryElectrodeMask)) count++;
		return count;
	}

	static boolean electrodeEnabled(StfrReactor r, StfrElectrodes.Chain chain) {
		return chain.index < 0
				? r.mainElectrodeEnabled(chain.direction)
				: (r.secondaryElectrodeMask(chain.direction) & (1 << chain.index)) != 0;
	}

	static int electrodeMask(StfrReactor r) {
		int mask = 0;
		for (StfrElectrodes.Chain chain : StfrElectrodes.chains())
			if (r.electrodeEnabled(chain))
				mask |= 1 << (chain.direction.ordinal() * 5 + chain.index + 1);
		return mask;
	}

	static double electrodeCoverage(StfrReactor r, World world) {
		int enabled = 0;
		for (StfrElectrodes.Chain chain : StfrElectrodes.chains())
			if (r.electrodeEnabled(chain)) enabled++;
		return enabled / 30D;
	}
}

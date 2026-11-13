package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrRunController.*;

import java.util.stream.Collectors;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Startup prerequisites, diagnostics and operator confirmation. */
final class StfrStartupControls {
	private StfrStartupControls() {}

	static void tickStartupAlarm(StfrReactor r, World world) {
		if ((r.uptimeTicks + 5) % 60 != 0) return;
		StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_BEEP, SoundCategory.RECORDS, 8.0F, 0.81F);
		StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_BEEP, SoundCategory.RECORDS, 8.0F, 0.80F);
	}

	static void diagnoseStartupPower(StfrReactor r, World world, long supplied) {
		Logger logger =
				LogManager.getLogger("STFR");
		logger.warn(
				"Startup power at {}: tick={}, supplied={} HE, capacity={} HE, shortageTicks={}",
				r.center,
				r.stateTicks,
				supplied,
				r.resources.energyCapacity(),
				r.startupPowerShortage);
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			BlockPos pos = origin.add(cell.localPos);
			StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
			logger.warn(
					"Power port {}: operational={}, io={}, network={}",
					pos,
					r.portOperational(world, pos),
					network.ioAvailable,
					network.issue);
		}
		for (StfrComponentStatus status : r.componentStatuses.values())
			if (!status.defects.isEmpty())
				logger.warn(
						"Stabilizer {}: critical={}, defects={}",
						status.direction,
						status.criticalFailures,
						status.defects.stream()
								.map(
										defect ->
												defect.localPos
														+ ":"
														+ defect.role
														+ ":"
														+ defect.actual)
								.collect(Collectors.joining("; ")));
	}

	static void abortStartup(StfrReactor r, World world, boolean automatic) {
		StfrRunController.abortStartup(r, world, automatic ? "start.abort_auto" : "start.abort_manual", automatic);
	}

	static void abortStartup(StfrReactor r, World world, String event, boolean automatic) {
		StfrRunController.stopStartupMusic(r, world);
		r.core.discardUnignited();
		r.enter(RunState.OFFLINE);
		if (r.lockState == LockState.LOCKED || r.lockState == LockState.LOCKING)
			r.lockState = LockState.UNLOCKING;
		StfrAnnouncementController.announce(r, world,
				event,
				Severity.WARNING,
				automatic ? STFRSounds.START_ABORT_AUTO : STFRSounds.START_ABORT);
		r.announcedState = RunState.OFFLINE;
	}

	static void stopStartupMusic(StfrReactor r, World world) {
		if (!(world instanceof WorldServer)) return;
		r.failure.stop(world, "hbmstfr:reactor.startup_music", 512);
	}

	static boolean prime(StfrReactor r) {
		// Compatibility path for a reactor that has already completed the port's
		// physical hatch-locking sequence.
		if (r.runState != RunState.OFFLINE || r.lockState != LockState.LOCKED || !r.keyOn)
			return false;
		r.enter(RunState.STARTUP);
		return true;
	}

	static boolean confirm(StfrReactor r, World world) {
		r.refresh(world);
		if (!r.startupProblem(world).isEmpty()) return false;
		r.core.beginStartup();
		r.enter(RunState.STARTUP_CONFIRMED);
		return true;
	}

	static String startupProblem(StfrReactor r, World world) {
		if (r.runState != RunState.STARTUP) return "gui.hbmstfr.mainframe.problem.start";
		if (r.lockState != LockState.LOCKED) return "gui.hbmstfr.mainframe.problem.hatches";
		if (!r.keyOn) return "gui.hbmstfr.mainframe.problem.key";
		if (!r.breakersOn) return "gui.hbmstfr.mainframe.problem.breakers";
		if (!StfrRunController.hasReadyFuel(r, world)) return "gui.hbmstfr.mainframe.problem.fuel";
		if (r.resources.energy() < 1_000_000) return "gui.hbmstfr.mainframe.problem.power";
		return "";
	}
}

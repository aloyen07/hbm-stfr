package fun.aloyenz.stfr.reactor;

import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;

/** Failure-state clocks and terminal transitions. */
final class StfrFailureSequence {
	private StfrFailureSequence() {}
	static void tickStartupFailure(StfrReactor r,World world) {
		r.resources.beginOutputTick(0);
		r.generatedPower = 0;
		r.stateTicks =
				StfrFailureTimeline.advanceFailure(r.stateTicks, r.failure.recovering());
		r.rotorVelocity = 100;
		r.rotorAngleUnits += r.rotorVelocity;
		if (r.failure.tickStartup(world, r.stateTicks)) {
			r.core.restabilize();
			r.enter(RunState.ONLINE);
			r.announcedState = RunState.ONLINE;
		} else if (StfrFailureTimeline.meltdownDue(r.stateTicks, r.failure.recovering())) {
			r.enter(RunState.MELTDOWN);
			r.rotorVelocity = 0;
			r.failure.meltdown(world);
			r.announcedState = r.runState;
		}
		return;
	}
	static void tickMeltdown(StfrReactor r,World world) {
		r.resources.beginOutputTick(0);
		r.generatedPower = 0;
		r.failure.tickMeltdown(world, ++r.stateTicks);
		if (StfrFailureTimeline.detonationDue(r.stateTicks)) {
			r.enter(RunState.DETONATION);
			r.failure.detonate(world);
			r.core.deactivate();
			r.announcedState = r.runState;
		}
		return;
	}
	static void tickDetonation(StfrReactor r,World world) {
		// A terminal saved record prevents an intact suppressed reactor from restarting.
		r.resources.beginOutputTick(0);
		r.generatedPower = 0;
		r.rotorVelocity = 0;
		return;	}
}

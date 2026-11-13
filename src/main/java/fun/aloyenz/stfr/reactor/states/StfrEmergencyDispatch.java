package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrEmergencyEffects;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** Emergency state transitions and per-scene tick dispatch. */
final class StfrEmergencyDispatch {
	private StfrEmergencyDispatch() {}

	static void checkOnline(StfrEmergencyController c, World w) {
		StfrReactor.RunState next =
				StfrEmergencyRules.onlineTransition(
						c.r.workingStabilizers(),
						c.r.core().shield(),
						c.r.core().shieldStress(),
						c.r.core().rigidityStress(),
						w.rand.nextDouble());
		if (next != ONLINE) c.start(w, next);
	}

	static void start(StfrEmergencyController c, World w, StfrReactor.RunState state) {
		if (state != SELF_DESTRUCT
				&& (c.r.runState() == OFFLINE || c.r.runState() == ONLINE || c.r.runState() == SHUTDOWN))
			c.r.failure().reset();
		c.selfWasOffline = !c.r.core().exists();
		if (state != REIGNITION && state != MANUAL_RESTABILIZATION || c.lenses.isEmpty())
			c.rememberLenses(w);
		if (state != SELF_DESTRUCT) c.selfWasOffline = false;
		c.stage = c.recovery = 0;
		c.recovering = false;
		c.internalWarning = false;
		c.r.emergencyEnter(w, state);
		c.r.emergencyStopOutput();
		switch (state) {
			case OVERLOAD:
				StfrOverloadState.enter(c, w);
				break;
			case REACTION_LOSS:
				StfrReactionLossState.enter(c, w);
				break;
			case REIGNITION:
				StfrReignitionState.enter(c, w);
				break;
			case UNDERLOAD:
				StfrUnderloadState.enter(c, w);
				break;
			case MANUAL_RESTABILIZATION:
				StfrManualRestabilizationState.enter(c, w);
				break;
			case STABILIZER_LOSS:
				StfrStabilizerLossState.enter(c, w);
				break;
			case SHUTDOWN_FAILURE:
				StfrShutdownFailureState.enter(c, w);
				break;
			case SELF_DESTRUCT:
				StfrSelfDestructState.enter(c, w);
				break;
			default:
				break;
		}
	}

	static void tick(StfrEmergencyController c, World w) {
		StfrReactor.RunState state = c.r.runState();
		int tick = c.r.stateTicks();
		boolean frozen =
				state == MANUAL_RESTABILIZATION && c.stage == 5
						|| state == SHUTDOWN_FAILURE && c.recovering;
		tick = StfrEmergencyRules.advance(state, tick, c.recovering, c.stage);
		if (c.recovering) c.recovery++;
		int speed = 100;
		if (state == OVERLOAD) speed = 200 + Math.min(500, tick * 2);
		if (state == MANUAL_RESTABILIZATION) speed = 500;
		if (state == SHUTDOWN_FAILURE)
			speed = StfrEmergencyRules.shutdownSpeed(tick, c.recovering ? c.recovery : 0);
		if (state == SELF_DESTRUCT)
			speed =
					c.selfWasOffline && tick < 2485
							? 0
							: 100
									+ (tick >= 2405 ? 100 : 0)
									+ (tick >= 2850 ? 100 : 0)
									+ (tick >= 2870 ? 100 : 0)
									+ (tick >= 2890 ? 100 : 0)
									+ (tick >= 2910 ? 100 : 0);
		if (state == UNDERLOAD) speed = 0;
		if (state == SHUTDOWN_FAILURE && c.recovering && c.recovery >= 1400) speed = 0;
		c.r.emergencyTick(tick, speed);
		c.r.emergencyStopOutput();
		c.r.failure().decayBroadcastLoad(w);
		StfrEmergencyEffects.tick(w, c.r, tick, c.recovering ? Math.max(1, c.recovery) : 0);
		c.resumeMusic(w);
		for (StfrEmergencyTimeline.Event event : StfrEmergencyTimeline.EVENTS) {
			int clock = event.recoveryClock() ? c.recovery : tick;
			if (event.state() == state
					&& event.tick() == clock
					&& (event.recoveryClock()
							|| !frozen && !(state == STABILIZER_LOSS && c.recovering)))
				c.broadcast(w, event.voice(), event.text(), event.level(), event.translationKey());
		}
		for (StfrEmergencyTimeline.TimedSound event : StfrEmergencyTimeline.SOUNDS) {
			if (event.state() != state
					|| event.tick() != (event.recoveryClock() ? c.recovery : tick)
					|| !event.recoveryClock() && (frozen || state == STABILIZER_LOSS && c.recovering))
				continue;
			String id = event.id().replace("terf:", "");
			if (id.startsWith("music.")) c.music(w, id, event.volume(), event.pitch());
			else
				c.sound(
						w,
						id,
						event.category().equals("master")
								? SoundCategory.MASTER
								: SoundCategory.AMBIENT,
						event.volume(),
						event.pitch());
		}
		if (c.r.core().exists()
				&& (state == OVERLOAD
						|| state == MANUAL_RESTABILIZATION
						|| state == SHUTDOWN_FAILURE
						|| state == REIGNITION && tick >= 380)) c.r.emergencySimulate(w);
		if (state == REIGNITION && tick > 380) {
			c.checkOnline(w);
			if (c.r.runState() != state) return;
		}
		switch (state) {
			case OVERLOAD:
				StfrOverloadState.tick(c, w, tick);
				break;
			case REACTION_LOSS:
				StfrReactionLossState.tick(c, w, tick);
				break;
			case REIGNITION:
				StfrReignitionState.tick(c, w, tick);
				break;
			case UNDERLOAD:
				StfrUnderloadState.tick(c, w, tick);
				break;
			case MANUAL_RESTABILIZATION:
				StfrManualRestabilizationState.tick(c, w, tick);
				break;
			case STABILIZER_LOSS:
				StfrStabilizerLossState.tick(c, w, tick);
				break;
			case SHUTDOWN_FAILURE:
				StfrShutdownFailureState.tick(c, w, tick);
				break;
			case SELF_DESTRUCT:
				StfrSelfDestructState.tick(c, w, tick);
				break;
			default:
				break;
		}
	}
}

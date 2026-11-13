package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrReactionLossTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							2105,
							"stfr.reaction_loss.instability",
							"EXTREME CORE INSTABILITY! Systems Predict a High Energy Explosion In"
								+ " ~T-30 Seconds!",
							4),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							20,
							"stfr.reaction_loss.stabilizer_fail",
							"Reactor Stabilizer Rotor Failure Detected! Calculating Currently"
								+ " Avaliable Options...",
							3),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							450,
							"stfr.reaction_loss.restab_options",
							"Action Found!",
							1),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							451,
							"",
							"The stabilizers' EMITTER blocks have been exhausted.",
							0),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							452,
							"",
							"The plasma lenses have frozen.",
							0),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							453,
							"",
							"Install the original blocks assigned to the EMITTER role.",
							0),
					new Event(
							StfrReactor.RunState.REACTION_LOSS,
							false,
							454,
							"",
							"Restore emitters in at least five stabilizers immediately!",
							0));
	static final List<TimedSound> SOUNDS = List.of();

	private StfrReactionLossTimeline() {}
}

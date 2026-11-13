package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrReignitionTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.REIGNITION,
							false,
							280,
							"stfr.reaction_loss.restab_firing",
							"Firing Power And Stabilization Lasers at 5.000.000% To Re-Stabilize"
								+ " Reactor Core. Brace For Impact!",
							2));
	static final List<TimedSound> SOUNDS = List.of();

	private StfrReignitionTimeline() {}
}

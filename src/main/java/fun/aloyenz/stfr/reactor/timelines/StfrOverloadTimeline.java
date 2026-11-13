package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrOverloadTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.OVERLOAD,
							false,
							20,
							"stfr.overload.overdrive",
							"Overdriving Stabilizers To Maintain Internal Shield Intensity...",
							3),
					new Event(
							StfrReactor.RunState.OVERLOAD,
							false,
							320,
							"stfr.overload.restab_options",
							"Restabilization Options Found:",
							1),
					new Event(
							StfrReactor.RunState.OVERLOAD,
							false,
							321,
							"",
							"- Replace plasma lenses with auxiliary quartz lenses in at least four"
								+ " stabilizers, set KEY to ON, then press SSTB.",
							0),
					new Event(
							StfrReactor.RunState.OVERLOAD,
							false,
							820,
							"stfr.overload.plasma",
							"Electromagnetic Systems At Critical Temperature! Unpredictable Plasma"
								+ " Behaviour Expected.",
							3),
					new Event(
							StfrReactor.RunState.OVERLOAD,
							false,
							1220,
							"stfr.overload.field",
							"High Probability Of Plasma Detachment Due To Chamber-Wide Magnetic"
								+ " Field Instability!",
							3));
	static final List<TimedSound> SOUNDS = List.of();

	private StfrOverloadTimeline() {}
}

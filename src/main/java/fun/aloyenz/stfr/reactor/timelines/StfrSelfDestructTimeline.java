package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrSelfDestructTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							100,
							"",
							"Self-Destruct Protocol Has Been Initialized...",
							2),
					new Event(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							2405,
							"",
							"Self-Destruct Protocol Is Now In Effect.",
							3),
					new Event(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							2800,
							"",
							"Removing Limits From Stabilizer Power Settings... Stabilizer Boost"
								+ " Level Is Now: 2,147,483,647",
							3),
					new Event(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							3360,
							"",
							"Extreme Stabilizer Overload! Total System Disassembly Imminent. Brace"
								+ " For Impact!",
							4),
					new Event(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							3700,
							"",
							"Monitoring Error! CODE: POINTOFNORETURNSETPOINT_REACHED",
							5));
	static final List<TimedSound> SOUNDS =
			List.of(
					new TimedSound(
							StfrReactor.RunState.SELF_DESTRUCT,
							false,
							3320,
							"terf:powerpurge",
							"ambient",
							10.0F,
							2.0F));

	private StfrSelfDestructTimeline() {}
}

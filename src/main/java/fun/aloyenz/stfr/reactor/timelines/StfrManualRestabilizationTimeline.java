package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrManualRestabilizationTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							10,
							"stfr.manual_restab.attempt_aux_shield",
							"Attempting To Form Auxilary Containment Shield!",
							1),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							340,
							"stfr.manual_restab.aux_shield_formed",
							"Auxilary Containment Shield Formed! Please Attempt The Following Tasks"
								+ " Before The Auxilary Shield Collapses:",
							1),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							350,
							"stfr.manual_restab.disconnect_panel",
							"Current Objective: Disconnect Control Panel Via. Mainframe To Bypass"
								+ " Security Measures",
							1),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							1300,
							"stfr.manual_restab.fail_in_2m",
							"Time Until Auxilary Shield Failure: T-2 minutes",
							2),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							2510,
							"stfr.manual_restab.fail_in_60s",
							"Time Until Auxilary Shield Failure: T-60s",
							2),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							3110,
							"stfr.manual_restab.fail_in_30s",
							"Time Until Auxilary Shield Failure: T-30s",
							2),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							3510,
							"stfr.manual_restab.fail_in_10s",
							"Time Until Auxilary Shield Failure: T-10s",
							2),
					new Event(
							StfrReactor.RunState.MANUAL_RESTABILIZATION,
							false,
							3610,
							"",
							"Time Until Auxilary Shield Failure: T-5s",
							2));
	static final List<TimedSound> SOUNDS = List.of();

	private StfrManualRestabilizationTimeline() {}
}

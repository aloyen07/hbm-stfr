package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrUnderloadTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							30,
							"stfr.underload.system_damage",
							"Major System Damage! Unable To Retreive Reactor Core Vitals",
							3),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							300,
							"stfr.underload.stabilization_failure",
							"Complete Stabilization Failure! Cannot Confirm Reactor Core Offline"
								+ " State",
							5),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							500,
							"stfr.underload.connecting_backup_systems",
							"Attempting To Connect Backup Systems, Standby...",
							0),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							700,
							"stfr.underload.backup_systems_online",
							"Backup Systems Online",
							0),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							720,
							"stfr.underload.scanning",
							"Scanning Reactor Core To Confirm Offline State...",
							0),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							900,
							"stfr.underload.no_match",
							"Cannot Confirm Reactor Core Offline State: No Match Found In Database",
							0),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							920,
							"stfr.underload.field_increasing",
							"Core Magnetic Field Rapidly Increasing! Monitoring...",
							0),
					new Event(
							StfrReactor.RunState.UNDERLOAD,
							false,
							1130,
							"stfr.underload.evacuate",
							"Extreme Structural Stress, A Complete Reactor Structural Failure May"
								+ " Occour! Evacuate The Facility Immediately.",
							4));
	static final List<TimedSound> SOUNDS = List.of(
            new TimedSound(StfrReactor.RunState.UNDERLOAD,false,320,
                    "terf:music.remi_galleno_panic_track","master",16F,.9F),
            new TimedSound(StfrReactor.RunState.UNDERLOAD,false,3400,
                    "terf:charge_up.mega_chargeup","master",2F,2F));

    private StfrUnderloadTimeline() {}
}

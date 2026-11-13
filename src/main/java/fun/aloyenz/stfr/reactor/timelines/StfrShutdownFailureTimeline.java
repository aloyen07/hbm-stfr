package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrShutdownFailureTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							300,
							"stfr.shut_fail.projection",
							"Increasing Shield Projection To Prevent Collapse From Low Internal"
								+ " Pressure Forces!",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							600,
							"stfr.shut_fail.ineffective",
							"Automatic Shutdown Ineffective! Manual Intervention Required.",
							2),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							750,
							"stfr.shut_fail.restab_options",
							"Re-stabilization Options Found:",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							752,
							"",
							"- Replace at least four fuel capsules with water-filled capsules and"
								+ " lock them.",
							0),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							753,
							"",
							"Set KEY to ON and press SSTB before monitoring fails to purge the"
								+ " core.",
							0),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							1200,
							"stfr.shut_fail.diameter",
							"Core Diameter Decreasing Due To Extreme Shield Densities! Core Spin"
								+ " Levels Rising Exponentially!",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							1350,
							"stfr.shut_fail.permeability",
							"Shield Reverse-Permeability Alert!",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							1650,
							"stfr.shut_fail.foreign",
							"Foreign Material Detected Within Reactor Core! Systems May Not Behave"
								+ " As Expected!",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							1950,
							"stfr.shut_fail.pressure",
							"Detecting Extreme Core Pressure Buildup! All Personnel Are To Attempt"
									+ " Re-stabilization Immediately.",
							2),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							2250,
							"stfr.shut_fail.reactivity",
							"Unknown Reactivity Rise Detected! Analysis pending...",
							2),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							2550,
							"stfr.shut_fail.fusion",
							"Fusion Of A New Element Detected! Reconfiguring Systems... Details:"
								+ " Details: p+Num:8",
							2),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							2850,
							"stfr.shut_fail.setpoint",
							"Automatic Shield Permeability Setpoint Unreachable Before A"
								+ " Containment Loss Event Occurs! Awaiting Intervention...",
							2),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							3150,
							"stfr.shut_fail.temp_fail",
							"Unable To Monitor Reactor Core Temperature! Switching To Approximation"
								+ " Via. Photon Spectrometers.",
							1),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							4050,
							"stfr.shut_fail.monitoring_fail",
							"Total Monitoring Failure! Analyzing previous stats...",
							3),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							false,
							4350,
							"stfr.shut_fail.instability",
							"Extreme Shield Instability! Systems Predict A Complete Containment"
								+ " Failure In T-30 Seconds!",
							4),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							true,
							100,
							"stfr.shut_fail.restab_inject",
							"Injecting Coolant Into Reactor Core To Reduce Stabilizer"
								+ " Backpressure...",
							0),
					new Event(
							StfrReactor.RunState.SHUTDOWN_FAILURE,
							true,
							400,
							"stfr.shut_fail.restab_overdrive",
							"Overdriving Fuel Channel Fields For The Venting Of Core Contents!"
								+ " Stabilizer Damage Will Occur.",
							2));
	static final List<TimedSound> SOUNDS = List.of();

	private StfrShutdownFailureTimeline() {}
}

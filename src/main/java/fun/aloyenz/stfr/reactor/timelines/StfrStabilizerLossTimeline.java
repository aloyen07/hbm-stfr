package fun.aloyenz.stfr.reactor.timelines;

import java.util.List;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.Event;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline.TimedSound;

final class StfrStabilizerLossTimeline {

	static final List<Event> EVENTS =
			List.of(
					new Event(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							300,
							"",
							"Restabilization Options Found:",
							1),
					new Event(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							301,
							"",
							"- Repair At Least 3 Stabilizers, Then Confirm Restabilization Via."
								+ " SSTB Before The Stabilizers Overheat.",
							0),
					new Event(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							1636,
							"stfr.stab_loss.fail_in_2m",
							"Estimated Time Until Stabilizer Destruction: T-2 Minutes",
							2),
					new Event(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							2785,
							"stfr.stab_loss.fail_in_1m",
							"Estimated Time Until Stabilizer Destruction: T-1 Minute",
							2),
					new Event(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3340,
							"stfr.stab_loss.fail_in_30s",
							"Estimated Time Until Stabilizer Destruction: T-30 Seconds",
							2));
	static final List<TimedSound> SOUNDS =
			List.of(
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F),
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F),
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F),
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F),
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F),
					new TimedSound(
							StfrReactor.RunState.STABILIZER_LOSS,
							false,
							3960,
							"terf:dem.metal_2",
							"master",
							10.0F,
							0.8F));

	private StfrStabilizerLossTimeline() {}
}

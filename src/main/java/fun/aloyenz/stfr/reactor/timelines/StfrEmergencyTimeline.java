package fun.aloyenz.stfr.reactor.timelines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Timed broadcasts extracted from original STFR; event clocks are reactor-local. */
public final class StfrEmergencyTimeline {

	private StfrEmergencyTimeline() {}

	public record Event(
			StfrReactor.RunState state,
			boolean recoveryClock,
			int tick,
			String voice,
			String text,
			int level) {
		public String translationKey() {
			return voice.isEmpty()
					? "message.hbmstfr.scene."
							+ state.name().toLowerCase(Locale.ROOT)
							+ "."
							+ (recoveryClock ? "recovery." : "")
							+ tick
					: "message.hbmstfr.scene." + voice;
		}
	}

	public static final List<Event> EVENTS = joinEvents();

	private static List<Event> joinEvents() {
		List<Event> events = new ArrayList<>();
		events.addAll(StfrOverloadTimeline.EVENTS);
		events.addAll(StfrReactionLossTimeline.EVENTS);
		events.addAll(StfrReignitionTimeline.EVENTS);
		events.addAll(StfrUnderloadTimeline.EVENTS);
		events.addAll(StfrManualRestabilizationTimeline.EVENTS);
		events.addAll(StfrStabilizerLossTimeline.EVENTS);
		events.addAll(StfrShutdownFailureTimeline.EVENTS);
		events.addAll(StfrSelfDestructTimeline.EVENTS);
		return Collections.unmodifiableList(events);
	}

	public record TimedSound(
			StfrReactor.RunState state,
			boolean recoveryClock,
			int tick,
			String id,
			String category,
			float volume,
			float pitch) {}

	public static final List<TimedSound> SOUNDS = joinSounds();

	private static List<TimedSound> joinSounds() {
		List<TimedSound> sounds = new ArrayList<>();
		sounds.addAll(StfrOverloadTimeline.SOUNDS);
		sounds.addAll(StfrReactionLossTimeline.SOUNDS);
		sounds.addAll(StfrReignitionTimeline.SOUNDS);
		sounds.addAll(StfrUnderloadTimeline.SOUNDS);
		sounds.addAll(StfrManualRestabilizationTimeline.SOUNDS);
		sounds.addAll(StfrStabilizerLossTimeline.SOUNDS);
		sounds.addAll(StfrShutdownFailureTimeline.SOUNDS);
		sounds.addAll(StfrSelfDestructTimeline.SOUNDS);
		return Collections.unmodifiableList(sounds);
	}

	public static int deadline(StfrReactor.RunState state) {
		switch (state) {
			case REACTION_LOSS:
				return 2705;
			case REIGNITION:
				return 1000;
			case UNDERLOAD:
				return 3640;
			case MANUAL_RESTABILIZATION:
				return 3710;
			case STABILIZER_LOSS:
				return 4000;
			case SHUTDOWN_FAILURE:
				return 5300;
			case SELF_DESTRUCT:
				return 3740;
			default:
				return 0;
		}
	}

	public static boolean canAbortSelfDestruct(int tick) {
		return tick < 2096;
	}

	public static boolean instructionsComplete(StfrReactor.RunState state, int tick) {
		switch (state) {
			case STARTUP_FAILURE:
				return tick >= 493;
			case OVERLOAD:
				return tick >= 321;
			case REACTION_LOSS:
				return tick >= 455;
			case REIGNITION:
				return tick >= 280;
			case UNDERLOAD:
				return tick >= 1130;
			case MANUAL_RESTABILIZATION:
				return tick >= 350;
			case STABILIZER_LOSS:
				return tick >= 301;
			case SHUTDOWN_FAILURE:
				return tick >= 753;
			case SELF_DESTRUCT:
				return tick >= 100;
			default:
				return false;
		}
	}
}

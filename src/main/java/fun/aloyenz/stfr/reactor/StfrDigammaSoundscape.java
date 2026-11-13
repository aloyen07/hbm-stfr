package fun.aloyenz.stfr.reactor;

/** One mutually exclusive sound draw every 600 reactor ticks. */
public final class StfrDigammaSoundscape {

	public enum Cue {
		HUM,
		PURGE,
		ROCKET,
		POWER_PURGE,
		SCREAM,
		SECRET_SCREAM,
		SILENCE
	}

	private StfrDigammaSoundscape() {}

	public static Cue select(double roll, boolean brokenPolaroid) {
		if (roll < .5D) return Cue.HUM;
		if (roll < .55D) return Cue.PURGE;
		if (roll < .65D) return Cue.ROCKET;
		if (roll < .66D) return Cue.POWER_PURGE;
		if (roll < .661D) return Cue.SCREAM;
		if (roll < .6611D && brokenPolaroid) return Cue.SECRET_SCREAM;
		return Cue.SILENCE;
	}
}

package fun.aloyenz.stfr.reactor.timelines;

/** Tick boundaries taken from TERF; deliberately independent of world/render code. */
public final class StfrFailureTimeline {

	public static final int CASE_BREACH = 3789;
	public static final int MELTDOWN = 8040;
	public static final int DETONATION = 1398;
	public static final int RECOVERY = 600;

	/** Three visual pulses; the existing final nuclear deadline is unchanged. */
	public static int visualBlastStage(int tick) {
		return tick==1280?1:tick==1320?2:tick==1360?3:0;
	}

	private StfrFailureTimeline() {}

	public static int advanceFailure(int tick, boolean recovering) {
		return recovering ? tick : tick + 1;
	}

	public static boolean recoveryComplete(int progress) {
		return progress > RECOVERY;
	}

	public static boolean meltdownDue(int tick, boolean recovering) {
		return !recovering && tick >= MELTDOWN;
	}

	public static boolean detonationDue(int tick) {
		return tick >= DETONATION;
	}

	public static boolean recoveryAllowed(
			int tick, int recoveryTicks, long energy, long charge, int stabilizers) {
		return tick >= 483
				&& tick < MELTDOWN
				&& recoveryTicks == 0
				&& charge > 0
				&& energy >= charge
				&& stabilizers >= 1;
	}

	public static String countdown(int tick) {
		switch (tick) {
			case 1390:
				return "5.5m";
			case 1990:
				return "5m";
			case 3040:
				return "4.5m";
			case 3640:
				return "4m";
			case 4690:
				return "3m";
			case 5290:
				return "2.5m";
			case 6340:
				return "60s";
			case 7090:
				return "30s";
			default:
				return "";
		}
	}
}

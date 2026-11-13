package fun.aloyenz.stfr.reactor.physics;

/** The shield blast starts breakup; the charging sound intensifies it ten seconds later. */
public final class StfrShutdownDebris {
	public static final int START = 4950, CHARGE = 5150, END = 5300;
	private StfrShutdownDebris() {}

	public static int releaseCount(int tick, boolean recovering) {
		if (recovering || tick < START || tick >= END || tick % 4 != START % 4) return 0;
		return tick < CHARGE ? 4 : 8;
	}
}

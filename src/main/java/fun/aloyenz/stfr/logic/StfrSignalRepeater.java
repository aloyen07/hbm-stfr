package fun.aloyenz.stfr.logic;

/** Independent hold timers; a new edge still executes immediately. */
public final class StfrSignalRepeater {

	private final int[] held = new int[6];

	public void reset(int face) {
		held[face] = 0;
	}

	public boolean tick(int face, boolean heldSignal, int interval) {
		if (!heldSignal || interval <= 0) {
			reset(face);
			return false;
		}
		if (++held[face] >= interval) {
			reset(face);
			return true;
		}
		return false;
	}
}

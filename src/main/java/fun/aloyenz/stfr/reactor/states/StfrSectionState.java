package fun.aloyenz.stfr.reactor.states;

/** Fuel service changes availability, never structural health. */
public final class StfrSectionState {

	public static int enteredRefueling(
			int previousStructuralMask, int previousRefuelingMask, int refuelingMask) {
		return previousStructuralMask & ~previousRefuelingMask & refuelingMask;
	}

	private StfrSectionState() {}

	public static String state(int structuralMask, int refuelingMask, int section) {
		int bit = 1 << section;
		return (structuralMask & bit) == 0
				? "FAILED"
				: (refuelingMask & bit) != 0 ? "REFUELING" : "READY";
	}

	public static int damaged(int structuralMask) {
		return 6 - Integer.bitCount(structuralMask & 63);
	}
}

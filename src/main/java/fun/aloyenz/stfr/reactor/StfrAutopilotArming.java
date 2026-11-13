package fun.aloyenz.stfr.reactor;

final class StfrAutopilotArming {

	private StfrAutopilotArming() {}

	static boolean hasBlockingWarnings(int flags, boolean allowSeal, boolean allowStructure) {
		int tolerated = 0;
		if (allowSeal) tolerated |= 1 << 24;
		if (allowStructure) tolerated |= (1 << 22) | (1 << 23);
		return (flags & ~tolerated) != 0;
	}
}

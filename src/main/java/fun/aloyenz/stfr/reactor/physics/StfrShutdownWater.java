package fun.aloyenz.stfr.reactor.physics;

/** Water reserved once when the shutdown-failure purge is confirmed. */
public final class StfrShutdownWater {
	public static final int CAPSULES = 4;
	public static final int PER_CAPSULE = 10_000;
	private StfrShutdownWater() {}

	public static boolean enough(int amount) { return amount >= PER_CAPSULE; }
}

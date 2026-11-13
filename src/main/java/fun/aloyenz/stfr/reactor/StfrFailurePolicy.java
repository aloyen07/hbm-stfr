package fun.aloyenz.stfr.reactor;

/** HBM MK3 cannot independently disable its terrain, damage and fallout passes. */
public final class StfrFailurePolicy {

	public static final int STRUCTURE = 1,
			SHOCKWAVE = 2,
			NUCLEAR = 4,
			HIGH_ENERGY = 8,
			FIRE = 16,
			PLAYER_DAMAGE = 32,
			ALL = 63;

	private StfrFailurePolicy() {}

	public static boolean allows(int mask, int category) {
		return (mask & category) == 0;
	}

	public static boolean allowsHbm(int mask, boolean nuclear) {
		return allows(mask, (nuclear ? NUCLEAR : HIGH_ENERGY) | STRUCTURE | FIRE | PLAYER_DAMAGE);
	}
}

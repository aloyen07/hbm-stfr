package fun.aloyenz.stfr.reactor.physics;

/** Vanilla-shaped explosion falloff. Exposure is the fraction not blocked by terrain. */
public final class StfrBlastDamage {
	private StfrBlastDamage() {}
	public static double impact(double strength, double distance, double exposure) {
		if (strength <= 0) return 0;
		return Math.max(0, 1 - Math.max(0, distance)/(strength*2))
				* Math.max(0, Math.min(1, exposure));
	}
	public static float damage(double strength, double distance, double exposure) {
		double amount = impact(strength, distance, exposure);
		return amount <= 0 ? 0 : (float) ((amount*amount+amount)*.5*7*(strength*2)+1);
	}
}

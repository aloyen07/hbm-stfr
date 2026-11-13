package fun.aloyenz.stfr.tile;

/** Shared transfer limit for either capsule filling path. */
public final class StfrCapsuleFilling {
	private StfrCapsuleFilling() {}

	public static int amount(int current, int capacity, int source, int companion) {
		return Math.max(0, Math.min(1000,
				Math.min(capacity - current, Math.min(source, companion))));
	}

	/** 1000 mB water + 100 mB nitrogen -> 1000 mB capsule water. */
	public static int waterAmount(int current, int capacity, int water, int nitrogen) {
		return Math.max(0, Math.min(1000,
				Math.min(capacity - current, Math.min(water, nitrogen * 10))));
	}

	public static int nitrogenCost(int waterOutput) {
		return Math.max(0, (waterOutput + 9) / 10);
	}
}

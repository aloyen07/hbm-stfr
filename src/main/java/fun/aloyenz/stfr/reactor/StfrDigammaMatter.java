package fun.aloyenz.stfr.reactor;

import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.StfrConfig;

/** Po-210 and yaronite in the core use the same internal unit scale as fuel. */
final class StfrDigammaMatter {

	private static final double UNITS_PER_MB = StfrCoreSimulation.PLASMA_UNITS_PER_MB;
	private static final double FULL_REACTION_POWER = 4000D;
	private double poloniumUnits, yaroniteUnits;
	// Older saves could hold thousands of mB in the core. Keep the excess in a
	// reserve and feed/export it gradually instead of deleting scarce matter.
	private double legacyPoloniumMb, legacyYaroniteMb;

	double poloniumMb() {
		return poloniumUnits / UNITS_PER_MB;
	}

	double yaroniteMb() {
		return yaroniteUnits / UNITS_PER_MB;
	}

	double legacyPoloniumMb() {
		return legacyPoloniumMb;
	}

	double legacyYaroniteMb() {
		return legacyYaroniteMb;
	}

	double acceptPolonium(double offeredMb) {
		if (!Double.isFinite(offeredMb) || offeredMb <= 0D) return 0D;
		double space = Math.max(0D, StfrConfig.maxCorePoloniumMb - poloniumMb());
		double accepted = Math.min(offeredMb, space);
		poloniumUnits += accepted * UNITS_PER_MB;
		return accepted;
	}

	void feedLegacyReserve() {
		if (legacyPoloniumMb <= 0D) return;
		double offered =
				Math.min(legacyPoloniumMb, StfrConfig.digammaConversionMbPerMinute / 1200D);
		legacyPoloniumMb -= acceptPolonium(offered);
		if (legacyPoloniumMb < 1E-10) legacyPoloniumMb = 0D;
	}

	double convert(double reactionPower) {
		if (!Double.isFinite(reactionPower) || reactionPower <= 0D) return 0D;
		double powerFraction = Math.min(1D, reactionPower / FULL_REACTION_POWER);
		double rate = StfrConfig.digammaConversionMbPerMinute / 1200D * powerFraction;
		double amount =
				Math.min(
						rate,
						Math.min(
								poloniumMb(),
								Math.max(0D, StfrConfig.maxCoreYaroniteMb - yaroniteMb())));
		poloniumUnits -= amount * UNITS_PER_MB;
		yaroniteUnits += amount * UNITS_PER_MB;
		if (poloniumUnits < 1E-8) poloniumUnits = 0D;
		return amount;
	}

	/** Export whole HBM mB; retain fractional product in the core. */
	int takeYaronite(int room) {
		if (room <= 0) return 0;
		int whole = (int) Math.min(room, Math.floor(yaroniteMb() + 1E-9));
		yaroniteUnits -= whole * UNITS_PER_MB;
		if (yaroniteUnits < 1E-8) yaroniteUnits = 0D;
		return whole;
	}

	void feedLegacyYaronite() {
		double moved =
				Math.min(
						legacyYaroniteMb,
						Math.max(0D, StfrConfig.maxCoreYaroniteMb - yaroniteMb()));
		if (moved <= 0D) return;
		legacyYaroniteMb -= moved;
		yaroniteUnits += moved * UNITS_PER_MB;
		if (legacyYaroniteMb < 1E-9) legacyYaroniteMb = 0D;
	}

	void clear() {
		poloniumUnits = yaroniteUnits = legacyPoloniumMb = legacyYaroniteMb = 0D;
	}

	void write(NBTTagCompound tag) {
		tag.setDouble("poloniumMb", poloniumMb());
		tag.setDouble("yaroniteMb", yaroniteMb());
		tag.setDouble("poloniumReserveMb", legacyPoloniumMb);
		tag.setDouble("yaroniteReserveMb", legacyYaroniteMb);
	}

	void read(NBTTagCompound tag) {
		double oldPolonium = nonnegative(tag.getDouble("poloniumMb"));
		double oldYaronite = nonnegative(tag.getDouble("yaroniteMb"));
		double corePo = Math.min(oldPolonium, StfrConfig.maxCorePoloniumMb);
		double coreY = Math.min(oldYaronite, StfrConfig.maxCoreYaroniteMb);
		poloniumUnits = corePo * UNITS_PER_MB;
		yaroniteUnits = coreY * UNITS_PER_MB;
		legacyPoloniumMb = oldPolonium - corePo + nonnegative(tag.getDouble("poloniumReserveMb"));
		legacyYaroniteMb = oldYaronite - coreY + nonnegative(tag.getDouble("yaroniteReserveMb"));
	}

	private static double nonnegative(double value) {
		return Double.isFinite(value) ? Math.max(0D, value) : 0D;
	}
}

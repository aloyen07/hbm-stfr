package fun.aloyenz.stfr.reactor;

/** Shield maintenance uses all thirty independent electrode chains. */
final class StfrShieldControl {

	private StfrShieldControl() {}

	static double advance(double shield, double stress, int stabilizers, double coverage) {
		coverage = Math.max(0D, Math.min(1D, coverage));
		if (shield >= .03D && stress >= 100D)
			shield -= Math.floor(stress / 50D) / 100D * (1D + 2D * (1D - coverage));
		if ((stabilizers <= 0 || coverage <= 0) && shield >= 1D) shield -= 1D;
		else shield -= (1D - coverage) * .03D;
		if (shield <= 100D && stress <= 99D) shield += .01D * coverage;
		return Math.max(0D, Math.min(100D, shield));
	}
}

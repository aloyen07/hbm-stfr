package fun.aloyenz.stfr.reactor;

/** Electrical collection is independent of the turbine cooling water/steam circuit. */
final class StfrElectrodeGeneration {

	private StfrElectrodeGeneration() {}

	static long power(double reactionMw, double coverage, int hePerMw, long maximum) {
		if (!Double.isFinite(reactionMw)
				|| !Double.isFinite(coverage)
				|| reactionMw <= 0
				|| coverage <= 0
				|| hePerMw <= 0
				|| maximum <= 0) return 0;
		return (long) Math.floor(Math.min(maximum, reactionMw * Math.min(1D, coverage) * hePerMw));
	}
}

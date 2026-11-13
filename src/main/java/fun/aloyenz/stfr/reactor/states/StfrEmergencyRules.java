package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.Locale;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Original scoreboard boundaries, isolated from particles, packets and terrain mutations. */
public final class StfrEmergencyRules {

	private StfrEmergencyRules() {}

	public static int selfScreenRemaining(int tick) {
		return Math.max(0, (tick < 2305 ? 2096 : 3710) - tick);
	}

	public static boolean selfCountdownBeep(int tick) {
		return tick >= 200 && selfScreenRemaining(tick) % 20 == 0;
	}

	public static boolean selfAlarmBeat(long uptime) {
		long beat = (uptime + 13) % 24;
		return beat == 2 || beat == 6 || beat == 10 || beat == 14;
	}

	public static String clock(int ticks) {
		ticks = Math.max(0, ticks);
		return String.format(
				Locale.ROOT, "%d:%02d:%02d", ticks / 1200, ticks / 20 % 60, ticks % 20);
	}

	public static boolean turbineAvailable(int supercritical, boolean main, int secondary) {
		return supercritical == 0 && (main || (secondary & 15) != 0);
	}

	public static boolean nominalShutdownAllowed(double shield, double totalStress) {
		return shield >= 60 && totalStress <= 20;
	}

	public static boolean shutdownFails(int available, int refueling) {
		return (available & 63) != 63 || (refueling & 63) != 0;
	}

	public static int shutdownSpeed(int tick, int recovery) {
		int speed = tick >= 4050 ? 0 : 100 + Math.min(200, Math.max(0, tick - 300));
		if (recovery > 400) speed += Math.min(400, (recovery - 400) * 2);
		if (recovery > 990) speed = (int) Math.round(speed * Math.max(0, 1400 - recovery) / 410D);
		return recovery >= 1400 ? 0 : Math.max(0, speed);
	}

	/** A single available turbine charges at the same rate as six, only while the core exists. */
	public static int chargeSelfDestruct(
			int charge, boolean active, boolean core, int available, int limit) {
		return Math.min(
				Math.max(1, limit),
				Math.max(0, charge) + (active && core && available > 0 ? 1 : 0));
	}

	public static double selfDestructMultiplier(int charge, int limit, double maximum) {
		return 1
				+ (Math.max(1, maximum) - 1)
						* Math.min(1, Math.max(0, charge) / (double) Math.max(1, limit));
	}

	public static int blastRange(int ordinaryRange, double volumeMultiplier) {
		return Math.max(
				1, (int) Math.round(ordinaryRange * Math.cbrt(Math.max(1, volumeMultiplier))));
	}

	public static StfrReactor.RunState onlineTransition(
			int stabilizers, double shield, double stress, double collapse, double roll) {
		if (stabilizers == 0 && stress < 100) return STABILIZER_LOSS;
		// Original shield_intensity_divided truncates B / 1000.
		if (shield >= 1) return ONLINE;
		if (collapse >= 100) return roll < .003 ? REACTION_LOSS : ONLINE;
		return stabilizers == 0 || stress >= 300 || roll < .003 ? OVERLOAD : ONLINE;
	}

	public static int advance(StfrReactor.RunState state, int tick, boolean recovering, int stage) {
		if (state == STABILIZER_LOSS && recovering) return Math.max(0, tick - 10);
		if (state == SHUTDOWN_FAILURE && recovering
				|| state == MANUAL_RESTABILIZATION && stage == 5) return tick;
		return tick + 1;
	}

	public static boolean recoveryAllowed(
			StfrReactor.RunState state,
			int tick,
			int stage,
			boolean active,
			boolean key,
			boolean failed,
			int auxiliary,
			int normal,
			int stabilizers,
			boolean water) {
		if (!key || active) return false;
		switch (state) {
			case OVERLOAD:
				return !failed && auxiliary >= 4;
			case REACTION_LOSS:
				return normal >= 5;
			case STABILIZER_LOSS:
				return stabilizers >= 3;
			case SHUTDOWN_FAILURE:
				return tick < 4050 && water;
			case MANUAL_RESTABILIZATION:
				return stage == 4 && stabilizers >= 2;
			default:
				return false;
		}
	}

	public static int manualRecoveryElapsed(int elapsed) {
		return elapsed + 3;
	}
}

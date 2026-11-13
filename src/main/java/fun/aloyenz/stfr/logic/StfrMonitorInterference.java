package fun.aloyenz.stfr.logic;

import java.util.Random;

/** Per-monitor randomness that is stable within a frame and cannot last two ticks. */
public final class StfrMonitorInterference {

	private StfrMonitorInterference() {}

	public static boolean active(long monitorPosition, long tick, boolean unstable) {
		return unstable
				&& (tick & 1L) == 0
				&& new Random(monitorPosition ^ (tick * 0x9E3779B97F4A7C15L)).nextInt(12) == 0;
	}
}

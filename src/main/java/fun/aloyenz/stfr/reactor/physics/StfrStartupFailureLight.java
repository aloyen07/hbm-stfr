package fun.aloyenz.stfr.reactor.physics;

import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;

/** Progressive violet volume and delayed surface lighting, with recovery fade. */
public final class StfrStartupFailureLight {
	private StfrStartupFailureLight() {}
	private static double smooth(double value) {
		double t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);
	}
	private static double recovery(double ticks) {
		return 1-smooth(ticks/StfrFailureTimeline.RECOVERY);
	}
	public static float volume(double ticks,double recovering) {
		return (float)(.8*(1+smooth(ticks/StfrFailureTimeline.MELTDOWN))*recovery(recovering));
	}
	public static float surface(double ticks,double recovering) {
		return (float)(1.5*smooth((ticks-StfrFailureTimeline.MELTDOWN*.5)/(StfrFailureTimeline.MELTDOWN*.5))*recovery(recovering));
	}
}

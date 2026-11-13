package fun.aloyenz.stfr.reactor.physics;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.util.math.Vec3d;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.structure.StfrBlockRole;

/** Shared deterministic timing, light envelopes and inward field; no world mutations. */
public final class StfrMagneticCollapse {
	public static final int LOSS_WARNING = 2105, LOSS_END = 2705;
	public static final int VIBRATION = 720, ELECTRODES = 920, STRUCTURE = 1980;
	public static final int CHARGE = 3400, END = 3640, MAX_FRAGMENTS = 420;
	public static final double RADIUS = 16;
	private StfrMagneticCollapse() {}
	private static double ramp(double value,double from,double to) {
		return Math.max(0,Math.min(1,(value-from)/(to-from)));
	}
	/** Bernoulli trial each tick: 1.5 times the old mean release rate. */
	public static double detachmentChance(int tick) { return tick<STRUCTURE?1.5/8:1.5/4; }
	public static double strength(double tick) { return .25+.75*ramp(tick,ELECTRODES,CHARGE); }
	private static final int[] LOSS_DISCHARGES=discharges(1780,0x51A7L,false);
	private static final int[] FINAL_DISCHARGES=discharges(ELECTRODES,0x73EFL,true);
	private static int[] discharges(int first,long seed,boolean rapid) {
		int[] starts=new int[4096];Arrays.fill(starts,-1);
		Random random=new Random(seed);
		int last=first-1;
		for(int t=first;t<starts.length;t++) {
			double chance=rapid?.025+(t-last)*.035:.01+(t-last)*.007;
			if(random.nextDouble()<chance)last=t;
			if(last>=first)starts[t]=last;
		}
		return starts;
	}
	/** Shared stochastic timeline keeps electrical geometry, light and sound on the same tick. */
	public static int dischargeStart(RunState state,double tick) {
		if(tick<0 || tick>=4096)return -1;
		if(state==RunState.REACTION_LOSS)return LOSS_DISCHARGES[(int)tick];
		if(state==RunState.UNDERLOAD)return FINAL_DISCHARGES[(int)tick];
		return -1;
	}
	public static int boltCount(RunState state,double tick) {
		int start=dischargeStart(state,tick);
		return state==RunState.UNDERLOAD && start>=0 && new Random(start*719L).nextDouble()<.25?2:1;
	}
	public static double discharge(RunState state,double tick) {
		int start=dischargeStart(state,tick);
		if(start<0)return 0;
		double duration=state==RunState.UNDERLOAD?3:6;
		return Math.max(0,1-(tick-start)/duration);
	}
	public static double light(RunState state,double tick) {
		double base=state==RunState.REACTION_LOSS ? .08 : .06;
		double burst=state==RunState.UNDERLOAD ? Math.max(0,1-tick/12)*.38
				+ Math.max(0,1-Math.abs(tick-STRUCTURE)/10)*.4 : 0;
		double finale=state==RunState.UNDERLOAD ? ramp(tick,CHARGE,END)*1.5 : 0;
		int start=dischargeStart(state,tick);
		double decay=state==RunState.UNDERLOAD?3.3:6.6;
		double afterglow=start<0?0:.65*Math.exp(-(tick-start)/decay);
		return base+afterglow+burst+finale;
	}
	public static double vibration(double tick,long position) {
		if(tick<VIBRATION)return 0;
		double amount=.012+.035*ramp(tick,VIBRATION,ELECTRODES);
		tick=Math.floor(tick/2)*2;
		return amount*(Math.sin(tick*2.7+position%97)+.3*Math.sin(tick*4.3+position%31));
	}
	public static boolean electrode(StfrBlockRole role,String block) {
		return role==StfrBlockRole.ELECTRODE || block.endsWith("_electrode")
				|| block.endsWith(":electrode_mechanism");
	}
	public static boolean structural(StfrBlockRole role,int radialDepth) {
		if(role==StfrBlockRole.HATCH || role==StfrBlockRole.HATCH_FIXED || role==StfrBlockRole.HATCH_FREE)return true;
		if(radialDepth>=13)return false;
		return role==StfrBlockRole.CRITICAL || role==StfrBlockRole.SEAL
				|| role==StfrBlockRole.EXACT || role==StfrBlockRole.OPTIONAL_EFFICIENCY;
	}
	/** Exposure changes only during discharges; ordinary illumination remains continuous. */
	public static boolean exposurePulse(RunState state,double tick) {
		return discharge(state,tick)>0 || state==RunState.UNDERLOAD
				&& (tick<12 || Math.abs(tick-STRUCTURE)<10 || tick>=END-20);
	}
	/** A dominant disk mixed with polar orbits, precession and repulsion at the core. */
	public static Vec3d swarm(Vec3d relative,Vec3d previous,double age,long seed) {
		double distance=relative.length();
		Vec3d radial=distance<.001?new Vec3d(1,0,0):relative.scale(1/distance);
		if(distance<1.45)return radial.scale(.18).add(previous.scale(.12));
		long mixed=seed*0x9E3779B97F4A7C15L;
		double phase=(mixed>>>12&1023)*Math.PI*2/1024;
		double tilt=(mixed&3)==0?1.8:.45;
		Vec3d axis=new Vec3d(Math.sin(phase+age*.006)*tilt,1,Math.cos(phase+age*.006)*tilt).normalize();
		Vec3d tangent=axis.crossProduct(radial);
		if(tangent.lengthSquared()<.001)tangent=new Vec3d(1,0,0).crossProduct(radial);
		tangent=tangent.normalize();
		double radius=1.65+(mixed>>>24&31)*.006+Math.sin(age*.031+phase)*.04;
		double inward=Math.max(-.07,Math.min(.12,(distance-radius)*.12));
		Vec3d next=previous.scale(.68).add(radial.scale(-inward)).add(tangent.scale(.035))
				.add(axis.scale(Math.sin(age*.07+phase)*.012-relative.dotProduct(axis)*.001));
		double speed=next.length();return speed>.85?next.scale(.85/speed):next;
	}
	/** -1: already cleared interior, 0: current shell, 1: outside. */
	public static int shellBand(int radius,double distanceSquared) {
		return distanceSquared>radius*radius?1:distanceSquared<(radius-1)*(radius-1)?-1:0;
	}
	public static Vec3d velocity(Vec3d relative,Vec3d previous,double strength,boolean debris) {
		double distance=relative.length();
		if(distance<.001 || !debris && distance>=RADIUS)return previous;
		double near=Math.max(0,1-distance/RADIUS);
		double pull=(.004+.15*near*near)*strength;
		Vec3d tangent=new Vec3d(-relative.z,0,relative.x).normalize();
		Vec3d next=previous.scale(debris ? .91 : .97)
				.add(relative.scale(-pull/distance))
				.add(tangent.scale((debris ? .018 : .008)*near*strength));
		double speed=next.length(),limit=debris ? .85 : .65;
		return speed>limit ? next.scale(limit/speed) : next;
	}
}

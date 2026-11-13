package fun.aloyenz.stfr.meme;

/** Cosmetic sequence; deliberately has no reactor/control dependencies. */
public final class StfrCatSequence {
	public static final int IDLE=0, WARNING=1, THREAT=2, SCARE=3, SCARE_TICKS=18;
	private int stage=WARNING;
	private long until, lastClick=Long.MIN_VALUE/2;
	public int stage() { return stage; }
	public long until() { return until; }
	public long lastClick() { return lastClick; }
	public static boolean eligible(boolean online,int stabilizers,int refueling,double distanceSq,boolean reactorMonitor) {
		return online && (stabilizers<6 || refueling>0) && reactorMonitor && distanceSq<=100D*100D;
	}
	/** Returns true only on the transition which should play the one-shot sound. */
	public boolean click(long now) {
		if (stage==IDLE || stage==SCARE || now-lastClick<5) return false;
		lastClick=now;
		if (stage==WARNING) { stage=THREAT; return false; }
		stage=SCARE; until=now+SCARE_TICKS; return true;
	}
	public boolean expire(long now) {
		if (stage==SCARE && now>=until) { stage=IDLE; return true; }
		return false;
	}
	public static StfrCatSequence restore(int stage,long until,long lastClick) {
		StfrCatSequence sequence=new StfrCatSequence();
		sequence.stage=stage>=WARNING && stage<=SCARE ? stage : IDLE;
		sequence.until=until; sequence.lastClick=lastClick;
		return sequence;
	}
}

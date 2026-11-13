package fun.aloyenz.stfr.reactor.physics;

/** Mechanical runout persists after core extinction and settles with the braking rotor. */
public final class StfrRotorRunout {
	private StfrRotorRunout() {}
	public static final class Sample {
		public final double tilt, nutation, axial;
		Sample(double tilt,double nutation,double axial) {
			this.tilt=tilt; this.nutation=nutation; this.axial=axial;
		}
	}

	public static Sample sample(double recovery,double angle,double speed,int axis) {
		if (recovery<990 || recovery>=1400 || speed<=0) return new Sample(0,0,0);
		double rise=Math.min(1,(recovery-990)/10D);
		double settle=Math.sqrt(Math.max(0,(1400-recovery)/410D));
		double phase=angle*4+axis*.83+(recovery-990)*.86;
		double beat=.78+.22*Math.cos((recovery-990)*.22);
		double envelope=rise*settle*beat*Math.min(1,speed/30D);
		return new Sample(Math.sin(phase)*5.7*envelope,
				Math.cos(phase*.79)*3.2*envelope,
				Math.sin(phase*1.13)*.025*envelope);
	}
}

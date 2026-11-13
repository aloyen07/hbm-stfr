package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrRotorRunout;

final class StfrRotorRunoutTest {
	@Test void allSixStabilizersStillBeatAfterTheCoreHasGoneOut() {
		for(int axis=0;axis<6;axis++) {
			double movement=0;
			for(int tick=1000;tick<1100;tick++) {
				// A stationary angular phase must not freeze the mechanical beat.
				StfrRotorRunout.Sample sample=StfrRotorRunout.sample(tick,0,400,axis);
				movement+=Math.abs(sample.tilt)+Math.abs(sample.nutation);
			}
			assertTrue(movement>100,"Stabilizer "+axis);
		}
	}

	@Test void runoutSettlesAtFullStopAndNeverLeaksIntoNormalOperation() {
		for(double clock:new double[]{0,694,695,800,989.99,1400,1500}) {
			StfrRotorRunout.Sample sample=StfrRotorRunout.sample(clock,1,500,0);
			assertEquals(0,sample.tilt);assertEquals(0,sample.nutation);assertEquals(0,sample.axial);
		}
		StfrRotorRunout.Sample stopped=StfrRotorRunout.sample(1200,2,0,0);
		assertEquals(0,stopped.tilt);
		StfrRotorRunout.Sample last=StfrRotorRunout.sample(1399.99,1,.01,0);
		assertTrue(Math.abs(last.tilt)<.01&&Math.abs(last.nutation)<.01&&Math.abs(last.axial)<.001);
	}
}

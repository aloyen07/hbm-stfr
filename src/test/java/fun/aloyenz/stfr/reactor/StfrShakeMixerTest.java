package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrShakeMixer;

final class StfrShakeMixerTest {
	@Test void capsuleExplosionsNeverReplaceSustainedStabilizerShake() {
		StfrShakeMixer mixer=new StfrShakeMixer();
		mixer.add(400,1000,100,20,true);
		assertEquals(2,mixer.backgroundAmplitude(694),1e-9);
		mixer.add(695,705,300,20,true);
		for(int tick:new int[]{800,855,870,920,940,950}) {
			mixer.add(tick,30,100,25,false);
			mixer.add(tick,20,100,20,false);
			assertEquals(2,mixer.backgroundAmplitude(tick+1),1e-9);
			mixer.prune(tick+31);
			assertEquals(2,mixer.backgroundAmplitude(tick+31),1e-9);
		}
		assertEquals(2,mixer.backgroundAmplitude(989),1e-9);
	}

	@Test void recoveryGainHalvesEveryAngleAndOnlyCompletionFadesBackground() {
		StfrShakeMixer mixer=new StfrShakeMixer();
		mixer.add(695,705,300,20,true);
		mixer.add(800,30,100,25,false);
		StfrShakeMixer.Sample normal=mixer.sample(801.5,1),recovery=mixer.sample(801.5,.5);
		assertEquals(normal.pitch*.5,recovery.pitch,1e-9);
		assertEquals(normal.yaw*.5,recovery.yaw,1e-9);
		assertEquals(normal.roll*.5,recovery.roll,1e-9);
		mixer.fadeBackground(990,410);
		assertEquals(2,mixer.backgroundAmplitude(990),1e-9);
		assertEquals(1,mixer.backgroundAmplitude(1195),1e-9);
		assertEquals(0,mixer.backgroundAmplitude(1400),1e-9);
		assertTrue(mixer.prune(1400));
	}

	@Test void longRunningBurstSpamDoesNotAccumulateExpiredShake() {
		StfrShakeMixer mixer=new StfrShakeMixer();
		for(int tick=0;tick<10000;tick++) mixer.add(tick,20,100,20,false);
		assertTrue(mixer.prune(10020));
	}
}

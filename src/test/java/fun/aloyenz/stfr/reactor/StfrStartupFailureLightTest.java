package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrStartupFailureLight;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;

class StfrStartupFailureLightTest {
	@Test void volumeDoublesAndSurfaceLightStartsHalfwayThroughFailure() {
		assertEquals(.8F,StfrStartupFailureLight.volume(0,0),.0001);
		assertEquals(1.6F,StfrStartupFailureLight.volume(StfrFailureTimeline.MELTDOWN,0),.0001);
		assertEquals(0,StfrStartupFailureLight.surface(StfrFailureTimeline.MELTDOWN/2,0));
		float previousVolume=0,previousSurface=0;
		for(int t=0;t<=StfrFailureTimeline.MELTDOWN;t++) {
			float volume=StfrStartupFailureLight.volume(t,0),surface=StfrStartupFailureLight.surface(t,0);
			assertTrue(volume>=previousVolume && surface>=previousSurface);
			assertTrue(volume-previousVolume<.801 && surface-previousSurface<.002);
			previousVolume=volume;previousSurface=surface;
		}
	}
	@Test void recoveryFadesBothEffectsToZero() {
		float volume=StfrStartupFailureLight.volume(7000,0),surface=StfrStartupFailureLight.surface(7000,0);
		for(int t=0;t<=600;t++) {
			float nextVolume=StfrStartupFailureLight.volume(7000,t),nextSurface=StfrStartupFailureLight.surface(7000,t);
			assertTrue(nextVolume<=volume && nextSurface<=surface);
			volume=nextVolume;surface=nextSurface;
		}
		assertEquals(0,volume);assertEquals(0,surface);
	}
	@Test void reignitionConfirmationIsNoLongerAFixedTimeInstruction() {
		assertFalse(StfrEmergencyTimeline.EVENTS.stream().anyMatch(e ->
			e.state()==StfrReactor.RunState.REACTION_LOSS && e.translationKey().equals("message.hbmstfr.scene.reaction_loss.455")));
	}
}

package fun.aloyenz.stfr.reactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;

public final class StfrSafetyRulesTest {
	@Test
	void dirtyShutdownCannotReplaceAnyContingencyOrRecovery() {
		for (RunState state : RunState.values()) {
			if (state == RunState.ONLINE || state == RunState.STARTUP_CONFIRMED
					|| state == RunState.SHUTDOWN) {
				Assertions.assertTrue(StfrDirtyShutdown.allowedFrom(state), state.name());
			} else {
				Assertions.assertFalse(StfrDirtyShutdown.allowedFrom(state),
						"A damaged vessel must not reset " + state + " into normal shutdown");
			}
		}
		Assertions.assertTrue(StfrEmergencyRules.recoveryAllowed(
				RunState.SHUTDOWN_FAILURE, 2000, 0, false, true, false, 0, 0, 4, true));
		Assertions.assertFalse(StfrEmergencyRules.recoveryAllowed(
				RunState.SHUTDOWN_FAILURE, 2000, 0, false, false, false, 0, 0, 4, true));
	}

	@Test
	void autopilotArmsOnlyWithPermittedWarnings() {
		Assertions.assertFalse(StfrAutopilotArming.hasBlockingWarnings(
				(1 << 24) | (1 << 23), true, true));
		Assertions.assertTrue(StfrAutopilotArming.hasBlockingWarnings(
				(1 << 24) | (1 << 23) | (1 << 4), true, true));
		Assertions.assertTrue(StfrAutopilotArming.hasBlockingWarnings(1 << 24, false, true));
		Assertions.assertTrue(StfrAutopilotArming.hasBlockingWarnings(1 << 23, true, false));
	}

	@Test
	void criticalHatchesWaitForLastInstruction() {
		Assertions.assertFalse(StfrEmergencyTimeline.instructionsComplete(RunState.REACTION_LOSS, 454));
		Assertions.assertTrue(StfrEmergencyTimeline.instructionsComplete(RunState.REACTION_LOSS, 455));
		Assertions.assertFalse(StfrEmergencyTimeline.instructionsComplete(RunState.STARTUP_FAILURE, 492));
		Assertions.assertTrue(StfrEmergencyTimeline.instructionsComplete(RunState.STARTUP_FAILURE, 493));
	}

	@Test
	void brokenLightningRodsIncreaseDamageAndShockFrequency() {
		Assertions.assertEquals(200, StfrLightningRodHazard.interval(1));
		Assertions.assertEquals(100, StfrLightningRodHazard.interval(2));
		Assertions.assertEquals(2F, StfrLightningRodHazard.damage(1));
		Assertions.assertEquals(4F, StfrLightningRodHazard.damage(2));
	}
}

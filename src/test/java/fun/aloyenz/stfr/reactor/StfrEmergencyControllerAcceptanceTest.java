package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.security.StfrAccessPolicy;

/** Exercise competing physical thresholds, recovery tasks, event clocks and bearer rights. */
public final class StfrEmergencyControllerAcceptanceTest {

	private static void check(boolean value, String message) {
		Assertions.assertTrue(value, message);
	}

	private static boolean recover(
			StfrReactor.RunState state,
			int tick,
			int stage,
			boolean active,
			boolean key,
			boolean failed,
			int quartz,
			int normal,
			int stabilizers,
			boolean water) {
		return StfrEmergencyRules.recoveryAllowed(
				state, tick, stage, active, key, failed, quartz, normal, stabilizers, water);
	}

	@Test
	void acceptanceScenarios() {
		check(
				StfrDigammaSoundscape.select(0D, false) == StfrDigammaSoundscape.Cue.HUM,
				"Digamma hum covers the first 50%");
		check(
				StfrDigammaSoundscape.select(.5D, false) == StfrDigammaSoundscape.Cue.PURGE,
				"Digamma purge begins at 50%");
		check(
				StfrDigammaSoundscape.select(.55D, false) == StfrDigammaSoundscape.Cue.ROCKET,
				"Digamma rocket begins at 55%");
		check(
				StfrDigammaSoundscape.select(.65D, false) == StfrDigammaSoundscape.Cue.POWER_PURGE,
				"Digamma power purge begins at 65%");
		check(
				StfrDigammaSoundscape.select(.66D, false) == StfrDigammaSoundscape.Cue.SCREAM,
				"Normal scream begins at 66%");
		check(
				StfrDigammaSoundscape.select(.661D, true)
						== StfrDigammaSoundscape.Cue.SECRET_SCREAM,
				"Broken polaroid unlocks secret scream");
		check(
				StfrDigammaSoundscape.select(.661D, false) == StfrDigammaSoundscape.Cue.SILENCE,
				"Secret chance remains silent without broken polaroid");
		check(
				StfrDigammaSoundscape.select(.6611D, true) == StfrDigammaSoundscape.Cue.SILENCE,
				"Remaining chance is silence");
		check(
				StfrEmergencyRules.onlineTransition(6, 100, 500, 1000, 0) == ONLINE,
				"A healthy shield prevents collapse despite stress");
		check(
				StfrEmergencyRules.onlineTransition(0, 100, 99, 0, .9) == STABILIZER_LOSS,
				"No stabilizers enters contingency before shield collapse");
		check(
				StfrEmergencyRules.onlineTransition(0, 100, 100, 0, .9) == ONLINE,
				"Contingency stress ceiling is strict");
		check(
				StfrEmergencyRules.onlineTransition(6, .99, 299, 99, .002999) == OVERLOAD,
				"Original integer shield quotient and 0.3% boundary");
		check(
				StfrEmergencyRules.onlineTransition(6, 0, 299, 99, .003) == ONLINE,
				"0.3 means 0.3%, not 30%");
		check(
				StfrEmergencyRules.onlineTransition(6, 0, 300, 99, .9) == OVERLOAD,
				"Stress divided by 50 reaches 6 immediately");
		check(
				StfrEmergencyRules.onlineTransition(0, 0, 100, 99, .9) == OVERLOAD,
				"No working stabilizers bypasses random overload entry");
		check(
				StfrEmergencyRules.onlineTransition(6, 0, 300, 100, .9) == ONLINE,
				"Severe collapse still uses its own random gate");
		check(
				StfrEmergencyRules.onlineTransition(6, 0, 300, 100, .002) == REACTION_LOSS,
				"Rigidity determines reaction-loss branch");
		check(
				!recover(OVERLOAD, 320, 0, false, true, false, 3, 6, 6, false),
				"Three auxiliary sectors cannot stabilize overload");
		check(
				recover(OVERLOAD, 320, 0, false, true, false, 4, 2, 2, false),
				"Four real quartz sectors suffice");
		check(
				!recover(OVERLOAD, 320, 0, false, true, true, 6, 6, 6, false),
				"Failed manual recovery cannot be retried");
		check(
				!recover(REACTION_LOSS, 2000, 0, false, true, false, 0, 4, 6, false),
				"Four restored sectors cannot re-ignite");
		check(
				recover(REACTION_LOSS, 2704, 0, false, true, false, 0, 5, 5, false),
				"Re-ignition remains possible before underload");
		check(
				!recover(UNDERLOAD, 0, 0, false, true, false, 6, 6, 6, true),
				"Underload cannot be reversed by SSTB");
		check(
				!recover(STABILIZER_LOSS, 300, 0, false, true, false, 0, 6, 2, false),
				"Two stabilizers are insufficient in contingency");
		check(
				recover(STABILIZER_LOSS, 3999, 0, false, true, false, 0, 6, 3, false),
				"Three stabilizers preserve the shield");
		check(
				!recover(MANUAL_RESTABILIZATION, 500, 3, false, true, false, 0, 6, 6, false),
				"Physical objectives cannot be skipped with SSTB");
		check(
				recover(MANUAL_RESTABILIZATION, 500, 4, false, true, false, 0, 6, 2, false),
				"Final manual task needs only two stabilizers");
		check(
				recover(SHUTDOWN_FAILURE, 4049, 0, false, true, false, 0, 0, 0, true),
				"Water purge does not require intact stabilizers");
		check(
				!recover(SHUTDOWN_FAILURE, 4050, 0, false, true, false, 0, 0, 6, true),
				"Purge closes at monitoring failure");
		check(
				!recover(SHUTDOWN_FAILURE, 4000, 0, false, true, false, 0, 0, 6, false),
				"Fuel cannot substitute for water");
		for (StfrReactor.RunState state :
				List.of(
						OVERLOAD,
						REACTION_LOSS,
						STABILIZER_LOSS,
						SHUTDOWN_FAILURE,
						MANUAL_RESTABILIZATION)) {
			check(
					!recover(state, 0, 4, false, false, false, 6, 6, 6, true),
					"All recoveries require KEY");
			check(
					!recover(state, 0, 4, true, true, false, 6, 6, 6, true),
					"Repeated SSTB cannot restart an active recovery");
		}
		int manual = 3500, shutdown = 4000, contingency = 3999;
		for (int elapsed = 1; elapsed <= 597; elapsed++) {
			manual = StfrEmergencyRules.advance(MANUAL_RESTABILIZATION, manual, true, 5);
			shutdown = StfrEmergencyRules.advance(SHUTDOWN_FAILURE, shutdown, true, 0);
			contingency = StfrEmergencyRules.advance(STABILIZER_LOSS, contingency, true, 0);
			check(manual == 3500 && shutdown == 4000, "Recovery pauses both failure deadlines");
			check(
					contingency == Math.max(0, 3999 - 10 * elapsed),
					"Contingency reverses time without replaying warnings");
			check(
					(StfrEmergencyRules.manualRecoveryElapsed(elapsed) >= 600) == (elapsed == 597),
					"Manual surge starts at original U=3");
		}
		for (int secondary = 0; secondary < 16; secondary++)
			for (boolean main : new boolean[] {false, true}) {
				check(
						StfrEmergencyRules.turbineAvailable(0, main, secondary)
								== (main || secondary != 0),
						"Independent electrode availability");
				check(
						!StfrEmergencyRules.turbineAvailable(1, main, secondary),
						"Supercritical damage always disables a sector");
			}
		check(
				!StfrEmergencyRules.nominalShutdownAllowed(59.99, 20.01),
				"Weak stressed shield prevents nominal shutdown");
		check(
				!StfrEmergencyRules.nominalShutdownAllowed(60, 20.01),
				"Stress above twenty percent prevents nominal shutdown");
		check(
				!StfrEmergencyRules.nominalShutdownAllowed(59.99, 20),
				"Shield below sixty percent prevents nominal shutdown");
		check(
				!StfrEmergencyRules.nominalShutdownAllowed(100, 100),
				"High stress alone prevents nominal shutdown");
		check(
				!StfrEmergencyRules.nominalShutdownAllowed(0, 0),
				"Low shield alone prevents nominal shutdown");
		check(
				StfrEmergencyRules.nominalShutdownAllowed(60, 20),
				"Threshold values themselves permit nominal shutdown");
		check(
				!StfrEmergencyRules.shutdownFails(63, 0),
				"All six sectors and no refueling permit shutdown");
		for (int face = 0; face < 6; face++) {
			check(
					StfrEmergencyRules.shutdownFails(63 ^ (1 << face), 0),
					"Each unavailable sector prevents shutdown");
			check(
					StfrEmergencyRules.shutdownFails(63, 1 << face),
					"Each refueling sector prevents shutdown");
		}
		int chargeOne = 0, chargeSix = 0;
		for (int t = 0; t < 1029; t++) {
			chargeOne = StfrEmergencyRules.chargeSelfDestruct(chargeOne, true, true, 1, 1029);
			chargeSix = StfrEmergencyRules.chargeSelfDestruct(chargeSix, true, true, 6, 1029);
			check(chargeOne == chargeSix, "One turbine and six accumulate identical power");
		}
		check(chargeOne == 1029, "Full planned working time reaches maximum charge");
		check(
				StfrEmergencyRules.chargeSelfDestruct(500, true, true, 0, 1029) == 500,
				"Waiting retains charge without adding it");
		check(
				StfrEmergencyRules.chargeSelfDestruct(500, true, false, 6, 1029) == 500,
				"A missing core cannot accumulate charge");
		check(
				StfrEmergencyRules.chargeSelfDestruct(500, false, true, 6, 1029) == 500,
				"Protocol countdown cannot accumulate charge");
		check(
				StfrEmergencyRules.selfDestructMultiplier(1029, 1029, 3) == 3,
				"Default final multiplier is three");
		check(StfrEmergencyRules.blastRange(120, 3) == 173, "HBM range scales by volume cube root");
		check(
				StfrEmergencyRules.shutdownSpeed(300, 0) == 100
						&& StfrEmergencyRules.shutdownSpeed(500, 0) == 300,
				"Shutdown rotors accelerate gradually");
		check(
				StfrEmergencyRules.shutdownSpeed(1000, 695) == 700
						&& StfrEmergencyRules.shutdownSpeed(1000, 1100) < 700,
				"Purge overdrives then slows rotors");
		check(
				StfrEmergencyRules.shutdownSpeed(1000, 1400) == 0,
				"Rotors stop at stabilizer shutdown");
		for (int failureTick : new int[] {300, 1000, 4000}) {
			int previous = StfrEmergencyRules.shutdownSpeed(failureTick, 990);
			for (int recovery = 991; recovery < 1400; recovery++) {
				int speed = StfrEmergencyRules.shutdownSpeed(failureTick, recovery);
				check(
						speed > 0 && speed <= previous,
						"Purge rotors slow continuously until the fade ends");
				check(previous - speed <= 2, "Purge deceleration cannot jump at low speed");
				previous = speed;
			}
			check(
					StfrEmergencyRules.shutdownSpeed(failureTick, 1400) == 0,
					"Every purge stops at the remnant fade boundary");
		}
		check(
				StfrEmergencyRules.selfScreenRemaining(200) == 1896,
				"Screen starts with original abort countdown");
		check(
				StfrEmergencyRules.selfScreenRemaining(2096) == 0
						&& StfrEmergencyRules.selfScreenRemaining(2304) == 0,
				"Expired protocol remains at zero before screen switch");
		check(
				StfrEmergencyRules.selfScreenRemaining(2305) == 1405,
				"Second screen counts to original display boundary 3710");
		check(
				StfrEmergencyRules.selfScreenRemaining(3710) == 0
						&& StfrEmergencyRules.selfScreenRemaining(3740) == 0,
				"Screen zero and actual explosion remain separate");
		check(
				StfrEmergencyRules.clock(1896).equals("1:34:16")
						&& StfrEmergencyRules.clock(20).equals("0:01:00"),
				"Countdown displays minutes seconds ticks");
		check(
				!StfrEmergencyRules.selfCountdownBeep(199)
						&& StfrEmergencyRules.selfCountdownBeep(216)
						&& !StfrEmergencyRules.selfCountdownBeep(217),
				"Beep follows countdown tick remainder");
		int beats = 0;
		for (int t = 0; t < 24; t++) if (StfrEmergencyRules.selfAlarmBeat(t)) beats++;
		check(beats == 4, "Alarm repeats exactly four paired beats per 24 ticks");
		check(StfrEmergencyTimeline.canAbortSelfDestruct(2095), "Last abort tick");
		check(!StfrEmergencyTimeline.canAbortSelfDestruct(2096), "Point of no return is strict");
		check(
				StfrEmergencyTimeline.deadline(REACTION_LOSS) == 2705
						&& StfrEmergencyTimeline.deadline(UNDERLOAD) == 3640,
				"Collapse chain keeps original deadlines");
		check(
				StfrEmergencyTimeline.deadline(SHUTDOWN_FAILURE) == 5300
						&& StfrEmergencyTimeline.deadline(SELF_DESTRUCT) == 3740,
				"Destruction deadlines");
		Set<String> voiceBoundaries = new HashSet<>();
		for (StfrEmergencyTimeline.Event event : StfrEmergencyTimeline.EVENTS) {
			check(event.tick() > 0, "No event disappears at initial clock zero");
			if (!event.voice().isEmpty())
				check(
						voiceBoundaries.add(
								event.state() + ":" + event.recoveryClock() + ":" + event.tick()),
						"A clock has only one announcement voice");
			int deadline = StfrEmergencyTimeline.deadline(event.state());
			check(
					event.recoveryClock() || deadline == 0 || event.tick() < deadline,
					"Warnings precede terminal transitions");
		}
		check(
				StfrEmergencyEffects.rays(SELF_DESTRUCT, 3319) == 0
						&& StfrEmergencyEffects.rays(SELF_DESTRUCT, 3320) == 4,
				"Four burning rays start with case explosion");
		check(
				StfrEmergencyEffects.rays(SHUTDOWN_FAILURE, 4049) == 0
						&& StfrEmergencyEffects.rays(SHUTDOWN_FAILURE, 4050) == 4,
				"Shutdown rays follow monitoring loss");
		for (int i = 0; i < 4; i++) {
			Vec3d ray = StfrEmergencyEffects.rayDirection(123, 456, i);
			check(Math.abs(ray.length() - 1) < 1E-9, "Ray direction is normalized");
			check(
					ray.equals(StfrEmergencyEffects.rayDirection(123, 456, i)),
					"Server and renderer agree on the same tick");
		}
		StfrCoreSimulation core = new StfrCoreSimulation();
		NBTTagCompound spin = core.write();
		spin.setDouble("rotation", 1200);
		core.read(spin);
		core.overloadSpin();
		check(core.rotation() == 151757956, "Overload boost keeps original scoreboard units");
		core.eventBrake(60000);
		check(core.rotation() == 151697956, "Emergency brake removes 60000 units per tick");
		spin.setDouble("rotation", -1200);
		core.read(spin);
		core.overloadSpin();
		check(core.rotation() == -151757956, "Negative rotation keeps its direction");
		core.eventBrake(60000);
		check(core.rotation() == -151697956, "Brake does not reverse negative rotation");
		spin.setDouble("rotation", 0);
		core.read(spin);
		core.overloadSpin();
		check(core.rotation() == 0, "Zero rotation does not receive a fictitious boost");
		core.eventBrake(60000);
		check(core.rotation() == 0, "Emergency brake cannot cross zero");
		StfrAccessPolicy policy = new StfrAccessPolicy();
		UUID admin = UUID.randomUUID(), second = UUID.randomUUID(), card = UUID.randomUUID();
		check(policy.grant(admin, StfrAccessPolicy.ALL), "Bootstrap first administrator");
		check(
				policy.grant(card, StfrAccessPolicy.CONTROL | StfrAccessPolicy.CONNECT),
				"Grant separate bearer permissions");
		policy.setEnabled(true);
		check(
				policy.allows(card, StfrAccessPolicy.CONTROL)
						&& !policy.allows(card, StfrAccessPolicy.DETONATE),
				"Control permission does not grant detonation");
		check(
				policy.grant(card, 0) && !policy.allows(card, StfrAccessPolicy.CONTROL),
				"UUID revocation invalidates a card without retrieving it");
		check(!policy.grant(admin, 0), "Prevent losing the last administrator");
		check(
				policy.grant(second, StfrAccessPolicy.ADMIN) && policy.grant(admin, 0),
				"Administrators can be replaced and revoked");
		check(
				!policy.allows(null, StfrAccessPolicy.DETONATE),
				"Missing physical card cannot authorize a detonator");
		StfrAccessPolicy reload = new StfrAccessPolicy();
		for (Map.Entry<UUID, Integer> entry : policy.cards().entrySet())
			reload.grant(entry.getKey(), entry.getValue());
		reload.setEnabled(policy.enabled());
		check(
				reload.enabled() && reload.cards().equals(policy.cards()),
				"Card rights remain keyed solely by card UUID");
		System.out.println("Remaining state, recovery, timing and card acceptance checks passed");
	}
}

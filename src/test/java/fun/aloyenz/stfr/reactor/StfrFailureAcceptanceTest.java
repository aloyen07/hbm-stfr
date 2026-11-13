package fun.aloyenz.stfr.reactor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.logic.StfrMonitorInterference;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.structure.StfrPositionTags;

/** Headless acceptance checks without Forge initialization. */
public final class StfrFailureAcceptanceTest {

	private static void check(boolean value, String message) {
		Assertions.assertTrue(value, message);
	}

	@Test
	void acceptanceScenarios() {
		long charge = 13_000_000_000L;
		check(
				!StfrFailureTimeline.recoveryAllowed(482, 0, charge, charge, 1),
				"Recovery must wait for options");
		check(
				StfrFailureTimeline.recoveryAllowed(483, 0, charge, charge, 1),
				"Exact charge and one stabilizer suffice");
		check(
				!StfrFailureTimeline.recoveryAllowed(483, 0, charge - 1, charge, 1),
				"Partial charge is rejected");
		check(
				!StfrFailureTimeline.recoveryAllowed(483, 0, charge, charge, 0),
				"No stabilizer means no recovery");
		check(
				!StfrFailureTimeline.recoveryAllowed(483, 1, charge, charge, 6),
				"Cannot pay twice while recovering");
		check(
				!StfrFailureTimeline.recoveryAllowed(8040, 0, charge, charge, 6),
				"MELTDOWN is irreversible");
		check(
				!StfrFailureTimeline.recoveryAllowed(483, 0, 0, 0, 1),
				"Zero-charge configuration cannot bypass recovery");
		int frozen = 4000, progress = 1;
		for (int elapsed = 1; elapsed <= 600; elapsed++) {
			frozen = StfrFailureTimeline.advanceFailure(frozen, true);
			progress++;
			check(frozen == 4000, "Emergency countdown must remain paused");
			check(
					StfrFailureTimeline.recoveryComplete(progress) == (elapsed == 600),
					"Recovery is exactly 600 world ticks");
		}
		check(
				StfrFailureTimeline.advanceFailure(frozen, false) == 4001,
				"Countdown resumes after pause");
		check(!StfrFailureTimeline.meltdownDue(8039, false), "No early meltdown");
		check(StfrFailureTimeline.meltdownDue(8040, false), "Meltdown at E8040");
		check(!StfrFailureTimeline.meltdownDue(8040, true), "Recovery prevents meltdown");
		check(!StfrFailureTimeline.detonationDue(1397), "No early detonation");
		check(
				StfrFailureTimeline.detonationDue(1398),
				"Detonation at E1398, not unreachable E1416");
		int[] ticks = {1390, 1990, 3040, 3640, 4690, 5290, 6340, 7090};
		String[] voices = {"5.5m", "5m", "4.5m", "4m", "3m", "2.5m", "60s", "30s"};
		for (int i = 0; i < ticks.length; i++) {
			check(
					StfrFailureTimeline.countdown(ticks[i]).equals(voices[i]),
					"Original countdown voice boundary");
			check(
					StfrFailureTimeline.countdown(ticks[i] + 1).isEmpty(),
					"Voice must not repeat next tick");
		}
		check(
				StfrFailurePolicy.allowsHbm(0, true) && StfrFailurePolicy.allowsHbm(0, false),
				"Unprotected profiles enabled");
		check(
				!StfrFailurePolicy.allowsHbm(StfrFailurePolicy.ALL, true),
				"Default suppressor blocks nuclear blast");
		check(
				!StfrFailurePolicy.allowsHbm(StfrFailurePolicy.ALL, false),
				"Default suppressor blocks high-energy blast");
		check(
				StfrFailurePolicy.allowsHbm(StfrFailurePolicy.NUCLEAR, false),
				"Nuclear category does not disable FLEIJA");
		check(
				StfrFailurePolicy.allowsHbm(StfrFailurePolicy.HIGH_ENERGY, true),
				"FLEIJA category does not disable nuclear");
		for (int coupled :
				new int[] {
					StfrFailurePolicy.STRUCTURE,
					StfrFailurePolicy.FIRE,
					StfrFailurePolicy.PLAYER_DAMAGE
				}) {
			check(
					!StfrFailurePolicy.allowsHbm(coupled, true),
					"No nuclear bypass of physical protection");
			check(
					!StfrFailurePolicy.allowsHbm(coupled, false),
					"No FLEIJA bypass of physical protection");
		}
		int combined = StfrFailurePolicy.NUCLEAR | StfrFailurePolicy.HIGH_ENERGY;
		check(
				!StfrFailurePolicy.allowsHbm(combined, true)
						&& !StfrFailurePolicy.allowsHbm(combined, false),
				"Multiple suppressors combine");
		int glitches = 0;
		for (int tick = 0; tick < 10000; tick++) {
			boolean glitch = StfrMonitorInterference.active(1234, tick, true);
			if (glitch) {
				glitches++;
				check(
						!StfrMonitorInterference.active(1234, tick + 1, true),
						"Interference lasts one tick");
			}
			check(
					!StfrMonitorInterference.active(1234, tick, false),
					"Stable monitors have no interference");
			check(
					glitch == StfrMonitorInterference.active(1234, tick, true),
					"Every frame of the tick agrees");
		}
		check(
				glitches > 250 && glitches < 600,
				"Interference is occasional, not permanently flashing");
		Set<Long> marked = new HashSet<>(Arrays.asList(0L, 1234L, -9876L, Long.MAX_VALUE));
		check(
				StfrPositionTags.read(StfrPositionTags.write(marked)).equals(marked),
				"Case Breach positions survive a 1.12-compatible NBT round trip");
		check(
				StfrPositionTags.read(StfrPositionTags.write(Collections.emptySet())).isEmpty(),
				"Empty Case Breach annotation does not introduce arbitrary destructive targets");
		StfrComponentStatus.Builder components = new StfrComponentStatus.Builder(EnumFacing.EAST);
		components.mainElectrodeEnabled = false;
		check(
				new StfrComponentStatus(components).secondaryElectrodeMask == 15,
				"Main electrode failure leaves all side electrodes enabled");
		components.mainElectrodeEnabled = true;
		components.secondaryElectrodeMask = 14;
		check(
				new StfrComponentStatus(components).mainElectrodeEnabled,
				"Side electrode failure leaves main electrode enabled");
		components.criticalFailures = 1;
		components.supercriticalFailures = 1;
		check(
				!new StfrComponentStatus(components).mainElectrodeEnabled
						&& new StfrComponentStatus(components).secondaryElectrodeMask == 0,
				"Supercritical damage disables the whole sector");
		components.supercriticalFailures = 0;
		check(
				!new StfrComponentStatus(components).mainElectrodeEnabled
						&& new StfrComponentStatus(components).secondaryElectrodeMask == 14
						&& new StfrComponentStatus(components).turbineStructureValid,
				"Critical damage disables main control but preserves independent secondaries");
		components.secondaryElectrodeMask = 0;
		check(
				!new StfrComponentStatus(components).turbineStructureValid,
				"Sector fails when main and all secondary electrodes fail");
		check(
				StfrShieldControl.advance(50, 0, 6, 29D / 30D)
						< StfrShieldControl.advance(50, 0, 6, 1),
				"One lost electrode reduces shield regeneration");
		check(
				StfrShieldControl.advance(50, 100, 6, 29D / 30D)
						< StfrShieldControl.advance(50, 100, 6, 1),
				"One lost electrode increases shield loss under stress");
		check(
				StfrShieldControl.advance(50, 0, 0, 0) < 50,
				"No electrodes means shield loss without regeneration");
		int previous = 0;
		Set<Integer> lightLevels = new HashSet<>();
		for (int tick = 415; tick <= 585; tick++) {
			int level = StfrChamberLight.level(StfrReactor.RunState.STARTUP_CONFIRMED, tick);
			check(
					level >= previous,
					"Chamber lighting increases monotonically as the shield forms");
			lightLevels.add(level);
			previous = level;
		}
		for (int level = 1; level <= 15; level++)
			check(lightLevels.contains(level), "Chamber lighting visits every light level");
		check(
				StfrChamberLight.level(StfrReactor.RunState.ONLINE, 0) == 15,
				"Online chamber lighting is fifteen");
		check(
				StfrChamberLight.level(StfrReactor.RunState.OFFLINE, 0) == 0,
				"Offline chamber emits no light");
		check(
				StfrChamberLight.level(StfrReactor.RunState.DETONATION, 0) == 0,
				"Detonation removes chamber lighting");
		check(
				StfrChamberLight.level(StfrReactor.RunState.SHUTDOWN, 470) == 0,
				"Lighting disappears with the shield on shutdown");
		BlockPos electrodePos = new BlockPos(17, 14, 16), reactorCenter = new BlockPos(14, 14, 14);
		for (EnumFacing facing : EnumFacing.VALUES) {
			Vec3d mount = StfrElectrodes.mount(electrodePos, facing);
			Vec3d tip = StfrElectrodes.tip(mount, reactorCenter);
			check(
					Math.abs(mount.distanceTo(new Vec3d(17.5, 14.5, 16.5)) - .5) < 1E-9,
					"Pivot remains on the mounting face for every orientation");
			check(
					Math.abs(tip.distanceTo(mount) - 1D) < 1E-9,
					"Beam starts at the end of the one-block electrode");
			Vec3d toCore = new Vec3d(14.5, 14.5, 14.5).subtract(mount);
			check(
					tip.subtract(mount).crossProduct(toCore).lengthSquared() < 1E-18,
					"Electrode tip and beam point exactly at core center");
		}
		System.out.println("STFR failure acceptance checks passed");
	}
}

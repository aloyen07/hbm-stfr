package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** Operator control, suspension and resumption of the self-destruct protocol. */
final class StfrSelfDestructControl {
	private StfrSelfDestructControl() {}

	static boolean selfFormingOffline(StfrEmergencyController c) {
		return c.selfWasOffline;
	}

	static boolean selfPending(StfrEmergencyController c) {
		return c.selfPending;
	}

	static float selfChargeFraction(StfrEmergencyController c) { return Math.min(1F,c.selfCharge/(float)Math.max(1,StfrConfig.selfDestructChargeTicks)); 	}

	static boolean selfBlast(StfrEmergencyController c) {
		return c.selfBlast;
	}

	static double selfMultiplier(StfrEmergencyController c) {
		return c.selfBlast
				? StfrEmergencyRules.selfDestructMultiplier(
						c.selfCharge,
						StfrConfig.selfDestructChargeTicks,
						StfrConfig.selfDestructMaxMultiplier)
				: 1;
	}

	static double selfProjectedMultiplier(StfrEmergencyController c) {
		return StfrEmergencyRules.selfDestructMultiplier(
				c.selfCharge,
				StfrConfig.selfDestructChargeTicks,
				StfrConfig.selfDestructMaxMultiplier);
	}

	static boolean canAbortSelfDestruct(StfrEmergencyController c) {
		return (c.selfPending || c.r.runState() == SELF_DESTRUCT)
				&& !c.selfCommitted
				&& (c.selfPending || StfrEmergencyTimeline.canAbortSelfDestruct(c.r.stateTicks()));
	}

	static void simulationClock(StfrEmergencyController c, World world, int tick) {
		if (tick <= 0) return;
		c.r.emergencyTick(tick, 100);
		if (c.r.runState() == SELF_DESTRUCT) {
			if (c.selfWasOffline && tick >= 2485) {
				c.r.core().simulationPreset();
				c.selfWasOffline = false;
			}
			c.selfCommitted = c.r.core().exists() && tick >= 2096;
			c.selfFinale = tick >= 3320;
			c.selfCharge = Math.min(StfrConfig.selfDestructChargeTicks, Math.max(0, tick - 2485));
		}
	}

	static boolean selfDestruct(StfrEmergencyController c, World w) {
		if (c.r.failure().detonated() || c.selfPending || c.r.runState() == SELF_DESTRUCT) return false;
		c.selfPreviousState = c.r.runState();
		c.selfPreviousTicks = c.r.stateTicks();
		c.previousEmergencySnapshot = c.write();
		c.previousEmergencySnapshot.removeTag("previousEmergencySnapshot");
		c.selfPending = true;
		c.selfCharge = 0;
		c.selfCommitted = c.selfFinale = c.selfBlast = false;
		c.r.refresh(w);
		if (c.r.workingStabilizers() == 0) {
			c.notice(w, "self_waiting");
			return true;
		}
		c.selfPending = false;
		c.start(w, SELF_DESTRUCT);
		return true;
	}

	static void pollSelfDestruct(StfrEmergencyController c, World w) {
		if (c.r.failure().detonated()) {
			c.selfPending = false;
			return;
		}
		if (c.selfPending && c.r.workingStabilizers() > 0) {
			c.selfPending = false;
			c.start(w, SELF_DESTRUCT);
			return;
		}
		if (c.r.runState() != SELF_DESTRUCT || c.selfFinale || c.r.workingStabilizers() > 0) return;
		boolean existing = c.r.core().exists();
		c.selfCommitted = existing && (c.selfCommitted || c.r.stateTicks() >= 2096);
		c.stopMusic(w);
		// Keep the actual plasma; it resumes ordinary uncontained physics and can fail
		// independently.
		c.r.emergencyEnter(w, existing ? ONLINE : OFFLINE);
		c.selfPending = true;
		c.notice(w, "self_waiting");
	}

	static boolean abortSelfDestruct(StfrEmergencyController c, World w) {
		if (!c.canAbortSelfDestruct()) return false;
		c.stopMusic(w);
		c.broadcast(
				w,
				"",
				"The Fusion Reactor Self-Destruct Protocol Has Been Aborted By Facility"
					+ " Administration.",
				2);
		if (c.r.runState() == SELF_DESTRUCT) {
			StfrReactor.RunState previous = c.selfPreviousState;
			int ticks = c.selfPreviousTicks;
			NBTTagCompound backup = c.previousEmergencySnapshot;
			c.read(backup);
			c.r.emergencyEnter(w, previous);
			c.r.emergencyTick(ticks, 0);
		}
		c.selfPending = c.selfCommitted = c.selfFinale = c.selfBlast = false;
		c.selfCharge = 0;
		return true;
	}
}

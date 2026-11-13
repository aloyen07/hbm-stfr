package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrShutdownWater;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** Recovery preconditions and operator confirmation for emergency scenes. */
final class StfrEmergencyRecovery {
	private StfrEmergencyRecovery() {}

	static boolean canConfirm(StfrEmergencyController c, World w) {
		return StfrEmergencyRules.recoveryAllowed(
				c.r.runState(),
				c.r.stateTicks(),
				c.stage,
				c.recovering,
				c.r.keyOn(),
				c.manualFailed,
				c.lensesInMode(w, true),
				c.lensesInMode(w, false),
				c.r.workingStabilizers(),
				c.r.runState() == SHUTDOWN_FAILURE && c.waterCapsules(w, true));
	}

	static String confirmationProblem(StfrEmergencyController c, World w) {
		if (c.canConfirm(w)) return "ready";
		return !c.r.keyOn()
				? "key"
				: c.recovering
						? "active"
						: c.r.runState() == SHUTDOWN_FAILURE
								? (c.r.stateTicks() >= 4050 ? "expired" : c.waterProblem(w))
								: c.r.runState() == REACTION_LOSS
										? "emitters"
										: c.r.runState() == OVERLOAD ? "auxiliary" : "stabilizers";
	}

	static void explainConfirmation(StfrEmergencyController c, World w) {
		if (c.canConfirm(w)) return;
		for (EntityPlayer player : w.playerEntities)
			if (player.getDistanceSq(c.r.center) <= 128D * 128D)
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.recovery." + c.confirmationProblem(w)));
	}

	static boolean confirm(StfrEmergencyController c, World w) {
		c.r.refresh(w);
		if (!c.canConfirm(w)) return false;
		switch (c.r.runState()) {
			case OVERLOAD:
				c.start(w, MANUAL_RESTABILIZATION);
				break;
			case REACTION_LOSS:
				c.start(w, REIGNITION);
				break;
			case STABILIZER_LOSS:
				c.recovery = 0;
				c.recovering = true;
				c.stopMusic(w);
				c.sound(w, "music.hacknet_music_end", 10, 1);
				c.repeated(w, "charge_up.heavy_shutdown", 5, 3, .8F);
				break;
			case MANUAL_RESTABILIZATION:
				c.stage = 5;
				c.recovery = 0;
				c.recovering = true;
				c.broadcast(
						w,
						"stfr.manual_restab.confirm",
						"Fusion Reactor Restabilization Confirmed. Commencing A Reactor Power Surge"
							+ " For Auxiliary Field Compression...",
						1);
				c.music(w, "music.restabilization_music", 15, 1);
				break;
			case SHUTDOWN_FAILURE:
				if (!c.waterCapsules(w, false)) return false;
				c.recovery = 0;
				c.recovering = true;
				c.broadcast(
						w,
						"stfr.shut_fail.restab_start",
						"Shutdown Failure Restabilization Sequence Initialized!",
						1);
				c.music(w, "music.joel_nielsen_blast_pit_3", 10, 1);
				for (float pitch : new float[] {.6F, .64F, 0}) c.sound(w, "alarms.beep2", 1, pitch);
				StfrAdvancements.grant(w, c.r.center, "shutdown_failure_restabilization");
				break;
			default:
				return false;
		}
		w.playSound(null, c.r.center, STFRSounds.ALARM_BEEP_2, SoundCategory.BLOCKS, 4, 1);
		return true;
	}

	static boolean waterCapsules(StfrEmergencyController c, World w, boolean check) {
		if (!c.waterProblem(w).equals("ready")) return false;
		List<TileEntityFuelSlot> slots = c.fuelSlots(w);
		if (!check) {
			int consumed = 0;
			for (TileEntityFuelSlot slot : slots)
				if (slot.getPhase() == TileEntityFuelSlot.READY
						&& slot.isLocked()
						&& StfrReactor.isWaterCapsule(ItemFuelCapsule.getFluid(slot.getCapsule()))
						&& StfrShutdownWater.enough(
								ItemFuelCapsule.getFluid(slot.getCapsule()).amount)) {
					slot.drainFuel(StfrShutdownWater.PER_CAPSULE);
					if (++consumed == StfrShutdownWater.CAPSULES) break;
				}
		}
		return true;
	}
}

package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.logic.StfrCoreFlashPacket;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructLight;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructMechanics;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrSelfDestructState {

	private StfrSelfDestructState() {}

	static void enter(StfrEmergencyController c, World w) {
		if (c.selfWasOffline) c.r.core().eventTemperature(16000);
		c.music(w, "music.the_final_flash_of_existance_remix", 16, 1);
		for (float pitch : new float[] {.6F, .64F, 0}) c.sound(w, "alarms.beep2", 1, pitch);
		c.r.eventSound(w, STFRSounds.ALARM_2, SoundCategory.AMBIENT, 4, 1);
		c.r.failure().crashMainframes(w, 200);
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		if(StfrSelfDestructMechanics.closeHatches(tick,c.selfWasOffline))
			c.r.closeSelfDestructHatches(w);
		if (tick == 200 || tick == 2305)
			for (int i = 0; i < 10; i++)
				c.sound(w, "lightsswitch", SoundCategory.MASTER, 5, tick == 200 ? 2 : 1.8F);
		if (StfrEmergencyRules.selfCountdownBeep(tick))
			c.sound(w, "alarms.beep", SoundCategory.MASTER, 1, 2);
		if (tick >= 200 && StfrEmergencyRules.selfAlarmBeat(c.r.uptimeTicks())) {
			c.r.eventSound(w, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.RECORDS, 1, 1.8F);
			c.r.eventSound(w, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.RECORDS, 1, 1.81F);
		}
		if (tick >= 2096 && c.r.core().exists()) c.selfCommitted = true;
		if (tick == 2485 && c.selfWasOffline) {
			c.r.core().simulateOnline();
			c.selfWasOffline = false;
			c.burst(w);
			StfrCoreFlashPacket.selfDestruct(w,c.r.center,1.2F,100,0);
		}
		c.selfCharge =
				StfrEmergencyRules.chargeSelfDestruct(
						c.selfCharge,
						tick >= 2405,
						c.r.core().exists(),
						c.r.workingStabilizers(),
						StfrConfig.selfDestructChargeTicks);
		if (tick == 3320) c.selfFinale = true;
		if (tick >= 2910) {
			c.r.core().eventHeat(332.634, 10.32634);
			c.fire(w);
		}
		if (tick == 2910) {
			c.repeated(w, "charge_up.motor_rampup", 2, 8, .8F);
			c.r.failure().shake(w, 80, 100, 3, 5);
		}
		if (tick == 2940) c.repeated(w, "powerpurge", 4, 10, 0);

		if (tick >= 3320) {
			c.plasma(w, tick);
			c.r.core().eventHeat(1032.634, 0);
		}
		if (tick == 3320) {
			c.burst(w);
			c.r.disassembleSelfDestruct(w, false);
		}
		if (tick == 3360) c.r.disassembleSelfDestruct(w, true);
		if (tick >= 3360 && tick < 3690) c.rotorParticles(w, EnumParticleTypes.FIREWORKS_SPARK, 25);
		for (EnumFacing face : EnumFacing.VALUES)
			if (tick == StfrSelfDestructLight.rotorTick(face.ordinal())) {
				c.r.ejectSelfDestructRotor(w, face);
				c.rotorExplosion(w, face);
				StfrCoreFlashPacket.selfDestruct(w,c.r.center.offset(face,9),1F,100,16);
				StfrCoreFlashPacket.selfDestruct(w,c.r.center,.35F,75,0);
			}
		if (tick == 3525) c.repeated(w, "charge_up.heavy_startup", 20, 10, 2);
		if (tick == 3730) c.repeated(w, "explosion.mega_explosion", 10, 10, 0);
		StfrSelfDestructLight.Pulse pulse=StfrSelfDestructLight.pulse(tick);
		if(pulse!=null)StfrCoreFlashPacket.selfDestruct(w,c.r.center,pulse.intensity,pulse.milliseconds,0);
		if (tick >= 3740) {
			c.selfBlast = true;
			c.detonate(w);
		}
	}
}

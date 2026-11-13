package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrStabilizerLossState {

	private StfrStabilizerLossState() {}

	static void enter(StfrEmergencyController c, World w) {
		c.music(w, "music.hacknet_labyrinths_ogre_snidelywhiplash", 10, 1);
		c.broadcast(
				w, "", "Complete Stabilization Loss Detected! Preparing Contingency Protocols.", 5);
		StfrAdvancements.grant(w, c.r.center, "stabilizer_loss");
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		if (tick == 280 && !c.recovering) {
			c.sound(w, "charge_up.motor_rampup", 10, 0);
			c.r.failure().shake(w, 120, 4000 - 280, 140, 5);
		}
		if (tick >= 280 && !c.recovering) {
			c.rotorParticles(w, EnumParticleTypes.CLOUD, 10);
			if (tick % 100 == 0) c.sound(w, "dem.purge", 5, 2);
			c.plasma(w, tick);
		}
		if (c.recovery >= 420) {
			c.broadcast(w, "", "Reactor Shield Has Been Successfully Preserved.", 1);
			c.r.failure().fadeShake(w, 120, 200);
			c.finish(w, "restabilization");
		} else if (tick >= 4000) {
			c.stopMusic(w);
			c.r.emergencyEnter(w, MELTDOWN);
		}
	}
}

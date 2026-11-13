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

final class StfrShutdownFailureState {

	private StfrShutdownFailureState() {}

	static void enter(StfrEmergencyController c, World w) {
		c.r.core().ignite();
		c.music(w, "music.carpenter_brut_run_sally_run", 10, 1);
		c.broadcast(
				w,
				"stfr.shut_fail.start",
				"Cannot Continue Fusion Reactor Shutdown Sequence: Stabilizer Shutdown Failure!",
				1);
		StfrAdvancements.grant(w, c.r.center, "shutdown_failure");
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		if (!c.recovering && (tick >= 300 && tick <= 350 || tick >= 2550 && tick <= 2650))
			c.rotorParticles(w, EnumParticleTypes.CLOUD, 10);
		if (tick == 1550 && !c.recovering) c.implosion(w);
		if (c.recovering) c.shutdownRecovery(w);
		else {
			if (tick >= 2250) c.r.core().eventHeat(Math.max(0, tick - 2200) * 2D, 0);
			if (tick >= 4050) {
				c.plasma(w, tick);
				c.rotorParticles(w, EnumParticleTypes.SMOKE_LARGE, 10);
			}
			if (tick == 4050 || tick == 4650 || tick == 4950) c.burst(w);
			if (tick == 4950) c.r.failure().crashMainframes(w, 1300);
			if (tick >= 4950 && tick < 5300) c.r.failure().shutdownDebris(w, tick);
			if (tick >= 5300) c.detonate(w);
		}
	}
}

package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.reactor.physics.StfrOverloadPulse;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrOverloadState {

	private StfrOverloadState() {}

	static void enter(StfrEmergencyController c, World w) {
		c.r.core().eventShield(0);
		c.r.core().overloadSpin();
		c.music(
				w,
				w.rand.nextInt(100) == 0
						? "music.hacknet_ost_paki_trak"
						: "music.hacknet_ost_panic_track",
				16,
				1);
		c.broadcast(
				w,
				"stfr.overload.start",
				"Failure Of Primary Outer Core Shield Detected! REACTOR CORE IS NOW IN AN OVERLOAD"
					+ " STATE!",
				3);
		c.burst(w);

		c.repeated(w, "charge_up.heavy_startup", 10, 5, 1.5F);
		StfrAdvancements.grant(w, c.r.center, "overload");
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		if(!w.isRemote && StfrOverloadPulse.triggers(c.r.runState(),c.recovering,w.rand.nextDouble()))
			c.r.failure().overloadPulse(w);
		c.r.core().eventHeat(216.375, 3.46743);
		c.r.core().eventShield(0);
		c.r.core().eventBrake(60000);
		c.particles(w, c.r.center, EnumParticleTypes.FLAME, 100, .4, .06);
		c.particles(w, c.r.center, EnumParticleTypes.SMOKE_LARGE, 10, .5, .01);
		if (tick <= 250 || tick >= 300)
			c.rotorParticles(
					w, tick >= 300 ? EnumParticleTypes.FLAME : EnumParticleTypes.CLOUD, 10);
		if (tick >= 1200) c.plasma(w, tick);
		if (c.r.core().mssr() <= 0) {
			if (!c.internalWarning) {
				c.broadcast(
						w,
						"stfr.overload.int_shield_crit",
						"Primary Internal Shield (Beta) Intensity Compromised! Total Core"
							+ " Containment Failure Imminent.",
						4);
				c.internalWarning = true;
			}
			if (w.rand.nextDouble() < .003) {
				c.stopMusic(w);
				c.r.emergencyEnter(w, MELTDOWN);
			}
		}
	}
}

package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrReignitionState {

	private StfrReignitionState() {}

	static void enter(StfrEmergencyController c, World w) {
		for (Map.Entry<Long, IBlockState> lens : c.lenses.entrySet())
			if (w.getBlockState(c.worldPos(lens.getKey())).getBlock() == STFRBlocks.STFR_FROZEN_PLASMA_LENS)
				w.setBlockState(c.worldPos(lens.getKey()), lens.getValue(), 3);
		c.stopMusic(w);
		c.sound(w, "music.hacknet_music_end", 8, 1);
		c.broadcast(
				w,
				"stfr.reaction_loss.restab_start",
				"Fusion Reactor Re-Ignition Protocol Initiated!",
				1);
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		if (tick <= 380) c.cold(w, tick);
		if (tick == 80) c.surge(w);
		if (tick == 380) {
			c.r.failure().caseBlowout(w);
			c.burst(w);
			c.repeated(w, "explosion.rocketfly", 10, 10, 1.2F);
			c.r.core().restabilize();
		}
		if (tick >= 1000) {
			c.broadcast(
					w,
					"stfr.reaction_loss.restab_complete",
					"Fusion Reactor Re-Ignition Protocol Completed!",
					1);
			c.finish(w, "restabilization");
		}
	}
}

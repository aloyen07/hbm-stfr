package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrReactionLossState {

	private StfrReactionLossState() {}

	static void enter(StfrEmergencyController c, World w) {
		c.emitterMask = 0;
		c.emitterInstructionSent=false;
		c.r.mainframeSound(w,SoundEvents.BLOCK_NOTE_BELL,SoundCategory.MASTER,5,0);
		c.music(w, "music.hacknet_ost_malware_injection_remix", 16, 1);
		c.broadcast(
				w, "stfr.reaction_loss.collapse", "FUSION REACTOR SHIELD COLLAPSE DETECTED!", 3);
		StfrReactionLossEffects.transition(c,w);
		if (c.r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE))
			for (long key : c.lenses.keySet())
				w.setBlockState(c.worldPos(key), STFRBlocks.STFR_FROZEN_PLASMA_LENS.getDefaultState(), 3);
		StfrAdvancements.grant(w, c.r.center, "reaction_loss");
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		c.announceEmitters(w);
		if (tick == 453) c.announceExpectedEmitters(w);
		c.cold(w, tick);
		c.r.core().eventShield(12);
		StfrReactionLossEffects.tick(c,w,tick,false);
		if (tick >= 2705) c.start(w, UNDERLOAD);
	}
}

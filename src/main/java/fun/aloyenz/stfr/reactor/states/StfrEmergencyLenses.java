package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** Emitter mapping, recovery progress and lens replacement checks. */
final class StfrEmergencyLenses {
	private StfrEmergencyLenses() {}

	static List<StfrStructureTemplate.Cell> emitterCells(StfrEmergencyController c, World w) {
		TileEntityStructureAnalyzer analyzer = null;
		for (TileEntity tile : w.loadedTileEntityList)
			if (tile instanceof TileEntityStructureAnalyzer) {
				TileEntityStructureAnalyzer candidate = (TileEntityStructureAnalyzer) tile;
				if (candidate.getOrigin().add(StfrStructureTemplate.CENTER).equals(c.r.center)) {
					analyzer = candidate;
					break;
				}
			}
		List<StfrStructureTemplate.Cell> cells = new ArrayList<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells())
			if ((analyzer == null ? cell.defaultRole : analyzer.getRole(cell.localPos))
					== StfrBlockRole.EMITTER_BLOCK) cells.add(cell);
		return cells;
	}

	static BlockPos worldPos(StfrEmergencyController c, long local) {
		return c.r.center.subtract(StfrStructureTemplate.CENTER).add(BlockPos.fromLong(local));
	}

	static void rememberLenses(StfrEmergencyController c, World w) {
		c.lenses.clear();
		for (StfrStructureTemplate.Cell cell : c.emitterCells(w)) {
			IBlockState state = w.getBlockState(c.worldPos(cell.localPos.toLong()));
			if (state.getBlock() != Blocks.AIR
					&& state.getBlock() != STFRBlocks.STFR_FROZEN_PLASMA_LENS
					&& state.getBlock() != STFRBlocks.STFR_AUXILIARY_QUARTZ_LENS)
				c.lenses.put(cell.localPos.toLong(), state);
			else c.lenses.put(cell.localPos.toLong(), cell.expected);
		}
	}

	static boolean auxiliaryLens(StfrEmergencyController c, BlockPos local, IBlockState state) {
		return c.lenses.containsKey(local.toLong())
				&& state.getBlock() == STFRBlocks.STFR_AUXILIARY_QUARTZ_LENS
				&& (c.r.runState() == OVERLOAD || c.r.runState() == MANUAL_RESTABILIZATION);
	}

	static int lensesInMode(StfrEmergencyController c, World w, boolean auxiliary) {
		int mask = 0;
		for (Map.Entry<Long, IBlockState> lens : c.lenses.entrySet()) {
			BlockPos local = BlockPos.fromLong(lens.getKey());
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(local);
			if (direction == null) continue;
			IBlockState actual = w.getBlockState(c.worldPos(lens.getKey()));
			boolean matches =
					auxiliary
							? actual.getBlock() == STFRBlocks.STFR_AUXILIARY_QUARTZ_LENS
							: actual.getBlock() == lens.getValue().getBlock();
			if (matches) mask |= 1 << direction.ordinal();
		}
		return Integer.bitCount(mask);
	}

	static void announceEmitters(StfrEmergencyController c, World w) {
		int mask = 0;
		for (Map.Entry<Long, IBlockState> lens : c.lenses.entrySet())
			if (w.getBlockState(c.worldPos(lens.getKey())).getBlock() == lens.getValue().getBlock()) {
				EnumFacing direction =
						StfrRadialSymmetry.sourceDirection(BlockPos.fromLong(lens.getKey()));
				if (direction != null) mask |= 1 << direction.ordinal();
			}
		if (c.r.announcementMessages() && (mask & ~c.emitterMask) != 0)
			for (EntityPlayer player : w.playerEntities)
				if (player.getDistanceSq(c.r.center) <= 128D * 128D)
					player.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.recovery.emitter_progress",
									Integer.bitCount(mask)));
		if(!c.emitterInstructionSent && Integer.bitCount(mask)>=5) {
			c.emitterInstructionSent=true;
			c.broadcast(w,"","Set KEY to ON and press SSTB to initiate REIGNITION.",0,"message.hbmstfr.scene.reaction_loss.455");
		}
		c.emitterMask = mask;
	}

	static void announceExpectedEmitters(StfrEmergencyController c, World w) {
		if (!c.r.announcementMessages()) return;
		ITextComponent names = new TextComponentString("");
		Set<Block> seen = new HashSet<>();
		for (IBlockState state : c.lenses.values())
			if (seen.add(state.getBlock())) {
				if (seen.size() > 1) names.appendText(", ");
				names.appendSibling(
						new TextComponentTranslation(
								state.getBlock().getTranslationKey() + ".name"));
			}
		for (EntityPlayer player : w.playerEntities)
			if (player.getDistanceSq(c.r.center) <= 128D * 128D)
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.scene.emitters_expected", names));
	}
}

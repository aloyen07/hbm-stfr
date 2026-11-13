package fun.aloyenz.stfr.tile;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer.RotorMark;

/** Rotor annotations, proxy replacement and render synchronization. */
final class StfrAnalyzerRotors {
	private StfrAnalyzerRotors() {}

	static void updateRotorSpin(TileEntityStructureAnalyzer c, double angle, int velocity, int runState, int stateTicks) {
		if (c.getWorld() == null) return;
		c.rotorAngle = angle;
		c.rotorVelocity = velocity;
		c.visualRunState = runState;
		c.visualStateTicks = stateTicks;
		c.rotorSyncTick = c.getWorld().getTotalWorldTime();
	}

	static void markRotor(TileEntityStructureAnalyzer c, BlockPos local, EnumFacing face, RotorMark mark) {
		if (!c.containsLocalPos(local) || !c.isTemplateCell(local)) return;
		long key = local.toLong();
		switch (mark) {
			case ROTOR:
				c.rotors.add(key);
				break;
			case AXIS:
				c.axes.put(key, face);
				break;
			case PARTICLE:
				c.particleFaces.put(key, c.particleFaces.getOrDefault(key, 0) | 1 << face.getIndex());
				break;
			case UNSIGNED:
				c.rotors.remove(key);
				c.axes.remove(key);
				c.particleFaces.remove(key);
				break;
			default:
				break;
		}
		c.sync();
	}

	static int saveRotorMap(TileEntityStructureAnalyzer c) {
		if (c.getWorld() == null || c.getWorld().isRemote) return 0;
		StfrManager manager = StfrManager.get(c.getWorld());
		StfrReactor reactor = manager.getOrCreate(c.getOrigin().add(StfrStructureTemplate.CENTER));
		int changed = reactor.setRotorMap(c.getWorld(), c.rotors, c.axes, c.particleFaces, c.hiddenRotorStates);
		manager.markDirty();
		c.hiddenRotorStates.clear();
		c.sync();
		return changed;
	}

	static int syncRotorBlocks(TileEntityStructureAnalyzer c, boolean spinning) {
		if (c.getWorld() == null || c.getWorld().isRemote) return 0;
		BlockPos origin = c.getOrigin();
		if (!c.getWorld().isAreaLoaded(origin, origin.add(28, 28, 28))) return 0;
		int changed = 0;
		for (Map.Entry<Long, IBlockState> entry : new HashMap<>(c.hiddenRotorStates).entrySet()) {
			if (spinning && c.rotors.contains(entry.getKey())) continue;
			BlockPos target = c.getOrigin().add(BlockPos.fromLong(entry.getKey()));
			if (c.getWorld().getBlockState(target).getBlock() == STFRBlocks.ROTOR_PLACEHOLDER) {
				c.getWorld().setBlockState(target, entry.getValue(), 3);
				changed++;
			}
			c.hiddenRotorStates.remove(entry.getKey());
		}
		if (!spinning) {
			if (changed > 0) c.sync();
			return changed;
		}
		Map<Long, IBlockState> additions = new HashMap<>();
		for (Long key : c.rotors) {
			if (c.hiddenRotorStates.containsKey(key)) continue;
			BlockPos local = BlockPos.fromLong(key);
			BlockPos target = c.getOrigin().add(local);
			IBlockState state = c.getWorld().getBlockState(target);
			if (state.getBlock() == Blocks.AIR
					|| state.getBlock() == STFRBlocks.ROTOR_PLACEHOLDER
					|| state.getBlock().hasTileEntity(state)) continue;
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			if (cell == null
					|| !StfrStructureValidator.matchesCell(
							cell, c.getRole(local), state, c.getWorld(), target, c)) continue;
			IBlockState rendered =
					state.getBlock() instanceof BlockStairs
							? state.getActualState(c.getWorld(), target)
							: state;
			additions.put(key, rendered);
		}
		// Capture stair corners before any neighbor becomes a placeholder.
		for (Map.Entry<Long, IBlockState> entry : additions.entrySet()) {
			Long key = entry.getKey();
			BlockPos target = c.getOrigin().add(BlockPos.fromLong(key));
			c.hiddenRotorStates.put(key, entry.getValue());
			if (c.getWorld().setBlockState(target, STFRBlocks.ROTOR_PLACEHOLDER.getDefaultState(), 3))
				changed++;
			else c.hiddenRotorStates.remove(key);
		}
		if (changed > 0) c.sync();
		return changed;
	}

	static void restoreRotorBlocks(TileEntityStructureAnalyzer c) {
		if (c.getWorld() == null || c.getWorld().isRemote) return;
		for (Map.Entry<Long, IBlockState> entry : c.hiddenRotorStates.entrySet()) {
			BlockPos target = c.getOrigin().add(BlockPos.fromLong(entry.getKey()));
			if (c.getWorld().getBlockState(target).getBlock() == STFRBlocks.ROTOR_PLACEHOLDER)
				c.getWorld().setBlockState(target, entry.getValue(), 3);
		}
		c.hiddenRotorStates.clear();
	}
}

package fun.aloyenz.stfr.reactor;

import java.util.Collections;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** One successful pass per saved reactor, before diagnostics or simulation run. */
public final class StfrEmitterMigration {
	static final int VERSION = 1;
	private StfrEmitterMigration() {}

	public static IBlockState replacement(IBlockState state) {
		if (state.getBlock() == Blocks.GLOWSTONE) return registeredState("stfr_plasma_lens");
		if (state.getBlock() == Blocks.QUARTZ_BLOCK) return registeredState("stfr_auxiliary_quartz_lens");
		if (state.getBlock() == Blocks.PACKED_ICE) return registeredState("stfr_frozen_plasma_lens");
		return state;
	}

	private static IBlockState registeredState(String name) {
		Block block = Block.REGISTRY.getObject(new ResourceLocation("hbmstfr", name));
		if (block == null || block == Blocks.AIR)
			throw new IllegalStateException("Missing STFR lens block: " + name);
		return block.getDefaultState();
	}

	static boolean migrate(StfrReactor reactor, World world) {
		if (reactor.emitterMigrationVersion >= VERSION) return true;
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		if (!world.isAreaLoaded(origin, origin.add(28, 28, 28))) return false;
		TileEntityStructureAnalyzer analyzer = StfrStructureValidator.matchingAnalyzer(world, reactor.center);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			// Include both shipped emitter positions and author-edited emitter positions.
			if (cell.defaultRole != StfrBlockRole.EMITTER_BLOCK
					&& (analyzer == null || analyzer.getRole(cell.localPos) != StfrBlockRole.EMITTER_BLOCK)) continue;
			BlockPos pos = origin.add(cell.localPos);
			IBlockState old = world.getBlockState(pos), next = replacement(old);
			if (old != next && !world.setBlockState(pos, next, 3)) return false;
		}
		reactor.emergency().migrateLegacyLenses();
		reactor.emitterMigrationVersion = VERSION;
		reactor.componentStatuses = Collections.emptyMap();
		StfrManager.get(world).markDirty();
		return true;
	}
}

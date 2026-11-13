package fun.aloyenz.stfr.reactor;

import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockReactorLight;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

public final class StfrChamberLight {

	private StfrChamberLight() {}

	public static double formation(StfrReactor.RunState state, double ticks) {
		if (state == StfrReactor.RunState.STARTUP_CONFIRMED)
			return Math.max(0, Math.min(1, (ticks - 415D) / 170D));
		if (state == StfrReactor.RunState.SHUTDOWN)
			return Math.max(0, Math.min(1, (470D - ticks) / 25D));
		if (state == StfrReactor.RunState.ONLINE
				|| state == StfrReactor.RunState.STARTUP_FAILURE
				|| state == StfrReactor.RunState.MELTDOWN) return 1D;
        if(state==StfrReactor.RunState.REACTION_LOSS || state==StfrReactor.RunState.UNDERLOAD)
            return .35;
        if (state.ordinal() >= StfrReactor.RunState.OVERLOAD.ordinal()) {
			if (state == StfrReactor.RunState.SELF_DESTRUCT) return ticks < 2485 ? 0D : 1D;
			return 1D;
		}
		return 0D;
	}

	public static int level(StfrReactor.RunState state, double ticks) {
		double formation = formation(state, ticks);
		return formation <= 0 ? 0 : Math.min(15, 1 + (int) Math.floor(formation * 14));
	}

	static void sync(World world, BlockPos center, int level) {
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, center);
		Map<BlockPos, StfrBlockRole> roles =
				analyzer == null ? StfrStructureTemplate.airRoles() : analyzer.airRoles();
		BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
		for (Map.Entry<BlockPos, StfrBlockRole> entry : roles.entrySet()) {
			if (!entry.getValue().requiresAir()) continue;
			BlockPos pos = origin.add(entry.getKey());
			if (!world.isBlockLoaded(pos)) continue;
			IBlockState current = world.getBlockState(pos);
			if (level == 0) {
				if (current.getBlock() == STFRBlocks.REACTOR_LIGHT) world.setBlockToAir(pos);
			} else if (current.getBlock() == STFRBlocks.REACTOR_LIGHT || world.isAirBlock(pos)) {
				IBlockState desired =
						STFRBlocks.REACTOR_LIGHT
								.getDefaultState()
								.withProperty(BlockReactorLight.LEVEL, level);
				if (current != desired) world.setBlockState(pos, desired, 3);
			}
		}
	}
}

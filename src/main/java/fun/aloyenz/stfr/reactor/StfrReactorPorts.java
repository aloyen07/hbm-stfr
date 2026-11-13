package fun.aloyenz.stfr.reactor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

final class StfrReactorPorts {

	private StfrReactorPorts() {}

	static boolean portOperational(StfrReactor r, BlockPos pos) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			if (direction == null) continue;
			BlockPos base = origin.add(cell.localPos);
			if (base.equals(pos) || base.offset(direction).equals(pos))
				return (r.structuralMask & 1 << direction.ordinal()) != 0
						&& (r.componentStatuses.get(direction) == null
								|| r.componentStatuses.get(direction).criticalFailures == 0);
		}
		return false;
	}

	static boolean portOperational(StfrReactor r, World world, BlockPos pos) {
		if (!r.portOperational(pos)) return false;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		BlockPos localPort = pos.subtract(origin);
		EnumFacing direction = StfrRadialSymmetry.sourceDirection(localPort);
		if (direction == null) return false;
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, r.center);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (StfrRadialSymmetry.sourceDirection(cell.localPos) != direction) continue;
			StfrBlockRole role =
					analyzer == null ? cell.defaultRole : analyzer.getRole(cell.localPos);
			if (role != StfrBlockRole.CRITICAL && role != StfrBlockRole.SUPERCRITICAL) continue;
			BlockPos target = origin.add(cell.localPos);
			if (!world.isBlockLoaded(target)) return false;
			IBlockState actual = world.getBlockState(target);
			actual = StfrStructureValidator.physicalState(world, target, cell, actual, analyzer);
			if (!StfrStructureValidator.matchesCell(cell, role, actual, world, target, analyzer))
				return false;
		}
		return true;
	}
}

package fun.aloyenz.stfr.tile;

import static fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer.*;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Block-state compatibility for editable structure roles. */
final class StfrAnalyzerMatching {
	private StfrAnalyzerMatching() {}

	static boolean matches(
			IBlockState expected, IBlockState actual, IBlockAccess blocks, BlockPos pos) {
		if (expected.getBlock() != actual.getBlock()) return false;
		if (expected.getBlock() instanceof BlockStairs) {
			expected = expected.getActualState(blocks, pos);
			actual = actual.getActualState(blocks, pos);
		}
		if (expected.equals(actual)) return true;
		if (!(expected.getBlock() instanceof BlockStairs)
				|| !(actual.getBlock() instanceof BlockStairs)) return false;
		if (expected.getValue(BlockStairs.HALF) != actual.getValue(BlockStairs.HALF)) return false;
		return innerCorner(expected) >= 0 && innerCorner(expected) == innerCorner(actual);
	}

	static int innerCorner(IBlockState state) {
		BlockStairs.EnumShape shape = state.getValue(BlockStairs.SHAPE);
		EnumFacing facing = state.getValue(BlockStairs.FACING);
		if (shape == BlockStairs.EnumShape.INNER_LEFT) {
			switch (facing) {
				case WEST:
					return 0;
				case SOUTH:
					return 1;
				case EAST:
					return 2;
				case NORTH:
					return 3;
				default:
					return -1;
			}
		}
		if (shape == BlockStairs.EnumShape.INNER_RIGHT) {
			switch (facing) {
				case SOUTH:
					return 0;
				case EAST:
					return 1;
				case NORTH:
					return 2;
				case WEST:
					return 3;
				default:
					return -1;
			}
		}
		return -1;
	}
}

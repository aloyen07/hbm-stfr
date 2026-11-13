package fun.aloyenz.stfr.structure;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/** Rotates a cell from one of the six turbine directions to all six directions. */
public final class StfrRadialSymmetry {

	private StfrRadialSymmetry() {}

	/** The same material's horizontal and vertical stairs are one radial role type. */
	public static boolean sameBlockType(Block first, Block second) {
		if (first == second) return true;
		ResourceLocation a = first.getRegistryName();
		ResourceLocation b = second.getRegistryName();
		if (a == null
				|| b == null
				|| !"hbmstfr".equals(a.getNamespace())
				|| !"hbmstfr".equals(b.getNamespace())) return false;
		String x = a.getPath();
		String y = b.getPath();
		if (!x.endsWith("_stairs") || !y.endsWith("_stairs")) return false;
		return x.equals("vertical_" + y) || y.equals("vertical_" + x);
	}

	public static Map<EnumFacing, BlockPos> copies(BlockPos local, EnumFacing source) {
		Map<EnumFacing, BlockPos> result = new EnumMap<>(EnumFacing.class);
		Frame original = new Frame(source);
		BlockPos relative = local.subtract(StfrStructureTemplate.CENTER);
		int depth = dot(relative, original.radial);
		int height = dot(relative, original.vertical);
		int side = dot(relative, original.lateral);
		for (EnumFacing direction : EnumFacing.values()) {
			Frame frame = new Frame(direction);
			BlockPos rotated =
					StfrStructureTemplate.CENTER
							.add(scale(frame.radial, depth))
							.add(scale(frame.vertical, height))
							.add(scale(frame.lateral, side));
			result.put(direction, rotated);
		}
		return result;
	}

	public static EnumFacing sourceDirection(BlockPos local) {
		BlockPos delta = local.subtract(StfrStructureTemplate.CENTER);
		int x = Math.abs(delta.getX());
		int y = Math.abs(delta.getY());
		int z = Math.abs(delta.getZ());
		if (x == 0 && y == 0 && z == 0) return null;
		if (x >= y && x >= z) return delta.getX() >= 0 ? EnumFacing.EAST : EnumFacing.WEST;
		if (y >= z) return delta.getY() >= 0 ? EnumFacing.UP : EnumFacing.DOWN;
		return delta.getZ() >= 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
	}

	/** Four side electrode chains, numbered in the turbine's local frame. */
	public static int secondaryElectrodeIndex(BlockPos local, EnumFacing direction) {
		Frame frame = new Frame(direction);
		BlockPos relative = local.subtract(StfrStructureTemplate.CENTER);
		int vertical = dot(relative, frame.vertical);
		int lateral = dot(relative, frame.lateral);
		if (vertical > 0 && lateral == 0) return 0;
		if (lateral > 0 && vertical == 0) return 1;
		if (vertical < 0 && lateral == 0) return 2;
		if (lateral < 0 && vertical == 0) return 3;
		return -1;
	}

	/** Distance from the chamber center along one stabilizer's axis. */
	public static int radialDepth(BlockPos local, EnumFacing direction) {
		return dot(local.subtract(StfrStructureTemplate.CENTER), direction);
	}

	private static BlockPos scale(EnumFacing direction, int amount) {
		return new BlockPos(
				direction.getXOffset() * amount,
				direction.getYOffset() * amount,
				direction.getZOffset() * amount);
	}

	private static int dot(BlockPos pos, EnumFacing direction) {
		return pos.getX() * direction.getXOffset()
				+ pos.getY() * direction.getYOffset()
				+ pos.getZ() * direction.getZOffset();
	}

	private static EnumFacing cross(EnumFacing a, EnumFacing b) {
		int x = a.getYOffset() * b.getZOffset() - a.getZOffset() * b.getYOffset();
		int y = a.getZOffset() * b.getXOffset() - a.getXOffset() * b.getZOffset();
		int z = a.getXOffset() * b.getYOffset() - a.getYOffset() * b.getXOffset();
		return EnumFacing.getFacingFromVector(x, y, z);
	}

	private static final class Frame {

		final EnumFacing radial;
		final EnumFacing vertical;
		final EnumFacing lateral;

		Frame(EnumFacing radial) {
			this.radial = radial;
			this.vertical =
					radial == EnumFacing.UP
							? EnumFacing.SOUTH
							: radial == EnumFacing.DOWN ? EnumFacing.NORTH : EnumFacing.UP;
			this.lateral = cross(vertical, radial);
		}
	}
}

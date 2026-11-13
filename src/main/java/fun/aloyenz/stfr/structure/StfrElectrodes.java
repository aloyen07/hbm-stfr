package fun.aloyenz.stfr.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockTechnetiumElectrode;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;

/** Independent physical electrode chains and the same mounting geometry for models and beams. */
public final class StfrElectrodes {

	private static List<Chain> chains;

	private StfrElectrodes() {}

	public static List<Chain> chains() {
		if (chains != null) return chains;
		Map<String, Chain> found = new LinkedHashMap<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!(cell.expected.getBlock() instanceof BlockTechnetiumElectrode)) continue;
			boolean primary = cell.defaultRole == StfrBlockRole.CRITICAL;
			if (!primary && cell.defaultRole != StfrBlockRole.ELECTRODE) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			if (direction == null) continue;
			int index =
					primary
							? -1
							: StfrRadialSymmetry.secondaryElectrodeIndex(cell.localPos, direction);
			if (!primary && index < 0) continue;
			String key = direction.getIndex() + ":" + index;
			Chain old = found.get(key);
			if (old == null
					|| cell.localPos.distanceSq(StfrStructureTemplate.CENTER)
							< old.electrode.localPos.distanceSq(StfrStructureTemplate.CENTER))
				found.put(key, new Chain(cell, direction, index));
		}
		chains = Collections.unmodifiableList(new ArrayList<>(found.values()));
		return chains;
	}

	public static EnumFacing facing(IBlockState state) {
		for (IProperty<?> property : state.getPropertyKeys()) {
			if (!"facing".equals(property.getName())) continue;
			Object value = state.getValue(property);
			if (value instanceof EnumFacing) return (EnumFacing) value;
		}
		return EnumFacing.UP;
	}

	public static Vec3d mount(BlockPos pos, IBlockState state) {
		return mount(pos, facing(state));
	}

	public static Vec3d mount(BlockPos pos, EnumFacing facing) {
		return new Vec3d(
				pos.getX() + .5 - facing.getXOffset() * .5,
				pos.getY() + .5 - facing.getYOffset() * .5,
				pos.getZ() + .5 - facing.getZOffset() * .5);
	}

	public static Vec3d tip(BlockPos pos, IBlockState state, BlockPos center) {
		Vec3d mount = mount(pos, state);
		return tip(mount, center);
	}

	public static Vec3d tip(Vec3d mount, BlockPos center) {
		Vec3d target = new Vec3d(center.getX() + .5, center.getY() + .5, center.getZ() + .5);
		return mount.add(target.subtract(mount).normalize());
	}

	public static final class Chain {

		public final StfrStructureTemplate.Cell electrode;
		public final EnumFacing direction;
		public final int index;
		private final BlockPos mechanism, controller;

		Chain(StfrStructureTemplate.Cell electrode, EnumFacing direction, int index) {
			this.electrode = electrode;
			this.direction = direction;
			this.index = index;
			EnumFacing back = facing(electrode.expected).getOpposite();
			mechanism = electrode.localPos.offset(back);
			controller = index < 0 ? null : mechanism.offset(back);
		}

		public boolean works(World world, BlockPos center) {
			BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
			return matches(world, origin, electrode.localPos)
					&& matches(world, origin, mechanism)
					&& (controller == null || matches(world, origin, controller));
		}

		private boolean matches(World world, BlockPos origin, BlockPos local) {
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			BlockPos pos = origin.add(local);
			if (cell == null || !world.isBlockLoaded(pos)) return false;
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() == STFRBlocks.ELECTRODE_PROXY) {
				TileEntity tile = world.getTileEntity(pos);
				if (!(tile instanceof TileEntityElectrodeProxy)) return false;
				state = ((TileEntityElectrodeProxy) tile).original();
				if (state == null) return false;
			}
			return StfrStructureValidator.matchesCell(cell, cell.defaultRole, state, world, pos);
		}

		public Vec3d relativeTip() {
			return tip(electrode.localPos, electrode.expected, StfrStructureTemplate.CENTER)
					.subtract(new Vec3d(14.5, 14.5, 14.5));
		}
	}
}

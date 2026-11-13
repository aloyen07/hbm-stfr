package fun.aloyenz.stfr.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** Self-contained visual state for a spinning block; no analyzer is needed. */
public final class TileEntityRotorPlaceholder extends TileEntity {

	private IBlockState original;
	private BlockPos center = BlockPos.ORIGIN;
	private BlockPos pivot = BlockPos.ORIGIN;
	private EnumFacing axis = EnumFacing.UP;

	public void configure(IBlockState original, BlockPos center, BlockPos pivot, EnumFacing axis) {
		this.original = original;
		this.center = center;
		this.pivot = pivot;
		this.axis = axis;
		markDirty();
		if (world != null && !world.isRemote) {
			IBlockState block = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, block, block, 3);
		}
	}

	public IBlockState original() {
		return original;
	}

	public BlockPos center() {
		return center;
	}

	public BlockPos pivot() {
		return pivot;
	}

	public EnumFacing axis() {
		return axis;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 192D * 192D;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos, pos.add(1, 1, 1))
				.union(new AxisAlignedBB(pivot, pivot.add(1, 1, 1)))
				.grow(8D);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (original != null)
			tag.setTag("original", NBTUtil.writeBlockState(new NBTTagCompound(), original));
		tag.setLong("center", center.toLong());
		tag.setLong("pivot", pivot.toLong());
		tag.setByte("axis", (byte) axis.getIndex());
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		original =
				tag.hasKey("original", 10)
						? NBTUtil.readBlockState(tag.getCompoundTag("original"))
						: null;
		center = BlockPos.fromLong(tag.getLong("center"));
		pivot = BlockPos.fromLong(tag.getLong("pivot"));
		axis = EnumFacing.byIndex(tag.getByte("axis"));
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
	}
}

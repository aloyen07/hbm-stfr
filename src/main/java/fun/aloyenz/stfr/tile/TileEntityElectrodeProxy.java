package fun.aloyenz.stfr.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** Original electrode and beam classification survive analyzer removal and chunk reloads. */
public final class TileEntityElectrodeProxy extends TileEntity {

	private IBlockState original;
	private BlockPos center = BlockPos.ORIGIN;
	private boolean primary;

	public void configure(IBlockState state, BlockPos center, boolean primary) {
		original = state;
		this.center = center;
		this.primary = primary;
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

	public boolean primary() {
		return primary;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 192D * 192D;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.add(-1, -1, -1), pos.add(2, 2, 2));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (original != null)
			tag.setTag("original", NBTUtil.writeBlockState(new NBTTagCompound(), original));
		tag.setLong("center", center.toLong());
		tag.setBoolean("primary", primary);
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
		primary = tag.getBoolean("primary");
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

package fun.aloyenz.stfr.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import fun.aloyenz.stfr.reactor.StfrLensThawing;

/** Keeps thaw progress when a partly heated lens is placed back in the reactor. */
public final class TileEntityFrozenLens extends TileEntity {
	private NBTTagCompound lensData = new NBTTagCompound();

	@Override public void onLoad() {
		if (!world.isRemote) {
			StfrLensThawing.remaining(lensData, world.rand);
			markDirty();
		}
	}

	public NBTTagCompound lensData() {
		if (world != null && !world.isRemote) StfrLensThawing.remaining(lensData, world.rand);
		return lensData.copy();
	}

	public void setLensData(NBTTagCompound data) {
		lensData = data == null ? new NBTTagCompound() : data.copy();
		if (world != null && !world.isRemote) StfrLensThawing.remaining(lensData, world.rand);
		markDirty();
	}

	@Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setTag("lensData", lensData.copy());
		return tag;
	}

	@Override public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		lensData = tag.getCompoundTag("lensData").copy();
	}
}

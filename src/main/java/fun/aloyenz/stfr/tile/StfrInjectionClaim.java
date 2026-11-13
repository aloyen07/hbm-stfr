package fun.aloyenz.stfr.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.block.BlockSolidInjector;

/** Persistent ownership of the injection point shared by adjacent feed devices. */
public final class StfrInjectionClaim {
	private long acquired = -1;
	private BlockPos injectionTarget;

	public void release(TileEntity owner) {
		if (acquired < 0) return;
		acquired = -1;
		injectionTarget = null;
		owner.markDirty();
	}

	public boolean acquire(TileEntity owner) {
		BlockPos target = target(owner);
		if (target == null) return false;
		if (injectionTarget != null && !injectionTarget.equals(target)) release(owner);
		long next = Math.max(0, owner.getWorld().getTotalWorldTime());
		for (EnumFacing side : EnumFacing.VALUES) {
			BlockPos neighbour = target.offset(side);
			if (neighbour.equals(owner.getPos()) || !owner.getWorld().isBlockLoaded(neighbour)) continue;
			TileEntity other = owner.getWorld().getTileEntity(neighbour);
			StfrInjectionClaim claim = of(other);
			if (claim == null) continue;
			if (!enabled(other) || claim.injectionTarget != null
					&& !claim.injectionTarget.equals(target(other))) {
				claim.release(other);
				continue;
			}
			if (claim.acquired < 0 || !target.equals(target(other))) continue;
			next = Math.max(next, claim.acquired + 1);
			if (acquired < 0 || claim.acquired < acquired
					|| claim.acquired == acquired && other.getPos().compareTo(owner.getPos()) < 0) {
				owner.getWorld().destroyBlock(owner.getPos(), true);
				return false;
			}
		}
		if (acquired < 0) {
			acquired = next;
			owner.markDirty();
		}
		if (injectionTarget == null) {
			injectionTarget = target;
			owner.markDirty();
		}
		return true;
	}

	private static boolean enabled(TileEntity tile) {
		return tile instanceof TileEntityFuelSlot slot ? slot.isLocked()
				: tile instanceof TileEntitySolidInjector injector && injector.injectionEnabled();
	}

	private static StfrInjectionClaim of(TileEntity tile) {
		if (tile == null || tile.isInvalid()) return null;
		if (tile instanceof TileEntityFuelSlot slot) return slot.injectionClaim;
		if (tile instanceof TileEntitySolidInjector injector) return injector.injectionClaim;
		return null;
	}

	public static BlockPos target(TileEntity tile) {
		if (tile == null || tile.getWorld() == null) return null;
		IBlockState state = tile.getWorld().getBlockState(tile.getPos());
		if (tile instanceof TileEntityFuelSlot)
			return tile.getPos().offset(state.getValue(BlockFuelSlot.FACING));
		if (tile instanceof TileEntitySolidInjector)
			return tile.getPos().offset(state.getValue(BlockSolidInjector.FACING));
		return null;
	}

	public void write(NBTTagCompound tag) {
		tag.setLong("injectionClaim", acquired);
		if (injectionTarget != null) tag.setLong("injectionTarget", injectionTarget.toLong());
		else tag.removeTag("injectionTarget");
	}
	public void read(NBTTagCompound tag) {
		acquired = tag.hasKey("injectionClaim") ? tag.getLong("injectionClaim") : -1;
		injectionTarget = tag.hasKey("injectionTarget") ? BlockPos.fromLong(tag.getLong("injectionTarget")) : null;
	}
}

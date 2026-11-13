package fun.aloyenz.stfr.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import fun.aloyenz.stfr.reactor.*;

/** A persistent solid-to-liquid buffer with metered reactor feed. */
public final class TileEntitySolidInjector extends TileEntity implements ITickable, IInventory {

	public static final int CAPACITY = 16000, MAX_INTERVAL = 72000;

	public enum RedstoneMode {
		IGNORE,
		HIGH,
		LOW;

		public boolean permits(boolean powered) {
			return this == IGNORE || (this == HIGH) == powered;
		}
	}

	final StfrInjectionClaim injectionClaim = new StfrInjectionClaim();
	private ItemStack input = ItemStack.EMPTY;
	private double polonium;
	private int dose = 2, interval = 600;
	private RedstoneMode mode = RedstoneMode.IGNORE;

	public int polonium() {
		return (int) Math.ceil(polonium - 1E-9);
	}

	public int dose() {
		return dose;
	}

	public int interval() {
		return interval;
	}

	public RedstoneMode mode() {
		return mode;
	}

	public void configure(int dose, int interval, int mode) {
		this.dose = Math.max(1, Math.min(CAPACITY, dose));
		this.interval = Math.max(1, Math.min(MAX_INTERVAL, interval));
		this.mode = RedstoneMode.values()[Math.max(0, Math.min(2, mode))];
		markDirty();
	}

	public ItemStack eject() {
		return removeStackFromSlot(0);
	}

	public ItemStack contents() {
		return input;
	}

	public boolean injectionEnabled() {
		return world != null && mode.permits(world.isBlockPowered(pos));
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		if (injectionEnabled()) {
			if (!injectionClaim.acquire(this)) return;
		} else injectionClaim.release(this);
		int volume = StfrPoloniumFeed.volume(input);
		if (volume > 0) {
			int count =
					Math.min(
							input.getCount(),
							(int) Math.floor((CAPACITY - polonium + 1E-9) / volume));
			if (count > 0) {
				polonium += count * volume;
				input.shrink(count);
				markDirty();
			}
		}
		if (polonium <= 0D || !mode.permits(world.isBlockPowered(pos))) return;
		StfrReactor reactor = StfrManager.get(world).forSolidInjector(world, pos);
		if (reactor == null || !reactor.digammaActive()) return;
		double transferred = reactor.insertPolonium(Math.min(polonium, dose / (double) interval));
		if (transferred > 0D) {
			polonium = Math.max(0D, polonium - transferred);
			markDirty();
			StfrManager.get(world).markDirty();
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		injectionClaim.write(tag);
		if (!input.isEmpty()) tag.setTag("input", input.writeToNBT(new NBTTagCompound()));
		tag.setDouble("polonium", polonium);
		tag.setInteger("dose", dose);
		tag.setInteger("interval", interval);
		tag.setInteger("redstoneMode", mode.ordinal());
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		injectionClaim.read(tag);
		// Old inventories contain at most 32 billets: migrate without losing material.
		input =
				tag.hasKey("input", 10)
						? new ItemStack(tag.getCompoundTag("input"))
						: tag.hasKey("billets", 10)
								? new ItemStack(tag.getCompoundTag("billets"))
								: ItemStack.EMPTY;
		double stored = tag.getDouble("polonium");
		polonium = Double.isFinite(stored) ? Math.max(0D, Math.min(CAPACITY, stored)) : 0D;
		dose = tag.hasKey("dose") ? Math.max(1, Math.min(CAPACITY, tag.getInteger("dose"))) : 2;
		interval =
				tag.hasKey("interval")
						? Math.max(1, Math.min(MAX_INTERVAL, tag.getInteger("interval")))
						: 600;
		if (dose == 96 && interval == 20) {
			dose = 2;
			interval = 600;
		}
		mode = RedstoneMode.values()[Math.max(0, Math.min(2, tag.getInteger("redstoneMode")))];
	}

	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return input.isEmpty();
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return index == 0 ? input : ItemStack.EMPTY;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if (index != 0 || count <= 0) return ItemStack.EMPTY;
		ItemStack result = input.splitStack(count);
		markDirty();
		return result;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		if (index != 0) return ItemStack.EMPTY;
		ItemStack result = input;
		input = ItemStack.EMPTY;
		markDirty();
		return result;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if (index != 0) return;
		input = stack;
		if (!input.isEmpty() && input.getCount() > getInventoryStackLimit())
			input.setCount(getInventoryStackLimit());
		markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return world != null && world.getTileEntity(pos) == this && player.getDistanceSq(pos) <= 64;
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return index == 0 && StfrPoloniumFeed.volume(stack) > 0;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		input = ItemStack.EMPTY;
		markDirty();
	}

	@Override
	public String getName() {
		return "tile.hbmstfr.solid_injector.name";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentTranslation(getName());
	}
}

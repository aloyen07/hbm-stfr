package fun.aloyenz.stfr.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;

public final class ContainerSolidInjector extends Container {

	public final TileEntitySolidInjector tile;
	public int polonium, dose = 2, interval = 600, mode;
	private final int[] previous = {-1, -1, -1, -1, -1};

	public ContainerSolidInjector(EntityPlayer player, TileEntitySolidInjector tile) {
		this.tile = tile;
		addSlotToContainer(
				new Slot(tile, 0, 26, 42) {
					@Override
					public boolean isItemValid(ItemStack stack) {
						return tile.isItemValidForSlot(0, stack);
					}
				});
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlotToContainer(
						new Slot(
								player.inventory, col + row * 9 + 9, 8 + col * 18, 138 + row * 18));
		for (int col = 0; col < 9; col++)
			addSlotToContainer(new Slot(player.inventory, col, 8 + col * 18, 196));
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile.isUsableByPlayer(player);
	}

	private int[] values() {
		return new int[] {
			tile.polonium(),
			tile.dose(),
			tile.interval() & 65535,
			tile.interval() >>> 16,
			tile.mode().ordinal()
		};
	}

	@Override
	public void addListener(IContainerListener listener) {
		super.addListener(listener);
		int[] values = values();
		for (int i = 0; i < values.length; i++) listener.sendWindowProperty(this, i, values[i]);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		int[] values = values();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != previous[i])
				for (IContainerListener listener : listeners)
					listener.sendWindowProperty(this, i, values[i]);
			previous[i] = values[i];
		}
	}

	@Override
	public void updateProgressBar(int id, int value) {
		switch (id) {
			case 0:
				polonium = value & 65535;
				break;
			case 1:
				dose = value & 65535;
				break;
			case 2:
				interval = (interval & 0xFFFF0000) | (value & 65535);
				break;
			case 3:
				interval = (interval & 65535) | ((value & 65535) << 16);
				break;
			case 4:
				mode = value;
				break;
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = inventorySlots.get(index);
		if (!slot.getHasStack()) return ItemStack.EMPTY;
		ItemStack stack = slot.getStack(), original = stack.copy();
		if (index == 0) {
			if (!mergeItemStack(stack, 1, 37, true)) return ItemStack.EMPTY;
		} else if (tile.isItemValidForSlot(0, stack)) {
			if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
		} else if (index < 28) {
			if (!mergeItemStack(stack, 28, 37, false)) return ItemStack.EMPTY;
		} else if (!mergeItemStack(stack, 1, 28, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
		else slot.onSlotChanged();
		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}

package fun.aloyenz.stfr.security;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.*;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public final class ContainerStfrSecurity extends Container {

	public final InventoryBasic cards = new InventoryBasic("STFR", false, 2);
	public final boolean detonator;
	private final World world;
	private final BlockPos pos;
	private final int heldSlot;
	private final UUID deviceId;
	public int enabled, targetRights, actorValid, abortable, result;

	public ContainerStfrSecurity(EntityPlayer p, boolean detonator, BlockPos pos, int heldSlot) {
		world = p.world;
		this.detonator = detonator;
		this.pos = pos;
		this.heldSlot = heldSlot;
		NBTTagCompound firmware =
				detonator ? p.inventory.getStackInSlot(heldSlot).getSubCompound("hbmstfr") : null;
		deviceId =
				firmware != null && firmware.hasUniqueId("device")
						? firmware.getUniqueId("device")
						: null;
		for (int i = 0; i < (detonator ? 1 : 2); i++)
			addSlotToContainer(
					new Slot(cards, i, detonator ? 80 : 53 + i * 54, 32) {
						public boolean isItemValid(ItemStack stack) {
							return stack.getItem() == STFRItems.KEY_CARD;
						}

						public int getSlotStackLimit() {
							return 1;
						}
					});
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlotToContainer(
						new Slot(p.inventory, col + row * 9 + 9, 8 + col * 18, 178 + row * 18));
		for (int col = 0; col < 9; col++)
			addSlotToContainer(new Slot(p.inventory, col, 8 + col * 18, 236));
	}

	public boolean canInteractWith(EntityPlayer p) {
		if (detonator) {
			if (heldSlot < 0 || heldSlot >= p.inventory.getSizeInventory()) return false;
			ItemStack stack = p.inventory.getStackInSlot(heldSlot);
			NBTTagCompound tag = stack.getSubCompound("hbmstfr");
			return stack.getItem() == STFRItems.DETONATOR
					&& deviceId != null
					&& tag != null
					&& tag.hasUniqueId("device")
					&& deviceId.equals(tag.getUniqueId("device"));
		}
		return world.isBlockLoaded(pos)
				&& world.getBlockState(pos).getBlock() == STFRBlocks.PRIVILEGES
				&& p.getDistanceSq(pos) <= 64;
	}

	private UUID network(EntityPlayer p) {
		if (detonator) {
			NBTTagCompound tag = p.inventory.getStackInSlot(heldSlot).getSubCompound("hbmstfr");
			return tag != null && tag.hasUniqueId("network") ? tag.getUniqueId("network") : null;
		}
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(world, pos);
		return net.networkId;
	}

	public void act(EntityPlayer p, int action, UUID revoke) {
		if (detonator ? action != 10 && action != 11 : action < 0 || action > 6) return;
		if (world.isRemote || !canInteractWith(p)) return;
		StfrLogicNetwork.invalidate(world);
		UUID network = network(p);
		if (network == null) {
			result = 2;
			return;
		}
		StfrSecurityData data = StfrSecurityData.get(world);
		StfrAccessPolicy policy = data.policy(network);
		UUID actor = StfrSecurity.cardId(cards.getStackInSlot(0));
		if (actor == null
				|| !policy.allows(
						actor, detonator ? StfrAccessPolicy.DETONATE : StfrAccessPolicy.ADMIN)) {
			result = 1;
			return;
		}
		if (detonator) {
			NBTTagCompound firmware =
					p.inventory.getStackInSlot(heldSlot).getSubCompound("hbmstfr");
			if (firmware.getInteger("dimension") != world.provider.getDimension()) {
				result = 2;
				return;
			}
			BlockPos mainframe = BlockPos.fromLong(firmware.getLong("mainframe"));
			if (!world.isBlockLoaded(mainframe)) {
				result = 2;
				return;
			}
			StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(world, mainframe);
			boolean connectorOnline = false;
			if (firmware.hasKey("connector")) {
				BlockPos connector = BlockPos.fromLong(firmware.getLong("connector"));
				TileEntity tile =
						world.isBlockLoaded(connector) ? world.getTileEntity(connector) : null;
				if (tile instanceof TileEntityLogicDevice
						&& ((TileEntityLogicDevice) tile).kind()
								== BlockLogicDevice.Kind.DETONATOR_CONNECTOR) {
					StfrLogicNetwork.Snapshot live = StfrLogicNetwork.inspect(world, connector);
					connectorOnline = live.ioAvailable && network.equals(live.networkId);
				}
			} else
				for (BlockPos device : net.devices) {
					TileEntity tile = world.getTileEntity(device);
					if (tile instanceof TileEntityLogicDevice
							&& ((TileEntityLogicDevice) tile).kind()
									== BlockLogicDevice.Kind.DETONATOR_CONNECTOR
							&& StfrLogicNetwork.inspect(world, device).ioAvailable)
						connectorOnline = true;
				}
			if (!connectorOnline) {
				result = 2;
				return;
			}
			StfrReactor reactor = net.reactor;
			if (!network.equals(net.networkId)
					|| reactor == null
					|| !firmware.getUniqueId("reactor").equals(reactor.id)) {
				result = 2;
				return;
			}
			boolean changed =
					action == 10
							? reactor.emergency().selfDestruct(world)
							: action == 11 && reactor.emergency().abortSelfDestruct(world);
			if (changed) StfrManager.get(world).markDirty();
			result = changed ? 3 : 4;
		} else {
			if (action == 0) {
				policy.setEnabled(!policy.enabled());
				data.markDirty();
				result = 3;
			} else {
				UUID target = action == 6 ? revoke : StfrSecurity.cardId(cards.getStackInSlot(1));
				if (target == null) {
					result = 1;
					return;
				}
				int right = action >= 1 && action <= 4 ? 1 << (action - 1) : 0;
				int rights = action >= 5 ? 0 : policy.rights(target) ^ right;
				result = policy.grant(target, rights) ? 3 : 5;
				data.markDirty();
			}
		}
		StfrLogicNetwork.invalidate(world);
		detectAndSendChanges();
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (world.isRemote) return;
		EntityPlayer p = null;
		for (IContainerListener listener : listeners)
			if (listener instanceof EntityPlayer) p = (EntityPlayer) listener;
		if (p != null) {
			UUID net = network(p);
			if (net != null) {
				StfrAccessPolicy policy = StfrSecurityData.get(world).policy(net);
				enabled = policy.enabled() ? 1 : 0;
				targetRights = policy.rights(StfrSecurity.cardId(cards.getStackInSlot(1)));
				actorValid =
						policy.allows(
										StfrSecurity.cardId(cards.getStackInSlot(0)),
										detonator
												? StfrAccessPolicy.DETONATE
												: StfrAccessPolicy.ADMIN)
								? 1
								: 0;
				if (detonator) {
					NBTTagCompound tag =
							p.inventory.getStackInSlot(heldSlot).getSubCompound("hbmstfr");
					StfrReactor reactor =
							tag == null
									? null
									: StfrManager.get(world)
											.get(BlockPos.fromLong(tag.getLong("center")));
					abortable = reactor != null && reactor.emergency().canAbortSelfDestruct() ? 1 : 0;
				}
			}
		}
		for (IContainerListener listener : listeners) {
			listener.sendWindowProperty(this, 0, enabled);
			listener.sendWindowProperty(this, 1, targetRights);
			listener.sendWindowProperty(this, 2, actorValid);
			listener.sendWindowProperty(this, 3, abortable);
			listener.sendWindowProperty(this, 4, result);
		}
	}

	@Override
	public void updateProgressBar(int id, int value) {
		switch (id) {
			case 0:
				enabled = value;
				break;
			case 1:
				targetRights = value;
				break;
			case 2:
				actorValid = value;
				break;
			case 3:
				abortable = value;
				break;
			case 4:
				result = value;
				break;
			default:
				break;
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer p, int index) {
		Slot slot = inventorySlots.get(index);
		if (!slot.getHasStack()) return ItemStack.EMPTY;
		ItemStack stack = slot.getStack(), copy = stack.copy();
		int count = detonator ? 1 : 2;
		if (index < count) {
			if (!mergeItemStack(stack, count, inventorySlots.size(), true)) return ItemStack.EMPTY;
		} else if (stack.getItem() != STFRItems.KEY_CARD || !mergeItemStack(stack, 0, count, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
		else slot.onSlotChanged();
		return copy;
	}

	@Override
	public void onContainerClosed(EntityPlayer p) {
		super.onContainerClosed(p);
		if (!world.isRemote) clearContainer(p, world, cards);
	}
}

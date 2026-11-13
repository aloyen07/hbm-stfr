package fun.aloyenz.stfr.tile;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.sound.STFRSounds;

public final class TileEntityFuelSlot extends TileEntity implements ITickable {

	public static final int ANIMATION_TICKS = 33;
	public static final byte EMPTY = 0;
	public static final byte INSERTING = 1;
	public static final byte READY = 2;
	public static final byte EXTRACTING = 3;

	private ItemStack capsule = ItemStack.EMPTY;
	private byte phase = EMPTY;
	private int animationTicks;
	private boolean locked;
	final StfrInjectionClaim injectionClaim = new StfrInjectionClaim();

	public boolean activate(EntityPlayer player, EnumHand hand) {
		if (world == null) return false;
		if (world.isRemote) return true;

		ItemStack held = player.getHeldItem(hand);
		if (phase == EMPTY) {
			if (held.isEmpty() || held.getItem() != STFRItems.FUEL_CAPSULE) return false;
			capsule = held.copy();
			capsule.setCount(1);
			capsule.setItemDamage(0);
			if (!player.capabilities.isCreativeMode) held.shrink(1);
			phase = INSERTING;
			animationTicks = 0;
			locked = false;
			play(STFRSounds.SLIDE_IN);
			sync();
			return true;
		}

		if (phase != READY) return true;
		if (player.isSneaking()) {
			if (fillingInterlock()) {
				player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.capsule_locked"));
				return true;
			}
			StfrReactor reactor = StfrManager.get(world).forFuelSlot(pos);
			if (reactor != null) {
				reactor.refresh(world);
				if (!reactor.canRemoveCapsule(pos)) {
					player.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.fuel_slot.concurrent_refueling"));
					return true;
				}
			}
			if (locked) {
				locked = false;
				injectionClaim.release(this);
				capsule.setItemDamage(0);
				play(STFRSounds.CLACK_OUT);
			} else {
				phase = EXTRACTING;
				animationTicks = 0;
				play(STFRSounds.SLIDE_OUT);
			}
			sync();
			return true;
		}

		if (!locked) {
			if (!injectionClaim.acquire(this)) return true;
			locked = true;
			capsule.setItemDamage(1);
			play(STFRSounds.CLACK_IN);
			sync();
		}
		return true;
	}

	private boolean fillingInterlock() {
		TileEntity tile = world.getTileEntity(pos.offset(getFacing()));
		return tile instanceof TileEntityCapsuleFiller
				&& ((TileEntityCapsuleFiller) tile).locksSlot(pos);
	}

	public void lockForFilling() {
		if (world == null || world.isRemote || phase != READY || capsule.isEmpty() || locked) return;
		if (!injectionClaim.acquire(this)) return;
		locked = true;
		capsule.setItemDamage(1);
		play(STFRSounds.CLACK_IN);
		sync();
	}

	@Override
	public void update() {
		if (world != null && !world.isRemote) {
			if (locked) {
				if (!injectionClaim.acquire(this)) return;
			} else injectionClaim.release(this);
		}
		if (world != null && !world.isRemote && phase == EXTRACTING && fillingInterlock()) {
			phase = INSERTING;
			animationTicks = ANIMATION_TICKS - animationTicks;
			sync();
		}
		if (phase != INSERTING && phase != EXTRACTING) return;
		if (animationTicks < ANIMATION_TICKS) animationTicks++;
		if (world == null || world.isRemote || animationTicks < ANIMATION_TICKS) return;

		if (phase == INSERTING) {
			phase = READY;
		} else {
			ejectCapsule();
			phase = EMPTY;
			animationTicks = 0;
		}
		sync();
	}

	private void ejectCapsule() {
		if (capsule.isEmpty()) return;
		EnumFacing outward = getFacing().getOpposite();
		EntityItem entity =
				new EntityItem(
						world,
						pos.getX() + 0.5D + outward.getXOffset() * 0.8D,
						pos.getY() + 0.5D + outward.getYOffset() * 0.8D,
						pos.getZ() + 0.5D + outward.getZOffset() * 0.8D,
						capsule.copy());
		entity.motionX = outward.getXOffset() * 0.08D;
		entity.motionY = outward.getYOffset() * 0.08D;
		entity.motionZ = outward.getZOffset() * 0.08D;
		world.spawnEntity(entity);
		capsule = ItemStack.EMPTY;
		locked = false;
	}

	private EnumFacing getFacing() {
		if (world == null || !(world.getBlockState(pos).getBlock() instanceof BlockFuelSlot))
			return EnumFacing.NORTH;
		return world.getBlockState(pos).getValue(BlockFuelSlot.FACING);
	}

	private void play(SoundEvent sound) {
		world.playSound(
				null, pos, sound, SoundCategory.BLOCKS, 1F, 0.9F + world.rand.nextFloat() * 0.2F);
	}

	public ItemStack removeForBreak() {
		ItemStack result = capsule;
		if (!result.isEmpty()) result.setItemDamage(0);
		capsule = ItemStack.EMPTY;
		return result;
	}

	public ItemStack getCapsule() {
		return capsule;
	}

	/** Fill only a stationary, latched capsule; the handler owns its fluid NBT. */
	public int fillCapsule(FluidStack fluid) {
		if (world == null || world.isRemote || phase != READY || !locked
				|| capsule.isEmpty() || fluid == null || fluid.amount <= 0) return 0;
		int accepted = ItemFuelCapsule.fillInternal(capsule, fluid);
		if (accepted > 0) {
			markDirty();
			IBlockStateHolder.notify(world, pos);
		}
		return accepted;
	}

	/** Draw measured fuel from the installed capsule; the fluid handler updates its NBT. */
	public int drainFuel(int maximum) {
		if (world == null
				|| world.isRemote
				|| phase != READY
				|| !locked
				|| capsule.isEmpty()
				|| maximum <= 0) return 0;
		IFluidHandlerItem handler =
				capsule.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
		if (handler == null) return 0;
		FluidStack drained = handler.drain(maximum, true);
		if (drained == null || drained.amount <= 0) return 0;
		capsule = handler.getContainer();
		markDirty();
		IBlockStateHolder.notify(world, pos);
		return drained.amount;
	}

	public byte getPhase() {
		return phase;
	}

	public boolean isLocked() {
		return locked;
	}

	public float getRenderProgress(float partialTicks) {
		if (phase == EMPTY) return 1F;
		if (phase == READY) return 0F;
		float progress = Math.min(1F, (animationTicks + partialTicks) / ANIMATION_TICKS);
		return phase == INSERTING ? 1F - progress : progress;
	}

	private void sync() {
		markDirty();
		if (world != null && !world.isRemote) {
			StfrManager manager = StfrManager.get(world);
			StfrReactor reactor = manager.forFuelSlot(pos);
			if (reactor != null) {
				reactor.refresh(world);
				manager.markDirty();
			}
		}
		IBlockStateHolder.notify(world, pos);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setByte("phase", phase);
		compound.setInteger("animationTicks", animationTicks);
		compound.setBoolean("locked", locked);
		injectionClaim.write(compound);
		if (!capsule.isEmpty())
			compound.setTag("capsule", capsule.writeToNBT(new NBTTagCompound()));
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		phase = compound.getByte("phase");
		animationTicks = compound.getInteger("animationTicks");
		locked = compound.getBoolean("locked");
		injectionClaim.read(compound);
		capsule =
				compound.hasKey("capsule", 10)
						? new ItemStack(compound.getCompoundTag("capsule"))
						: ItemStack.EMPTY;
		if (!capsule.isEmpty()) capsule.setItemDamage(locked ? 1 : 0);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
	}

	private static final class IBlockStateHolder {

		private static void notify(World world, BlockPos pos) {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}
}

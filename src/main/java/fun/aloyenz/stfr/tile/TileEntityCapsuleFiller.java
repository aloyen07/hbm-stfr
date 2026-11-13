package fun.aloyenz.stfr.tile;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import java.util.Locale;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import fun.aloyenz.stfr.block.BlockCapsuleFiller;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.reactor.StfrFluids;
import fun.aloyenz.stfr.sound.STFRSounds;

/** Four lateral HBM pressure inputs, one front-mounted locked capsule. */
public final class TileEntityCapsuleFiller extends TileEntity
		implements ITickable, IFluidStandardTransceiverMK2 {
	private static final int BUFFER_CAPACITY = 16_000;
	private final FluidTankNTM deuterium =
			new FluidTankNTM(Fluids.DEUTERIUM, BUFFER_CAPACITY)
					.withPressure(5).withOwner(this);
	private final FluidTankNTM water =
			new FluidTankNTM(Fluids.WATER, BUFFER_CAPACITY)
					.withPressure(1).withOwner(this);
	private final FluidTankNTM nitrogen =
			new FluidTankNTM(StfrFluids.LIQUID_NITROGEN, BUFFER_CAPACITY)
					.withPressure(5).withOwner(this);
	private boolean poweredLastTick;
	public enum Mode {
		UNSELECTED, DEUTERIUM, WATER;

		public boolean accepts(FluidType type) {
			return this == WATER ? type == Fluids.WATER || StfrFluids.isLiquidNitrogen(type)
					: this == DEUTERIUM && type == Fluids.DEUTERIUM;
		}

		public int amount(int current, int capacity, int deuterium, int water, int nitrogen) {
			return this == WATER ? StfrCapsuleFilling.waterAmount(current, capacity, water, nitrogen)
					: this == DEUTERIUM ? StfrCapsuleFilling.amount(current, capacity, deuterium, deuterium) : 0;
		}
	}
	private Mode mode = Mode.UNSELECTED;

	public boolean locksSlot(BlockPos slotPos) {
		return world != null && pos.offset(front()).equals(slotPos) && world.isBlockPowered(pos);
	}

	public void selectMode(EntityPlayer player, FluidType type) {
		if (world.isBlockPowered(pos)) {
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.mode_locked"));
			return;
		}
		Mode selected = type == Fluids.WATER ? Mode.WATER
				: type == Fluids.DEUTERIUM ? Mode.DEUTERIUM : Mode.UNSELECTED;
		if (selected == Mode.UNSELECTED) {
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.unsupported"));
			return;
		}
		mode = selected;
		markDirty();
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.mode", modeName()));
	}

	private TextComponentTranslation modeName() {
		return new TextComponentTranslation("message.hbmstfr.capsule_filler.mode."
				+ mode.name().toLowerCase(Locale.ROOT));
	}

	public void sendStatus(EntityPlayer player) {
		if (mode == Mode.UNSELECTED) {
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.select_mode"));
			return;
		}
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.mode", modeName()));
		player.sendMessage(mode == Mode.WATER
				? new TextComponentTranslation("message.hbmstfr.capsule_filler.buffers.water", water.getFill(), BUFFER_CAPACITY, nitrogen.getFill())
				: new TextComponentTranslation("message.hbmstfr.capsule_filler.buffers.deuterium", deuterium.getFill(), BUFFER_CAPACITY));
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.status", capsuleAmount(), capsuleCapacity()));
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.capsule_filler.wait." + statusReason()));
	}

	private String statusReason() {
		TileEntityFuelSlot slot = slot();
		if (slot == null || slot.getCapsule().isEmpty()) return "no_capsule";
		if (slot.getPhase() != TileEntityFuelSlot.READY) return "moving";
		FluidStack contents = ItemFuelCapsule.getFluid(slot.getCapsule());
		if (contents != null && contents.amount > 0 && contents.getFluid()
				!= (mode == Mode.WATER ? Fluids.WATER.getFF() : Fluids.DEUTERIUM.getFF())) return "wrong_fluid";
		if (capsuleAmount() >= capsuleCapacity()) return "full";
		if (!world.isBlockPowered(pos)) return "redstone";
		if (!slot.isLocked()) return "moving";
		if (mode == Mode.WATER && water.getFill() == 0) return "water";
		if (mode == Mode.WATER && nitrogen.getFill() == 0) return "nitrogen";
		if (mode == Mode.DEUTERIUM && deuterium.getFill() == 0) return "deuterium";
		return "filling";
	}

	private boolean accepts(FluidType type) {
		return mode.accepts(type);
	}

	private EnumFacing front() {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof BlockCapsuleFiller
				? state.getValue(BlockCapsuleFiller.FACING) : EnumFacing.NORTH;
	}

	private TileEntityFuelSlot slot() {
		EnumFacing front = front();
		BlockPos target = pos.offset(front);
		if (!world.isBlockLoaded(target)) return null;
		IBlockState state = world.getBlockState(target);
		if (!(state.getBlock() instanceof BlockFuelSlot)
				|| state.getValue(BlockFuelSlot.FACING) != front.getOpposite()) return null;
		TileEntity tile = world.getTileEntity(target);
		return tile instanceof TileEntityFuelSlot ? (TileEntityFuelSlot) tile : null;
	}

	public int capsuleAmount() {
		if (world == null) return 0;
		TileEntityFuelSlot slot = slot();
		if (slot == null) return 0;
		FluidStack fluid = ItemFuelCapsule.getFluid(slot.getCapsule());
		return fluid == null ? 0 : fluid.amount;
	}

	public boolean hasCapsule() {
		if (world == null) return false;
		TileEntityFuelSlot slot = slot();
		return slot != null && !slot.getCapsule().isEmpty();
	}

	public int capsuleCapacity() {
		return ItemFuelCapsule.CAPACITY;
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		EnumFacing front = front();
		for (EnumFacing side : EnumFacing.VALUES) {
			if (side.getAxis() == front.getAxis()) continue;
			BlockPos neighbour = pos.offset(side);
			ForgeDirection direction = ForgeDirection.getOrientation(side);
			if (mode == Mode.DEUTERIUM) trySubscribe(Fluids.DEUTERIUM, world, neighbour, direction);
			if (mode != Mode.WATER) continue;
			trySubscribe(Fluids.WATER, world, neighbour, direction);
			trySubscribe(StfrFluids.LIQUID_NITROGEN, world, neighbour, direction);
			FluidType spaceNitrogen = StfrFluids.spaceNitrogen();
			if (spaceNitrogen != null) trySubscribe(spaceNitrogen, world, neighbour, direction);
		}
		TileEntityFuelSlot slot = slot();
		boolean powered = world.isBlockPowered(pos);
		if (powered && slot != null) slot.lockForFilling();
		if (powered && !poweredLastTick && slot != null && !slot.getCapsule().isEmpty())
			world.playSound(null, pos, STFRSounds.CAPSULE_FILLER,
					SoundCategory.BLOCKS, 0.7F, 2F);
		if (poweredLastTick != powered) {
			poweredLastTick = powered;
			markDirty();
		}
		int moved = 0;
		if (mode != Mode.UNSELECTED && powered && slot != null && slot.isLocked()
				&& slot.getPhase() == TileEntityFuelSlot.READY) {
			FluidStack contents = ItemFuelCapsule.getFluid(slot.getCapsule());
			int current = contents == null ? 0 : contents.amount;
			int room = ItemFuelCapsule.CAPACITY - current;
			if (room > 0) {
				if (mode == Mode.DEUTERIUM && (contents == null || contents.getFluid() == Fluids.DEUTERIUM.getFF())
						&& deuterium.getFill() > 0) {
					int amount = mode.amount(current, ItemFuelCapsule.CAPACITY,
							deuterium.getFill(), water.getFill(), nitrogen.getFill());
					moved = slot.fillCapsule(new FluidStack(Fluids.DEUTERIUM.getFF(), amount));
					deuterium.setFill(deuterium.getFill() - moved);
				} else if (mode == Mode.WATER && (contents == null || contents.getFluid() == Fluids.WATER.getFF())) {
					int amount = mode.amount(current, ItemFuelCapsule.CAPACITY,
							deuterium.getFill(), water.getFill(), nitrogen.getFill());
					if (amount > 0) {
						moved = slot.fillCapsule(new FluidStack(Fluids.WATER.getFF(), amount));
						water.setFill(water.getFill() - moved);
						nitrogen.setFill(nitrogen.getFill()
								- StfrCapsuleFilling.nitrogenCost(moved));
					}
				}
			}
		}
		if (moved > 0) markDirty();
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection direction) {
		EnumFacing side = direction.toEnumFacing();
		return world != null && side != null && side.getAxis() != front().getAxis()
				&& accepts(type);
	}

	@Override
	public boolean isLoaded() {
		return world != null && world.isBlockLoaded(pos);
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		return getReceivingTanks();
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return new FluidTankNTM[] {deuterium, water, nitrogen};
	}

	@Override
	public FluidTankNTM[] getSendingTanks() {
		return new FluidTankNTM[0];
	}

	@Override
	public long getDemand(FluidType type, int pressure) {
		return accepts(type) ? IFluidStandardTransceiverMK2.super.getDemand(canonical(type), pressure) : 0;
	}

	@Override
	public int[] getReceivingPressureRange(FluidType type) {
		return IFluidStandardTransceiverMK2.super.getReceivingPressureRange(canonical(type));
	}

	static FluidType canonical(FluidType type) {
		return StfrFluids.isLiquidNitrogen(type) ? StfrFluids.LIQUID_NITROGEN : type;
	}

	@Override
	public long getReceiverSpeed(FluidType type, int pressure) {
		return StfrFluids.isLiquidNitrogen(type) ? 100 : 1000;
	}

	@Override
	public long transferFluid(FluidType type, int pressure, long amount) {
		if (world == null || amount <= 0 || !accepts(type)) return amount;
		long offered = Math.min(amount, getReceiverSpeed(type, pressure));
		if (offered <= 0) return amount;
		long remaining = IFluidStandardTransceiverMK2.super.transferFluid(canonical(type), pressure, offered);
		int accepted = (int) (offered - remaining);
		if (accepted > 0) markDirty();
		return amount - accepted;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		deuterium.writeToNBT(tag, "deuterium");
		water.writeToNBT(tag, "water");
		nitrogen.writeToNBT(tag, "nitrogen");
		tag.setBoolean("poweredLastTick", poweredLastTick);
		tag.setString("fillMode", mode.name());
		return tag;
	}

	@Override
	public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
		world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		deuterium.readFromNBT(tag, "deuterium");
		water.readFromNBT(tag, "water");
		nitrogen.readFromNBT(tag, "nitrogen");
		// HBM stores the former tank size in NBT, so old worlds must be capped too.
		deuterium.changeTankSize(BUFFER_CAPACITY);
		water.changeTankSize(BUFFER_CAPACITY);
		nitrogen.changeTankSize(BUFFER_CAPACITY);
		poweredLastTick = tag.getBoolean("poweredLastTick");
		try { mode = Mode.valueOf(tag.getString("fillMode")); }
		catch (IllegalArgumentException ignored) { mode = Mode.UNSELECTED; }
		deuterium.setTankType(Fluids.DEUTERIUM);
		water.setTankType(Fluids.WATER);
		nitrogen.setTankType(StfrFluids.LIQUID_NITROGEN);
		deuterium.withPressure(5);
		water.withPressure(1);
		nitrogen.withPressure(5);
	}
}

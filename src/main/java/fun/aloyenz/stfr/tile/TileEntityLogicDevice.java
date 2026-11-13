package fun.aloyenz.stfr.tile;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.ForgeDirection;
import java.util.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.api.data.IDataMachine;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMainframe;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrMainframePower;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.logic.StfrMonitorText;
import fun.aloyenz.stfr.logic.StfrSignal;
import fun.aloyenz.stfr.logic.StfrSignalRepeater;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;

public final class TileEntityLogicDevice extends TileEntity
		implements ITickable, IDataMachine, IEnergyReceiverMK2, IEnergyProviderMK2 {
	long mainframeEnergy;
	long mainframePowerTick = Long.MIN_VALUE;
	boolean mainframePowered;

	public long mainframeStoredEnergy() {
		return mainframeEnergy;
	}

	public void setMainframeStoredEnergy(long value) {
		value = Math.max(0, Math.min(StfrMainframePower.BUFFER_PER_BLOCK, value));
		if (mainframeEnergy != value) {
			mainframeEnergy = value;
			markDirty();
		}
	}

	public long mainframePowerTick() {
		return mainframePowerTick;
	}

	public void setMainframePowerState(long tick, boolean powered) {
		if (mainframePowered != powered) StfrLogicNetwork.invalidate(world);
		mainframePowerTick = tick;
		mainframePowered = powered;
	}

	public boolean creativeMainframe() {
		return world != null
				&& world.getBlockState(pos).getBlock() instanceof BlockLogicMainframe
				&& ((BlockLogicMainframe) world.getBlockState(pos).getBlock()).creative();
	}

	public int mainframeSlotCapacity() {
		return StfrMainframePower.slots(creativeMainframe());
	}

	public int mainframeEnergyCost() {
		return StfrMainframePower.energyCost(creativeMainframe());
	}

	public boolean mainframeOnline() {
		if (kind() != BlockLogicDevice.Kind.MAINFRAME) return false;
		StfrMainframePower.tick(this);
		return (creativeMainframe() || mainframePowered) && crashTicks == 0;
	}

	@Override
	public boolean canConnect(ForgeDirection side) {
		return kind() == BlockLogicDevice.Kind.MAINFRAME
				&& (side == ForgeDirection.UP || side == ForgeDirection.DOWN);
	}

	@Override
	public boolean isLoaded() {
		return world != null && !isInvalid() && world.isBlockLoaded(pos);
	}

	@Override
	public long getPower() {
		return kind() == BlockLogicDevice.Kind.MAINFRAME ? StfrMainframePower.stored(this) : 0;
	}

	@Override
	public long getMaxPower() {
		return kind() == BlockLogicDevice.Kind.MAINFRAME ? StfrMainframePower.capacity(this) : 0;
	}

	@Override
	public void setPower(long amount) {
		if (kind() == BlockLogicDevice.Kind.MAINFRAME) StfrMainframePower.setStored(this, amount);
	}

	@Override
	public long getProviderSpeed() {
		return getMaxPower();
	}

	@Override
	public void usePower(long amount) {
		setPower(getPower() - Math.max(0, amount));
	}

	final StfrSignal[] signals = new StfrSignal[6];
	final boolean[] applied = new boolean[6];
	final int[] repeatIntervals = new int[6];
	final StfrSignalRepeater repeater = new StfrSignalRepeater();

	public int repeatInterval(EnumFacing face) {
		return repeatIntervals[face.getIndex()];
	}

	public void setRepeatInterval(EnumFacing face, int ticks) {
		repeatIntervals[face.getIndex()] = Math.max(0, ticks);
		repeater.reset(face.getIndex());
		markDirty();
		if (world != null)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	final String[] monitorLines = {"", "", "", ""};
	final String[] templateLines = StfrMonitorText.DEFAULT.clone();
	final int[] monitorColors = {0xF4F4F4, 0xF4F4F4, 0xF4F4F4, 0xF4F4F4};
	boolean monitorConnected;
	boolean monitorUnstable;
	int catStage;
	public int catStage() { return catStage; }
	public void setCatOverlay(int stage) {
		stage = Math.max(0, Math.min(3, stage));
		if (catStage == stage) return;
		catStage = stage;
		markDirty();
		if (world != null && !world.isRemote)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}
	StfrMonitorDisplay monitorDisplay = StfrMonitorDisplay.TEXT;
	NBTTagCompound monitorGraphics = new NBTTagCompound();

	public StfrMonitorDisplay monitorDisplay() {
		return monitorDisplay;
	}

	public NBTTagCompound monitorGraphics() {
		return monitorGraphics;
	}

	public void setMonitorDisplay(StfrMonitorDisplay mode) {
		if (kind() != BlockLogicDevice.Kind.MONITOR || monitorDisplay == mode) return;
		monitorDisplay = mode;
		monitorGraphics = new NBTTagCompound();
		markDirty();
		if (world != null)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	public boolean monitorUnstable() {
		return monitorUnstable;
	}

	boolean inlineMode;
	int crashTicks;
	int suppressionMask = StfrFailureEffects.ALL;
	long autopilotFuelThreshold = 6000L;
	boolean autopilotForceShutdown;

	public long autopilotFuelThreshold() {
		return autopilotFuelThreshold;
	}

	public void setAutopilotFuelThreshold(long value) {
		autopilotFuelThreshold = Math.max(0L, Math.min(1000000000L, value));
		markDirty();
		if (world != null)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	public boolean autopilotForceShutdown() {
		return autopilotForceShutdown;
	}

	public void setAutopilotForceShutdown(boolean value) {
		autopilotForceShutdown = value;
		markDirty();
		if (world != null)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	public int crashTicks() {
		return crashTicks;
	}

	BlockPos crashOrigin;

	public BlockPos crashOrigin() { return crashOrigin == null ? pos : crashOrigin; }

	public void crash(int ticks, BlockPos origin) {
		crashOrigin = origin;
		crashTicks = Math.max(crashTicks, ticks);
		StfrLogicNetwork.invalidate(world);
		markDirty();
	}

	public int suppressionMask() {
		return suppressionMask;
	}

	public void setSuppressionMask(int mask) {
		suppressionMask = mask & StfrFailureEffects.ALL;
		markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	public static EnumFacing monitorSlotFace(int slot) {
		return EnumFacing.HORIZONTALS[slot];
	}

	public boolean inlineMode() {
		return inlineMode;
	}

	public void setInlineMode(boolean value) {
		if (inlineMode == value) return;
		inlineMode = value;
		markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	public boolean monitorConnected() {
		return monitorConnected;
	}

	public String monitorLine(int index) {
		return monitorLines[index];
	}

	public String templateLine(int index) {
		return templateLines[index];
	}

	public boolean setMonitorText(String[] lines) {
		if (kind() != BlockLogicDevice.Kind.MONITOR || lines == null || lines.length != 4)
			return false;
		for (String line : lines) if (!StfrMonitorText.valid(line)) return false;
		System.arraycopy(lines, 0, templateLines, 0, 4);
		markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
		return true;
	}

	public int monitorColor(int index) {
		return monitorColors[index];
	}

	public TileEntityLogicDevice() {
		Arrays.fill(signals, StfrSignal.NONE);
	}

	@Override
	public boolean acceptsDataCable() {
		return kind() != BlockLogicDevice.Kind.MONITOR;
	}

	public BlockLogicDevice.Kind kind() {
		IBlockState state = world == null ? null : world.getBlockState(pos);
		return state != null && state.getBlock() instanceof BlockLogicDevice
				? ((BlockLogicDevice) state.getBlock()).kind()
				: BlockLogicDevice.Kind.RECEIVER;
	}

	public boolean breakerClosed() {
		IBlockState state = world.getBlockState(pos);
		return kind() == BlockLogicDevice.Kind.BREAKER && state.getValue(BlockLogicDevice.CLOSED);
	}

	public StfrSignal signal(EnumFacing face) {
		return signals[face.getIndex()];
	}

	public void setSignal(EnumFacing face, StfrSignal signal) {
		if (world != null
				&& !world.isRemote
				&& applied[face.getIndex()]
				&& signals[face.getIndex()].isCommand()) {
			StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
			if (network.reactor != null) {
				signals[face.getIndex()].clear(network.reactor);
				StfrManager.get(world).markDirty();
			}
		}
		applied[face.getIndex()] = false;
		repeater.reset(face.getIndex());
		signals[face.getIndex()] = signal;
		markDirty();
		if (world != null)
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	@Override
	public void update() {
		StfrLogicDeviceTick.update(this);
	}

	void setMonitorDisplay(String[] lines, int[] colors, boolean connected) {
		if (connected == monitorConnected
				&& Arrays.equals(lines, monitorLines)
				&& Arrays.equals(colors, monitorColors)) return;
		monitorConnected = connected;
		System.arraycopy(lines, 0, monitorLines, 0, 4);
		System.arraycopy(colors, 0, monitorColors, 0, 4);
		markDirty();
		world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
	}

	static NBTTagCompound graphicsSnapshot(StfrReactor reactor) {
		return StfrMonitorSnapshot.graphicsSnapshot(reactor);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		// TileEntity's default writes only the base id/position/capabilities.
		// The sign needs its resolved lines and connection state on chunk load.
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

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		return StfrLogicDevicePersistence.writeToNBT(this, tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		StfrLogicDevicePersistence.readFromNBT(this, tag);
	}
}

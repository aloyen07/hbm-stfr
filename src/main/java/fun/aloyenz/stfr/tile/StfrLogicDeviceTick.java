package fun.aloyenz.stfr.tile;

import com.hbm.lib.ForgeDirection;
import net.minecraft.block.BlockButton;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMonitor;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrMainframePower;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.logic.StfrMonitorText;
import fun.aloyenz.stfr.logic.StfrSignal;
import fun.aloyenz.stfr.meme.StfrCatEvents;
import fun.aloyenz.stfr.reactor.StfrAutopilot;
import fun.aloyenz.stfr.reactor.StfrDestabilizer;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.security.StfrDetonatorProgramming;
import fun.aloyenz.stfr.sound.StfrMainframeCrashSound;

/** Network-device tick dispatch, redstone signals and monitor refresh. */
final class StfrLogicDeviceTick {
	private StfrLogicDeviceTick() {}

	static void update(TileEntityLogicDevice c) {
		if (c.getWorld() == null || c.getWorld().isRemote) return;
		BlockLogicDevice.Kind kind = c.kind();
		if (kind == BlockLogicDevice.Kind.MONITOR) StfrCatEvents.updateMonitor(c);
		if (kind == BlockLogicDevice.Kind.MAINFRAME) {
			for (EnumFacing side : new EnumFacing[] {EnumFacing.UP, EnumFacing.DOWN}) {
				TileEntity neighbour = c.getWorld().getTileEntity(c.getPos().offset(side));
				if (neighbour instanceof TileEntityLogicDevice
						&& ((TileEntityLogicDevice) neighbour).kind()
								== BlockLogicDevice.Kind.MAINFRAME) continue;
				ForgeDirection direction = ForgeDirection.getOrientation(side);
				c.trySubscribe(c.getWorld(), c.getPos().offset(side), direction);
			}
			StfrMainframePower.tick(c);
			for (EnumFacing side : new EnumFacing[] {EnumFacing.UP, EnumFacing.DOWN}) {
				TileEntity neighbour = c.getWorld().getTileEntity(c.getPos().offset(side));
				if (neighbour instanceof TileEntityLogicDevice
						&& ((TileEntityLogicDevice) neighbour).kind()
								== BlockLogicDevice.Kind.MAINFRAME) continue;
				c.tryProvide(c.getWorld(), c.getPos().offset(side), ForgeDirection.getOrientation(side));
			}
		}
		if (kind == BlockLogicDevice.Kind.AUTOPILOT_1
				|| kind == BlockLogicDevice.Kind.AUTOPILOT_2
				|| kind == BlockLogicDevice.Kind.AUTOPILOT_3) StfrAutopilot.tick(c.getWorld(), c);
		if (kind == BlockLogicDevice.Kind.DESTABILIZER && c.getWorld().getTotalWorldTime() % 10 == 0)
			StfrDestabilizer.keepAlive(c.getWorld(), c);
		if (kind == BlockLogicDevice.Kind.DETONATOR_CONNECTOR) StfrDetonatorProgramming.tick(c);
		if (kind == BlockLogicDevice.Kind.MAINFRAME && c.crashTicks > 0) {
			if (c.crashTicks == 1300 || c.crashTicks == 1160 || c.crashTicks == 1140)
				StfrMainframeCrashSound.play(c.getWorld(), c, c.crashTicks);
			c.crashTicks--;
			c.markDirty();
		}
		if (kind == BlockLogicDevice.Kind.MAINFRAME && c.crashTicks > 0) return;
		if (kind == BlockLogicDevice.Kind.TRANSMITTER) {
			StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(c.getWorld(), c.getPos());
			if (!network.ioAvailable)
				for (EnumFacing face : EnumFacing.VALUES) {
					if (c.applied[face.getIndex()]
							&& network.reactor != null
							&& c.signals[face.getIndex()].isCommand()) {
						c.signals[face.getIndex()].clear(network.reactor);
						StfrManager.get(c.getWorld()).markDirty();
					}
					c.applied[face.getIndex()] = false;
					c.repeater.reset(face.getIndex());
				}
			for (EnumFacing face : EnumFacing.VALUES) {
				StfrSignal signal = c.signals[face.getIndex()];
				boolean now = c.getWorld().getRedstonePower(c.getPos().offset(face), face.getOpposite()) > 0;
				boolean button =
						c.getWorld().getBlockState(c.getPos().offset(face)).getBlock() instanceof BlockButton;
				boolean repeat =
						c.repeater.tick(
								face.getIndex(),
								signal.isCommand() && now && c.applied[face.getIndex()] && !button,
								c.repeatIntervals[face.getIndex()]);
				if (signal.isCommand() && (now != c.applied[face.getIndex()] || repeat)) {
					if (network == null) network = StfrLogicNetwork.inspect(c.getWorld(), c.getPos());
					if (network.ioAvailable) {
						if (now && signal.send(network.reactor, c.getWorld()))
							StfrManager.get(c.getWorld()).markDirty();
						else if (!now) {
							signal.clear(network.reactor);
							StfrManager.get(c.getWorld()).markDirty();
						}
						c.applied[face.getIndex()] = now;
					}
				}
				// A transmitter treats a directly attached button as a one-tick pulse,
				// even when c face has no command or its cable is disconnected.
				if (now) {
					IBlockState input = c.getWorld().getBlockState(c.getPos().offset(face));
					if (input.getBlock() instanceof BlockButton
							&& input.getValue(BlockButton.POWERED))
						c.getWorld().setBlockState(
								c.getPos().offset(face),
								input.withProperty(BlockButton.POWERED, false),
								3);
				}
			}
		} else if (kind == BlockLogicDevice.Kind.MAINFRAME && c.getWorld().getTotalWorldTime() % 20 == 0) {
			StfrLogicNetwork.Snapshot snapshot = StfrLogicNetwork.inspect(c.getWorld(), c.getPos());
			if (snapshot.reactor != null) {
				if (snapshot.reactor.runState() == StfrReactor.RunState.OFFLINE) {
					StfrLogicNetwork.openBreakers(c.getWorld(), snapshot.reactor);
					snapshot = StfrLogicNetwork.inspect(c.getWorld(), c.getPos());
				}
				boolean connected = snapshot.ioAvailable && snapshot.breakers >= 15;
				if (snapshot.reactor.breakersOn() != connected) {
					snapshot.reactor.setBreakersOn(connected);
					StfrManager.get(c.getWorld()).markDirty();
				}
			}
		} else if (kind == BlockLogicDevice.Kind.MONITOR) {
			TileEntity receiver = c.getWorld().getTileEntity(c.getPos().down());
			if (!(receiver instanceof TileEntityLogicDevice)
					|| ((TileEntityLogicDevice) receiver).kind()
							!= BlockLogicDevice.Kind.RECEIVER) {
				IBlockState state = c.getWorld().getBlockState(c.getPos());
				if (state.getBlock() instanceof BlockLogicMonitor
						&& state.getValue(BlockLogicMonitor.WALL))
					receiver =
							c.getWorld().getTileEntity(
									c.getPos().offset(
											state.getValue(BlockLogicMonitor.FACING)
													.getOpposite()));
			}
			boolean attachedReceiver =
					receiver instanceof TileEntityLogicDevice
							&& ((TileEntityLogicDevice) receiver).kind()
									== BlockLogicDevice.Kind.RECEIVER;
			StfrLogicNetwork.Snapshot snapshot = StfrLogicNetwork.inspect(c.getWorld(), c.getPos());
			if (attachedReceiver) snapshot = StfrLogicNetwork.inspect(c.getWorld(), receiver.getPos());
			boolean protocol =
					snapshot.reactor != null
							&& (snapshot.reactor.emergency().selfPending()
									|| snapshot.reactor.runState()
											== StfrReactor.RunState.SELF_DESTRUCT);
			boolean booting =
					protocol
							&& !snapshot.reactor.emergency().selfPending()
							&& snapshot.reactor.stateTicks() < 200;
			boolean ready = !booting && (snapshot.ioAvailable);
			boolean unstable = snapshot.reactor != null && snapshot.reactor.unstable();
			if (c.monitorUnstable != unstable) {
				c.monitorUnstable = unstable;
				c.markDirty();
				c.getWorld().notifyBlockUpdate(c.getPos(), c.getWorld().getBlockState(c.getPos()), c.getWorld().getBlockState(c.getPos()), 2);
			}
			if (ready
					&& snapshot.reactor.runState() == StfrReactor.RunState.OFFLINE
					&& c.getWorld().getTotalWorldTime() % 10 != 0) return;
			if (c.monitorDisplay != StfrMonitorDisplay.TEXT
					|| (ready
							&& (snapshot.reactor.emergency().selfPending()
									|| snapshot.reactor.runState()
											== StfrReactor.RunState.SELF_DESTRUCT))) {
				if (ready
						&& (!c.monitorConnected
								|| !c.monitorGraphics.hasKey("state")
								|| c.getWorld().getTotalWorldTime() % 5 == 0)) {
					NBTTagCompound graphics = c.graphicsSnapshot(snapshot.reactor);
					graphics.setLong("snapshotTime", c.getWorld().getTotalWorldTime());
					if (!graphics.equals(c.monitorGraphics)) {
						c.monitorGraphics = graphics;
						c.markDirty();
						c.getWorld().notifyBlockUpdate(
								c.getPos(), c.getWorld().getBlockState(c.getPos()), c.getWorld().getBlockState(c.getPos()), 2);
					}
				}
				c.setMonitorDisplay(
						new String[] {"", "", "", ""},
						new int[] {0xF4F4F4, 0xF4F4F4, 0xF4F4F4, 0xF4F4F4},
						ready);
				return;
			}
			if (c.monitorGraphics.getBoolean("selfPending")
					|| "SELF_DESTRUCT".equals(c.monitorGraphics.getString("state"))) {
				c.monitorGraphics = new NBTTagCompound();
				c.markDirty();
				c.getWorld().notifyBlockUpdate(c.getPos(), c.getWorld().getBlockState(c.getPos()), c.getWorld().getBlockState(c.getPos()), 2);
			}
			String[] lines = {"", "", "", ""};
			int[] colors = {0xF4F4F4, 0xF4F4F4, 0xF4F4F4, 0xF4F4F4};
			if (ready)
				for (int i = 0; i < 4; i++) {
					lines[i] = StfrMonitorText.expand(c.templateLines[i], snapshot.reactor);
					colors[i] = StfrMonitorText.alarmColor(c.templateLines[i], snapshot.reactor);
				}
			c.setMonitorDisplay(lines, colors, ready);
		}
	}
}

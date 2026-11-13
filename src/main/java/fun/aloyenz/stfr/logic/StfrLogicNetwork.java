package fun.aloyenz.stfr.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.api.data.IDataMachine;
import fun.aloyenz.stfr.block.BlockDataCable;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMainframe;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.security.StfrSecurityData;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;

/** Resolves a single reactor through the data-cable network and its mainframe. */
public final class StfrLogicNetwork {

	private StfrLogicNetwork() {}

	private static final Map<World, TickCache> CACHE = new WeakHashMap<>();

	private static final class TickCache {

		long tick = Long.MIN_VALUE;
		final Map<BlockPos, Snapshot> secure = new HashMap<>(), raw = new HashMap<>();
	}

	public static void invalidate(World world) {
		if (world != null) CACHE.remove(world);
	}

	private static Snapshot cachedInspect(World world, BlockPos pos, boolean secure) {
		TickCache cache = CACHE.computeIfAbsent(world, ignored -> new TickCache());
		long tick = world.getTotalWorldTime();
		if (cache.tick != tick) {
			cache.tick = tick;
			cache.secure.clear();
			cache.raw.clear();
		}
		Map<BlockPos, Snapshot> entries = secure ? cache.secure : cache.raw;
		Snapshot result = entries.get(pos);
		if (result == null) {
			result = inspectCableComponent(world, pos, secure);
			entries.put(pos.toImmutable(), result);
		}
		return result;
	}

	public static final class Snapshot {

		public final int mainframes;
		public final int breakers;
		public final int ports;
		public final StfrReactor reactor;
		public final String issue;
		public final boolean ioAvailable;
		public UUID networkId;
		public BlockPos mainframePos;
		public boolean routingAvailable;
		public int activeMainframes,
				slotCapacity,
				slotDemand,
				slotsUsed,
				installedSlotCapacity,
				powerRequired;
		public Set<BlockPos> enabledDevices = Collections.emptySet();

		public boolean deviceAvailable(BlockPos pos) {
			return enabledDevices.contains(pos);
		}

		public Set<BlockPos> devices = Collections.emptySet();
		public Map<BlockPos, String> unavailableDevices = Collections.emptyMap();

		Snapshot(
				int mainframes,
				int breakers,
				int ports,
				StfrReactor reactor,
				String issue,
				boolean ioAvailable) {
			this.mainframes = mainframes;
			this.breakers = breakers;
			this.ports = ports;
			this.reactor = reactor;
			this.issue = issue;
			this.ioAvailable = ioAvailable && mainframes > 0 && reactor != null;
		}

		public boolean ready() {
			return ioAvailable && breakers >= 15;
		}
	}

	public static Snapshot inspect(World world, BlockPos pos) {
		// The physical cable component is authoritative. Uninos maintains a
		// short-lived endpoint cache for API consumers, which can still report
		// a severed link or omit a newly connected receiver for several ticks.
		return cachedInspect(world, pos, true);
	}

	/** Discovery for enrollment only; callers must never use this to send reactor commands. */
	public static Snapshot inspectRaw(World world, BlockPos pos) {
		return cachedInspect(world, pos, false);
	}

	/**
	 * Shutdown disconnects only the physical breakers belonging to this reactor's cable component.
	 */
	public static int openBreakers(World world, StfrReactor reactor) {
		List<BlockPos> positions = new ArrayList<>();
		for (TileEntity tile : world.loadedTileEntityList) {
			if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
			TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
			if (device.kind() == BlockLogicDevice.Kind.BREAKER
					&& device.breakerClosed()
					&& inspect(world, tile.getPos()).reactor == reactor)
				positions.add(tile.getPos());
		}
		for (BlockPos pos : positions) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() == STFRBlocks.LOGIC_BREAKER)
				world.setBlockState(pos, state.withProperty(BlockLogicDevice.CLOSED, false), 3);
		}
		return positions.size();
	}

	public static int slotCost(BlockLogicDevice.Kind kind) {
		switch (kind) {
			case TRANSMITTER:
			case RECEIVER:
			case DESTABILIZER:
			case CRASH_SUPPRESSOR:
				return StfrConfig.signalDeviceSlots;
			case ALARM:
				return StfrConfig.alarmSlots;
			case DETONATOR_CONNECTOR:
				return StfrConfig.detonatorSlots;
			case AUTOPILOT_1:
			case AUTOPILOT_2:
			case AUTOPILOT_3:
				return StfrConfig.autopilotSlots;
			default:
				return 0;
		}
	}

	private static int retention(BlockLogicDevice.Kind kind) {
		switch (kind) {
			case DETONATOR_CONNECTOR:
				return 0;
			case RECEIVER:
				return 1;
			case AUTOPILOT_1:
			case AUTOPILOT_2:
			case AUTOPILOT_3:
				return 3;
			default:
				return 2;
		}
	}

	private static Snapshot inspectMachines(
			World world,
			Iterable<IDataMachine> machines,
			boolean hasCable,
			boolean secure,
			BlockPos start) {
		List<TileEntityLogicDevice> frames = new ArrayList<>();
		for (IDataMachine machine : machines)
			if (machine instanceof TileEntityLogicDevice
					&& ((TileEntityLogicDevice) machine).kind() == BlockLogicDevice.Kind.MAINFRAME)
				frames.add((TileEntityLogicDevice) machine);
		frames.sort(Comparator.comparingLong(frame -> frame.getPos().toLong()));
		UUID networkId = null;
		TileEntityLogicDevice identityFrame = null;
		boolean identityConflict = false;
		// Prefer an established access policy. New rack members inherit its identity;
		// two independently configured protected networks cannot silently merge.
		Set<UUID> configured = new HashSet<>();
		if (!world.isRemote)
			for (TileEntityLogicDevice frame : frames) {
				if (!frame.getTileData().hasUniqueId("stfrNetworkIdentity")) continue;
				UUID id = frame.getTileData().getUniqueId("stfrNetworkIdentity");
				if (StfrSecurityData.get(world).policy(id).initialized()) {
					configured.add(id);
					if (identityFrame == null) identityFrame = frame;
				}
			}
		identityConflict = configured.size() > 1;
		if (identityFrame == null)
			for (TileEntityLogicDevice frame : frames)
				if (frame.getTileData().hasUniqueId("stfrNetworkIdentity")) {
					identityFrame = frame;
					break;
				}
		if (identityFrame == null && !frames.isEmpty()) identityFrame = frames.get(0);
		if (identityFrame != null && !identityConflict) {
			networkId = StfrSecurity.networkId(identityFrame);
			if (networkId != null && !world.isRemote)
				for (TileEntityLogicDevice frame : frames) {
					if (!networkId.equals(frame.getTileData().getUniqueId("stfrNetworkIdentity"))) {
						frame.getTileData().setUniqueId("stfrNetworkIdentity", networkId);
						frame.markDirty();
					}
					if (!StfrSecurity.enrolled(frame, networkId))
						StfrSecurity.enroll(frame, networkId);
				}
		}
		StfrAccessPolicy policy =
				networkId == null || world.isRemote
						? null
						: StfrSecurityData.get(world).policy(networkId);
		boolean protectedNetwork = policy != null && policy.enabled();
		int activeFrames = 0,
				capacity = 0,
				installedCapacity = 0,
				powerRequired = 0,
				breakers = 0,
				ports = 0;
		BlockPos mainframePos = identityFrame == null ? null : identityFrame.getPos();
		for (TileEntityLogicDevice frame : frames) {
			installedCapacity += frame.mainframeSlotCapacity();
			powerRequired += frame.mainframeEnergyCost();
			if (frame.mainframeOnline()) {
				capacity += frame.mainframeSlotCapacity();
				if (activeFrames++ == 0) mainframePos = frame.getPos();
			}
		}
		Set<BlockPos> allDevices = new HashSet<>();
		Map<BlockPos, String> unavailable = new LinkedHashMap<>();
		List<StfrNetworkCapacity.Device> requests = new ArrayList<>();
		StfrReactor found = null;
		boolean conflicting = false;
		Set<Long> portIds = new HashSet<>();
		for (IDataMachine machine : machines) {
			if (!(machine instanceof TileEntity)) continue;
			TileEntity tile = (TileEntity) machine;
			if (tile.isInvalid() || tile.getWorld() != world) continue;
			if (tile instanceof TileEntityLogicDevice) {
				TileEntityLogicDevice logic = (TileEntityLogicDevice) tile;
				if (protectedNetwork && !StfrSecurity.enrolled(tile, networkId)) {
					unavailable.put(tile.getPos().toImmutable(), "not_enrolled");
					continue;
				}
				allDevices.add(tile.getPos());
				if (logic.kind() == BlockLogicDevice.Kind.BREAKER && logic.breakerClosed())
					breakers++;
				if (logic.kind() != BlockLogicDevice.Kind.MAINFRAME)
					requests.add(
							new StfrNetworkCapacity.Device(
									tile.getPos().toLong(),
									slotCost(logic.kind()),
									retention(logic.kind())));
			} else if (tile instanceof TileEntityMultiFluidPort) {
				ports++;
				portIds.add(tile.getPos().toLong());
				requests.add(
						new StfrNetworkCapacity.Device(
								tile.getPos().toLong(), StfrConfig.portSlots, 4));
				StfrReactor reactor = StfrManager.get(world).forPort(tile.getPos());
				if (reactor != null) {
					if (found != null && found != reactor) conflicting = true;
					found = reactor;
				}
			}
		}
		// The ports access one reactor-wide store. Only one port needs a data
		// cable, but every installed port still occupies its own two slots.
		if (found != null && !conflicting)
			for (BlockPos port : reactorPorts(world, found))
				if (portIds.add(port.toLong())) {
					ports++;
					requests.add(
							new StfrNetworkCapacity.Device(port.toLong(), StfrConfig.portSlots, 4));
				}
		StfrNetworkCapacity.Allocation allocation =
				StfrNetworkCapacity.allocate(capacity, requests);
		Set<BlockPos> enabled = new HashSet<>();
		for (long key : allocation.enabled) enabled.add(BlockPos.fromLong(key));
		for (TileEntityLogicDevice frame : frames)
			if (frame.mainframeOnline()) enabled.add(frame.getPos());
		for (TileEntityLogicDevice frame : frames)
			if (!frame.mainframeOnline())
				unavailable.put(frame.getPos().toImmutable(), "mainframe_power");
		int operatingPorts = 0;
		for (long key : portIds) {
			BlockPos port = BlockPos.fromLong(key);
			if (found != null && !found.portOperational(world, port)) {
				enabled.remove(port);
				unavailable.put(port.toImmutable(), "port_damaged");
			}
			if (found != null && enabled.contains(port)) operatingPorts++;
		}
		for (StfrNetworkCapacity.Device request : requests) {
			BlockPos device = BlockPos.fromLong(request.id);
			if (!enabled.contains(device) && !unavailable.containsKey(device))
				unavailable.put(device.toImmutable(), "no_slot");
		}
		boolean startAvailable = enabled.contains(start);
		if (!(world.getTileEntity(start) instanceof IDataMachine))
			startAvailable = activeFrames > 0;
		String issue =
				!hasCable
						? "NO CABLE NETWORK"
						: frames.isEmpty()
								? "NO MAINFRAME"
								: identityConflict
										? "CONFLICTING NETWORK IDENTITIES"
										: conflicting
												? "MULTIPLE REACTORS"
												: activeFrames == 0
														? "MAINFRAME NEEDS POWER"
														: found == null
																? "NO REACTOR PORT"
																: operatingPorts == 0
																		? "NO OPERATIONAL PORT"
																		: !startAvailable
																				? "DEVICE"
																					  + " DISCONNECTED:"
																					  + " CAPACITY"
																				: allocation.demand
																								> allocation
																										.capacity
																						? "NETWORK"
																							  + " OVER"
																							  + " CAPACITY"
																						: breakers
																										< 15
																								? "NEED 15"
																									  + " CLOSED"
																									  + " BREAKERS"
																								: "READY";
		Snapshot result =
				new Snapshot(
						frames.size(),
						breakers,
						ports,
						conflicting ? null : found,
						issue,
						!identityConflict
								&& operatingPorts > 0
								&& activeFrames > 0
								&& startAvailable);
		result.routingAvailable =
				!identityConflict
						&& !conflicting
						&& found != null
						&& operatingPorts > 0
						&& activeFrames > 0;
		result.networkId = networkId;
		result.mainframePos = mainframePos;
		result.activeMainframes = activeFrames;
		result.slotCapacity = allocation.capacity;
		result.slotDemand = allocation.demand;
		result.slotsUsed = allocation.used;
		result.installedSlotCapacity = installedCapacity;
		result.powerRequired = powerRequired;
		result.enabledDevices = Collections.unmodifiableSet(enabled);
		result.unavailableDevices = Collections.unmodifiableMap(unavailable);
		if (secure) allDevices.retainAll(enabled);
		result.devices = Collections.unmodifiableSet(allDevices);
		return result;
	}

	private static List<BlockPos> reactorPorts(World world, StfrReactor reactor) {
		List<BlockPos> ports = new ArrayList<>();
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			BlockPos base = origin.add(cell.localPos);
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			for (BlockPos candidate : new BlockPos[] {base, base.offset(direction)})
				if (world.isBlockLoaded(candidate)
						&& world.getTileEntity(candidate) instanceof TileEntityMultiFluidPort)
					ports.add(candidate);
		}
		return ports;
	}

	/** Physical port redundancy, independent of the shared reactor inventory allocation. */
	public static int connectedStabilizers(World world,StfrReactor reactor) {
		int mask=0;BlockPos mainframe=null;UUID identity=null;
		BlockPos origin=reactor.center.subtract(StfrStructureTemplate.CENTER);
		for(BlockPos port:reactorPorts(world,reactor)) {
			Snapshot net=inspect(world,port);
			if(net.reactor!=reactor || !net.routingAvailable || net.mainframePos==null)continue;
			if(mainframe==null) {mainframe=net.mainframePos;identity=net.networkId;}
			if(!mainframe.equals(net.mainframePos) && (identity==null || !identity.equals(net.networkId)))continue;
			EnumFacing side=StfrRadialSymmetry.sourceDirection(port.subtract(origin));
			if(side!=null)mask|=1<<side.ordinal();
		}
		return Integer.bitCount(mask);
	}

	/** Reactor fluids are shared by every intact, allocated port. */
	public static boolean portAccess(World world, StfrReactor reactor, BlockPos port) {
		if (!reactor.portOperational(world, port)) return false;
		for (BlockPos connected : reactorPorts(world, reactor)) {
			Snapshot snapshot = inspect(world, connected);
			if (snapshot.reactor == reactor
					&& snapshot.ioAvailable
					&& snapshot.deviceAvailable(port)) return true;
		}
		return false;
	}

	/** Cables and vertical Mainframe racks form the bus; other devices remain endpoints. */
	private static Snapshot inspectCableComponent(World world, BlockPos start, boolean secure) {
		if (world.getTileEntity(start) instanceof IDataMachine machine && !machine.acceptsDataCable())
			return inspectMachines(world, Collections.emptySet(), false, secure, start);
		Set<BlockPos> visitedCables = new HashSet<>(),
				visitedFrames = new HashSet<>(),
				endpoints = new HashSet<>();
		ArrayDeque<BlockPos> pending = new ArrayDeque<>();
		collectEndpoint(world, start, endpoints);
		enqueueBus(world, start, pending, visitedCables, visitedFrames);
		for (EnumFacing side : EnumFacing.VALUES) {
			BlockPos neighbour = start.offset(side);
			if (world.isBlockLoaded(start)
					&& world.getBlockState(start).getBlock() instanceof BlockLogicMainframe
					&& world.isBlockLoaded(neighbour)
					&& world.getBlockState(neighbour).getBlock() instanceof BlockLogicMainframe
					&& !side.getAxis().isVertical()) continue;
			enqueueBus(world, neighbour, pending, visitedCables, visitedFrames);
		}
		while (!pending.isEmpty()) {
			BlockPos node = pending.removeFirst();
			for (EnumFacing side : EnumFacing.VALUES) {
				BlockPos neighbour = node.offset(side);
				if (!world.isBlockLoaded(neighbour)) continue;
				boolean frame =
						world.getBlockState(neighbour).getBlock() instanceof BlockLogicMainframe;
				// Adjacent frames join directly only along the stacking axis.
				if (frame && visitedFrames.contains(node) && !side.getAxis().isVertical()) continue;
				enqueueBus(world, neighbour, pending, visitedCables, visitedFrames);
				collectEndpoint(world, neighbour, endpoints);
			}
		}
		endpoints.addAll(visitedFrames);
		Set<IDataMachine> machines = new HashSet<>();
		for (BlockPos endpoint : endpoints) {
			TileEntity tile = world.getTileEntity(endpoint);
			if (tile instanceof IDataMachine) machines.add((IDataMachine) tile);
		}
		return inspectMachines(
				world,
				machines,
				!visitedCables.isEmpty() || !visitedFrames.isEmpty(),
				secure,
				start);
	}

	private static void enqueueBus(
			World world,
			BlockPos pos,
			ArrayDeque<BlockPos> pending,
			Set<BlockPos> cables,
			Set<BlockPos> frames) {
		if (!world.isBlockLoaded(pos)) return;
		if (BlockDataCable.isDataCable(world.getBlockState(pos).getBlock())) {
			if (cables.add(pos)) pending.addLast(pos);
		} else if (world.getBlockState(pos).getBlock() instanceof BlockLogicMainframe
				&& frames.add(pos)) pending.addLast(pos);
	}

	private static void collectEndpoint(World world, BlockPos pos, Set<BlockPos> endpoints) {
		if (!world.isBlockLoaded(pos)) return;
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof IDataMachine machine && machine.acceptsDataCable() && !tile.isInvalid()) endpoints.add(pos);
	}
}

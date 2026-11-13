package fun.aloyenz.stfr.reactor;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrVoicePacket;
import fun.aloyenz.stfr.sound.StfrFailureSounds;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/**
 * Network controller for the three physical autopilot blocks. It only turns the public reactor
 * controls; the simulation remains the sole source of heat, shielding, rotation and fuel behaviour.
 */
public final class StfrAutopilot {

	private StfrAutopilot() {}

	static final Map<StfrReactor, State> STATES = new IdentityHashMap<>();
	static final Logger LOG =
			LogManager.getLogger("HBM-STFR");

	interface Controls {

		StfrCoreSimulation core();

		boolean digammaActive();

		double autopilotTelemetry(double value, int salt);

		int fuelInputLevel();

		int suppression();

		int offset();

		int lasers();

		int vent();

		int permeability();

		BlockPos center();

		void autopilotAdjust(
				int fuel, int suppression, int offset, int lasers, int vent, int permeability);
	}

	static final class LiveControls implements Controls {

		private final StfrReactor reactor;

		LiveControls(StfrReactor reactor) {
			this.reactor = reactor;
		}

		public StfrCoreSimulation core() {
			return reactor.core();
		}

		public boolean digammaActive() {
			return reactor.digammaActive();
		}

		public double autopilotTelemetry(double value, int salt) {
			return reactor.autopilotTelemetry(value, salt);
		}

		public int fuelInputLevel() {
			return reactor.fuelInputLevel();
		}

		public int suppression() {
			return reactor.suppression();
		}

		public int offset() {
			return reactor.offset();
		}

		public int lasers() {
			return reactor.lasers();
		}

		public int vent() {
			return reactor.vent();
		}

		public int permeability() {
			return reactor.permeability();
		}

		public BlockPos center() {
			return reactor.center;
		}

		public void autopilotAdjust(
				int fuel, int suppression, int offset, int lasers, int vent, int permeability) {
			StfrRunController.autopilotAdjust(reactor, fuel, suppression, offset, lasers, vent, permeability);
		}
	}

	static final class State {

		long processedTick = Long.MIN_VALUE;
		long previousPower;
		int stableTicks;
		boolean active;
		int activeLevel;
		long engagedTick;
		boolean crashed;
		boolean resetRequired;
		boolean stopAnnounced;
		int laserRecoveryTicks;
		boolean recoveringReaction;
		boolean recordActions = true;
		long unpoweredSince = Long.MIN_VALUE;
		double previousTemperature;
		double previousPressure;
		StfrTemperatureControl temperatureControl = new StfrTemperatureControl();
	}

	public static void tick(World world, TileEntityLogicDevice source) {
		StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, source.getPos());
		if (network.reactor == null) return;
		StfrReactor reactor = network.reactor;
		State state = STATES.computeIfAbsent(reactor, ignored -> new State());
		if (!network.ioAvailable) {
			// Network tiles may load after the saved reactor. Keep the lease until
			// the network can positively report the owner's redstone state.
			state.active = false;
			state.stableTicks = 0;
			reactor.setAutopilotActive(false);
			return;
		}
		long tick = world.getTotalWorldTime();
		if (state.processedTick == tick) return;
		state.processedTick = tick;

		TileEntityLogicDevice selected = selectPowered(world, network);
		if (selected == null) {
			if (!state.active && reactor.autopilotLease.armed()) {
				if (state.unpoweredSince == Long.MIN_VALUE) state.unpoweredSince = tick;
				if (tick - state.unpoweredSince < 40L) return;
			}
			if (state.active || state.crashed) {
				LOG.info(
						"STFR autopilot disengaged at {}: redstone signal removed", reactor.center);
				announce(world, reactor, "message.hbmstfr.autopilot.disabled");
			}
			state.active = false;
			state.activeLevel = 0;
			state.crashed = false;
			state.resetRequired = false;
			state.stopAnnounced = false;
			state.previousTemperature = 0D;
			state.previousPressure = 0D;
			state.laserRecoveryTicks = 0;
			state.recoveringReaction = false;
			if (source.kind() == BlockLogicDevice.Kind.AUTOPILOT_3
					&& source.autopilotForceShutdown()) source.setAutopilotForceShutdown(false);
			reactor.setAutopilotActive(false);
			clearLease(world, reactor);
			return;
		}
		state.unpoweredSince = Long.MIN_VALUE;
		// The destabilizer owns every reactor control until the rotor
		// transition is complete. Redstone changes during that interval must
		// not re-arm a second controller on top of it.
		if (reactor.destabilizerStage() != 0) {
			state.active = false;
			reactor.setAutopilotActive(false);
			clearLease(world, reactor);
			return;
		}
		int level = level(selected.kind());
		boolean forcedStop = level == 3 && selected.autopilotForceShutdown();
		if (state.resetRequired) {
			state.active = false;
			reactor.setAutopilotActive(false);
			clearLease(world, reactor);
			if (reactor.runState() != StfrReactor.RunState.OFFLINE && tick % 40 == 0)
				reactor.eventSound(world, StfrFailureSounds.get("alarms.notification"),
					SoundCategory.BLOCKS, 4F, 1F);
			return;
		}
		if (reactor.runState() != StfrReactor.RunState.ONLINE && reactor.catastrophic()) {
			if (!state.crashed) {
				LOG.info(
						"STFR autopilot crashed at {}: reactor entered {}",
						reactor.center,
						reactor.runState());
				announce(
						world,
						reactor,
						"message.hbmstfr.autopilot.crash");
			}
			state.active = false;
			state.crashed = true;
			reactor.setAutopilotActive(false);
			clearLease(world, reactor);
			if (reactor.runState() != StfrReactor.RunState.OFFLINE && tick % 40 == 0)
				reactor.eventSound(world, StfrFailureSounds.get("alarms.notification"),
					SoundCategory.BLOCKS, 4F, 1F);
			return;
		}
		if (reactor.runState() != StfrReactor.RunState.ONLINE) {
			state.active = false;
			reactor.setAutopilotActive(false);
			clearLease(world, reactor);
			return;
		}
		state.crashed = false;
		boolean resuming = !state.active
				&& reactor.canResumeAutopilot(selected.getPos(), level);
		if (!state.active || state.activeLevel != level
				|| !selected.getPos().equals(reactor.autopilotLease.owner())) {
			if (!state.active && !resuming) {
				clearLease(world, reactor);
				if (!canArm(reactor, state)) return;
			}
			state.active = true;
			state.activeLevel = level;
			state.engagedTick = resuming
					? Math.min(tick, reactor.autopilotLease.engagedTick()) : tick;
			state.temperatureControl = new StfrTemperatureControl();
			state.laserRecoveryTicks = 0;
			state.recoveringReaction = false;
			state.previousTemperature = reactor.core().coreTemperature();
			state.previousPressure = reactor.core().pressure();
			reactor.setAutopilotActive(true);
			reactor.rememberAutopilot(selected.getPos(), level, state.engagedTick);
			StfrManager.get(world).markDirty();
			LOG.info(
					"STFR autopilot level {} engaged at {} (fuel={}, suppress={}, offset={},"
						+ " rpm={})",
					level,
					reactor.center,
					reactor.fuelInputLevel(),
					reactor.suppression(),
					reactor.offset(),
					rpm(reactor));
			if (!resuming) announce(world, reactor, "message.hbmstfr.autopilot.enabled", level);
		}
		// A fault is an entered contingency state, not merely a warning.  The
		// controller remains online through warnings and corrects its knobs.
		reactor.setAutopilotActive(true);
		if (level == 3
				&& (forcedStop || reactor.fuelRemaining() <= selected.autopilotFuelThreshold())) {
			if (!state.stopAnnounced) {
				state.stopAnnounced = true;
				announce(world, reactor, "message.hbmstfr.autopilot.stopping");
			}
			if (calmForShutdown(new LiveControls(reactor), level, tick, true)
					&& StfrRunController.autopilotShutdown(reactor, world)) {
				selected.setAutopilotForceShutdown(false);
				state.active = false;
				reactor.setAutopilotActive(false);
				clearLease(world, reactor);
			}
			return;
		}
		state.stopAnnounced = false;
		regulate(reactor, level, tick, state);
	}

	/**
	 * The destabilizer forcibly takes the controls. Redstone must be cycled before this autopilot
	 * can arm again.
	 */
	public static void crashForDestabilizer(World world, StfrReactor reactor) {
		State state = STATES.computeIfAbsent(reactor, ignored -> new State());
		boolean wasActive = state.active || reactor.autopilotActive();
		state.active = false;
		state.crashed = true;
		state.resetRequired = true;
		state.stableTicks = 0;
		reactor.setAutopilotActive(false);
		clearLease(world, reactor);
		LOG.info(
				"STFR autopilot interlocked at {}: digamma destabilizer took control",
				reactor.center);
		if (wasActive)
			announce(world, reactor, "message.hbmstfr.autopilot.destabilizer");
	}

	static void clearLease(World world, StfrReactor reactor) {
		if (!reactor.autopilotLease.armed()) return;
		reactor.clearAutopilotLease();
		StfrManager.get(world).markDirty();
	}

	static TileEntityLogicDevice selectPowered(
			World world, StfrLogicNetwork.Snapshot network) {
		TileEntityLogicDevice result = null;
		for (BlockPos pos : network.devices) {
			if (!world.isBlockPowered(pos)) continue;
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TileEntityLogicDevice)) continue;
			TileEntityLogicDevice candidate = (TileEntityLogicDevice) tile;
			if (level(candidate.kind()) == 0) continue;
			if (result == null
					|| level(candidate.kind()) > level(result.kind())
					|| level(candidate.kind()) == level(result.kind())
							&& pos.toLong() < result.getPos().toLong()) result = candidate;
		}
		return result;
	}

	/** A manual stop request is accepted only from the controller that owns the reactor now. */
	public static boolean requestForceShutdown(World world, TileEntityLogicDevice device) {
		if (device.kind() != BlockLogicDevice.Kind.AUTOPILOT_3
				|| !world.isBlockPowered(device.getPos())) return false;
		StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, device.getPos());
		if (!network.ioAvailable || network.reactor == null || !network.reactor.autopilotActive())
			return false;
		if (selectPowered(world, network) != device) return false;
		device.setAutopilotForceShutdown(true);
		return true;
	}

	static boolean canArm(StfrReactor reactor, State state) {
		// Direct generation may be disabled by server configuration, therefore
		// use the simulated reaction rather than exported HE for arming.
		long power = Math.round(reactor.autopilotTelemetry(reactor.core().reactionPower(), 1));
		boolean eligible = reactor.runState() == StfrReactor.RunState.ONLINE
				&& !reactor.hasAutopilotBlockingWarnings()
				&& reactor.core().shield() > StfrConfig.autopilotArmShieldPercent;
		if (!eligible) {
			state.previousPower = 0;
			state.stableTicks = 0;
			return false;
		}
		boolean stable =
				power > 0
						&& (state.previousPower == 0
								|| Math.abs(power - state.previousPower)
										<= Math.max(StfrConfig.autopilotArmPowerVariationFloor, state.previousPower
												* StfrConfig.autopilotArmPowerVariationPercent / 100));
		state.previousPower = power;
		state.stableTicks = stable ? state.stableTicks + 1 : 0;
		return state.stableTicks >= StfrConfig.autopilotArmStableTicks;
	}

	static void regulate(StfrReactor reactor, int level, long tick, State state) {
		StfrAutopilotRegulation.regulate(reactor, level, tick, state);
	}

	static void regulate(Controls reactor, int level, long tick, State state) {
		StfrAutopilotRegulation.regulate(reactor, level, tick, state);
	}

	/** Only the planned operating setpoint differs; physical emergency guards stay shared. */
	static double operatingTemperatureTarget(int level, long elapsedTicks) {
		return StfrAutopilotRegulation.operatingTemperatureTarget(level, elapsedTicks);
	}

	/** Runs the production controller against a headless reactor for long physics checks. */
	static final class Simulation {

		private final State state = new State();
		private final int level;

		Simulation(Controls reactor) {
			this(reactor, 3);
		}

		Simulation(Controls reactor, int level) {
			this.level = level;
			state.recordActions = false;
			state.active = true;
			state.activeLevel = level;
			state.previousTemperature = reactor.core().coreTemperature();
			state.previousPressure = reactor.core().pressure();
		}

		void tick(Controls reactor, long tick) {
			regulate(reactor, level, tick, state);
		}

		boolean shutdownTick(Controls reactor, long tick) {
			return calmForShutdown(reactor, 3, tick, false);
		}
	}

	static boolean calmForShutdown(
			Controls reactor, int level, long tick, boolean recordActions) {
		return StfrAutopilotRegulation.calmForShutdown(reactor, level, tick, recordActions);
	}

	static void apply(
			StfrReactor reactor,
			int level,
			String reason,
			int fuel,
			int suppress,
			int offset,
			int laser,
			int vent,
			int permeate) {
		StfrAutopilotRegulation.apply(reactor, level, reason, fuel, suppress, offset, laser, vent, permeate);
	}

	static void apply(
			Controls reactor,
			int level,
			String reason,
			int fuel,
			int suppress,
			int offset,
			int laser,
			int vent,
			int permeate,
			boolean recordActions) {
		StfrAutopilotRegulation.apply(reactor, level, reason, fuel, suppress, offset, laser, vent, permeate, recordActions);
	}

	static double rpm(StfrReactor reactor) {
		return StfrAutopilotRegulation.rpm(reactor);
	}

	static double rpm(Controls reactor) {
		return StfrAutopilotRegulation.rpm(reactor);
	}

	static double signedRpm(Controls reactor) {
		return StfrAutopilotRegulation.signedRpm(reactor);
	}

	static int approachDelta(int current, int target, int step) {
		return StfrAutopilotRegulation.approachDelta(current, target, step);
	}

	static void announce(World world, StfrReactor reactor, String key, Object... args) {
		if (reactor.runState() == StfrReactor.RunState.OFFLINE
				&& !key.endsWith(".disabled")) return;
		reactor.eventSound(world, StfrFailureSounds.get("alarms.notification"),
				SoundCategory.AMBIENT, 4F, 1F);
		String voiceKey = key.endsWith(".enabled") && args.length > 0
				? key + "." + args[0] : key;
		StfrVoicePacket.send(world, reactor.center, voiceKey);
		for (EntityPlayer player : world.playerEntities)
			if (reactor.announcementMessages()
					&& player.getDistanceSq(reactor.center) <= 512D * 512D)
				player.sendMessage(new TextComponentTranslation(key, args));
	}

	public static int level(BlockLogicDevice.Kind kind) {
		return kind == BlockLogicDevice.Kind.AUTOPILOT_1
				? 1
				: kind == BlockLogicDevice.Kind.AUTOPILOT_2
						? 2
						: kind == BlockLogicDevice.Kind.AUTOPILOT_3 ? 3 : 0;
	}
}

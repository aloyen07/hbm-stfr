package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.Fluids;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyController;
import fun.aloyenz.stfr.reactor.states.StfrSectionState;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Persistent logical reactor; it has no block or TileEntity at its center. */
public final class StfrReactor extends StfrReactorState {

	static final Logger LOG =
			LogManager.getLogger("HBM-STFR");

	public boolean caseBreachBlock(BlockPos local) { return caseBreachPositions.contains(local.toLong()); }

	public boolean rotorBlock(BlockPos local) { return rotorPositions.contains(local.toLong()); }

	public boolean destructibleBlock(BlockPos local) { return destructiblePositions.contains(local.toLong()); }

	public boolean liftsDebris(BlockPos local) { return StfrReactorStatus.liftsDebris(this, local); }

	public double debrisReleaseY() { return center.getY() + liftReleaseLocalY - StfrStructureTemplate.CENTER.getY(); }

	final StfrFailureEffects failure = new StfrFailureEffects(this);
	final StfrMaintenance maintenance = new StfrMaintenance(this);
	final StfrEmergencyController emergency = new StfrEmergencyController(this);

	public StfrEmergencyController emergency() { return emergency; }

	public StfrMaintenance maintenance() { return maintenance; }

	public void serviceNotice(World world, String event, boolean warning) {
		StfrReactorStatus.serviceNotice(this, world, event, warning);
	}

	public void serviceAlarm(World world, long uptime) { StfrReactorStatus.serviceAlarm(this, world, uptime); }

	public StfrFailureEffects failure() { return failure; }

	public boolean catastrophic() { return StfrReactorStatus.catastrophic(this); }

	public boolean emergencyState() { return runState.ordinal() >= RunState.OVERLOAD.ordinal(); }

	public boolean terminal() { return StfrReactorStatus.terminal(this); }

	public boolean unstable() { return StfrReactorStatus.unstable(this); }

	public boolean restabilize(World world) { return StfrReactorStatus.restabilize(this, world); }

	public List<BlockPos> announcementSources(World world) {
		List<BlockPos> sources=new ArrayList<>(StfrAnnouncementController.alarmSources(this,world));
		if(!sources.contains(center))sources.add(center);
		return sources;
	}

	/** A controller signal has physical sources only in the connected announcement network. */
	public void mainframeSound(World world,SoundEvent sound,
			SoundCategory category,float volume,float pitch) {
		List<BlockPos> sources=StfrAnnouncementController.alarmSources(this,world);
		if(alarmMainframe==null)return;
		for(BlockPos source:sources)world.playSound(null,source,sound,category,volume,pitch);
	}

	public void eventSound(
			World world, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		StfrReactorStatus.eventSound(this, world, sound, category, volume, pitch);
	}

	public StfrReactor(BlockPos center) { this(UUID.randomUUID(), center); }

	StfrReactor(UUID id, BlockPos center) {
		super(id, center);
	}

	public int setRotorMap(
			World world,
			Set<Long> positions,
			Map<Long, EnumFacing> axes,
			Map<Long, Integer> particles,
			Map<Long, IBlockState> legacyHidden) {
		return StfrRotorController.setRotorMap(
				this, world, positions, axes, particles, legacyHidden);
	}

	public void tick(World world) { StfrRunController.tick(this, world); }

	void enter(RunState next) {
		runState = next;
		if (next != RunState.ONLINE) {
			autopilotActive = false;
			clearAutopilotLease();
		}
		stateTicks = 0;
		if (next == RunState.STARTUP_CONFIRMED) {
			rotorAngleUnits = 0;
			rotorVelocity = 0;
			startupPowerShortage = 0;
		}
		if (next == RunState.OFFLINE || next == RunState.STARTUP) rotorVelocity = 0;
		if (next == RunState.SHUTDOWN) shutdownCompletionAnnounced = false;
		if (next == RunState.STARTUP) dirtyShutdown = false;
		if (next == RunState.STARTUP_FAILURE) resources.setFailure(true);
		else
			resources.setOnline(
					next == RunState.ONLINE || next.ordinal() >= RunState.OVERLOAD.ordinal());
		if (next == RunState.ONLINE) warningsInitialized = false;
	}

	final transient Map<UUID, Long> rotorContactTicks = new HashMap<>();

	public boolean prime() { return StfrRunController.prime(this); }

	public boolean confirm(World world) { return StfrRunController.confirm(this, world); }

	public String startupProblem(World world) { return StfrRunController.startupProblem(this, world); }

	public enum SstbResult {
		STARTED(true),
		CONFIRMED(true),
		STOPPING(true),
		RECOVERING(true),
		BLOCKED(false),
		NOT_READY(false),
		KEY_REQUIRED(false),
		NO_ACTION(false),
		SHUTDOWN_UNSAFE(false);
		public final boolean accepted;

		SstbResult(boolean accepted) {
			this.accepted = accepted;
		}

		public String translationKey() {
			return "gui.hbmstfr.mainframe.feedback."
					+ (this == SHUTDOWN_UNSAFE ? "unsafe_shutdown" : name().toLowerCase(Locale.ROOT));
		}
	}

	/** One button press. The level of SSTB itself never controls a running state. */
	public SstbResult pulseSstb(World world) { return StfrRunController.pulseSstb(this, world); }

	public SstbResult pulseSstb(World world, BlockPos soundAt) {
		return StfrRunController.pulseSstb(this, world, soundAt);
	}

	enum Severity {
		INFO,
		NOTICE,
		WARNING,
		CRITICAL
	}

	static void particles(
			World world,
			EnumParticleTypes type,
			BlockPos pos,
			int count,
			double offsetX,
			double offsetY,
			double offsetZ,
			double speed) {
		if (world instanceof WorldServer)
			((WorldServer) world)
					.spawnParticle(
							type,
							true,
							pos.getX() + 0.5,
							pos.getY() + 0.5,
							pos.getZ() + 0.5,
							count,
							offsetX,
							offsetY,
							offsetZ,
							speed);
	}

	public boolean nominalShutdownAllowed() { return StfrRunController.nominalShutdownAllowed(this); }

	public boolean shutdown() { return StfrRunController.shutdown(this); }

	public boolean shutdown(World world) { return StfrRunController.shutdown(this, world); }

	/** Debug-only cold reset; leaves the built structure and fuel capsules untouched. */
	public void debugReset() { StfrRunController.debugReset(this); }

	public int sealFailures() { return StfrRunController.sealFailures(this); }

	public boolean caseIntact(World world) { return StfrRunController.caseIntact(this, world); }

	public boolean operatingCaseCell(StfrStructureTemplate.Cell cell) {
		return StfrRunController.operatingCaseCell(this, cell);
	}

	public Map<BlockPos, StfrBlockRole> chamberAirRoles(World world) {
		return StfrRunController.chamberAirRoles(this, world);
	}

	public boolean sectionHasPort(World world, EnumFacing side) {
		return StfrRunController.sectionHasPort(this, world, side);
	}

	public double destabilizerCompression() {
		if (destabilizerStage < 2) return 0D;
		if (destabilizerStage >= 5) return Math.max(0D, 1D - destabilizerTicks / 460D);
		double t =
				1D
						- Math.min(
								1D,
								Math.abs(core.coreTemperature() - resonanceTemperature)
										/ 70_000_000D);
		double p = 1D - Math.min(1D, Math.abs(core.pressure() - resonancePressure) / 4_000_000D);
		return Math.max(0D, Math.min(1D, (t + p) * .5D));
	}

	public boolean energyLocked() {
		return destabilizerStage == 5;
	}

	/**
	 * External power may feed the 5 MHE/t destabilizer load until the final SSTB pulse locks the
	 * accumulator.
	 */
	public boolean destabilizerAcceptsPower() {
		return destabilizerStage >= 1 && destabilizerStage <= 4;
	}

	public double telemetry(double value, int salt) {
		if (!digammaActive) return value;
		return value * (1D + Math.sin((uptimeTicks + salt * 37L) * .173D) * .04D);
	}

	public double autopilotTelemetry(double value, int salt) {
		if (!digammaActive) return value;
		return value * (1D + Math.sin((uptimeTicks + salt * 37L) * .173D) * .0001D);
	}

	public boolean hasWarnings() {
		return warningFlags != 0;
	}

	/** Only explicitly configured seal and structure warnings permit engagement. */
	public boolean hasAutopilotBlockingWarnings() {
		return StfrAutopilotArming.hasBlockingWarnings(warningFlags,
				StfrConfig.autopilotAllowSealWarning,
				StfrConfig.autopilotAllowStructureWarning);
	}

	public void setAutopilotActive(boolean active) {
		autopilotActive = active && destabilizerStage == 0;
	}

	void rememberAutopilot(BlockPos owner, int level, long engagedTick) {
		autopilotLease.remember(owner, level, engagedTick);
	}

	void clearAutopilotLease() { autopilotLease.clear(); }

	boolean canResumeAutopilot(BlockPos owner, int level) {
		return autopilotLease.canResume(owner, level, runState == RunState.ONLINE,
				destabilizerStage != 0);
	}

	boolean controlsLocked() {
		return autopilotActive || destabilizerStage > 0 && destabilizerStage < 6;
	}

	public void adjustSuppression(int delta) {
		if (!controlsLocked() && !catastrophic()) suppression = clamp(suppression + delta, 0, 100);
	}

	public void adjustOffset(int delta) {
		if (!controlsLocked() && !catastrophic()) offset = clamp(offset + delta, -100, 100);
	}

	public void adjustLasers(int delta) {
		if (!controlsLocked() && !catastrophic()) lasers = clamp(lasers + delta, 0, 100);
	}

	public void adjustVent(int delta) {
		if (!controlsLocked() && !catastrophic()) vent = clamp(vent + delta, 0, 100);
	}

	public void adjustPermeability(int delta) {
		if (!controlsLocked() && !catastrophic())
			permeability = clamp(permeability + delta, 0, 100);
	}

	static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

	public boolean portOperational(BlockPos pos) { return StfrReactorPorts.portOperational(this, pos); }

	/** A freshly broken critical block closes the port before the periodic reactor scan. */
	public boolean portOperational(World world, BlockPos pos) {
		return StfrReactorPorts.portOperational(this, world, pos);
	}

	public void adjustFuelInputLevel(int delta) { StfrRunController.adjustFuelInputLevel(this, delta); }

	public void setKeyOn(boolean value) { StfrRunController.setKeyOn(this, value); }

	public void setSstbOn(boolean value) { StfrRunController.setSstbOn(this, value); }

	public void setBreakersOn(boolean value) { StfrRunController.setBreakersOn(this, value); }

	public static boolean isWaterCapsule(FluidStack fluid) {
		return fluid != null
				&& fluid.amount > 0
				&& (fluid.getFluid() == Fluids.WATER.getFF()
						|| fluid.getFluid() == FluidRegistry.WATER);
	}

	public void disassembleSelfDestruct(World world, boolean seals) {
		StfrReactorEffects.disassembleSelfDestruct(this, world, seals);
	}

	public void removeRecoveryShielding(World world, EnumFacing side) {
		StfrReactorEffects.removeRecoveryShielding(this, world, side);
	}

	public void ejectSelfDestructRotor(World world, EnumFacing face) {
		StfrReactorEffects.ejectSelfDestructRotor(this, world, face);
	}

	static boolean isFusionFuel(FluidStack fluid) {
		return fuelOf(fluid) != StfrCoreSimulation.Fuel.NONE;
	}

	static StfrCoreSimulation.Fuel fuelOf(FluidStack fluid) { return StfrReactorSimulation.fuelOf(fluid); }

	public void destabilizerHeartbeat(World world, BlockPos pos) {
		StfrDigammaController.destabilizerHeartbeat(this, world, pos);
	}

	static int approach(int value, int target, int step) {
		return value < target
				? Math.min(target, value + step)
				: value > target ? Math.max(target, value - step) : value;
	}

	static int destabilizerRefillLevel(double intrinsicPressure) {
		return intrinsicPressure < 1_500_000D ? 40 : intrinsicPressure < 2_500_000D ? 20 : 0;
	}

	static int resonanceVentTarget(double casePressure, int requested) {
		return casePressure >= 35_000D ? 100 : requested;
	}

	static int digammaSuppression(int ordinarySuppression, double poloniumMb) {
		double reactionFactor = 2D * Math.exp(-.288D * poloniumMb / 96D);
		return clamp(
				(int) Math.round(100D - (100D - ordinarySuppression) / reactionFactor), 0, 100);
	}

	/**
	 * The test wand uses the same spatial source while explicitly emulating polaroid eligibility.
	 */
	public void playDigammaScream(World world, boolean brokenPolaroid) {
		StfrDigammaController.playDigammaScream(this, world, brokenPolaroid);
	}

	public boolean insertPoloniumBillet() { return StfrDigammaController.insertPoloniumBillet(this); }

	public int insertPolonium(int requested) { return StfrDigammaController.insertPolonium(this, requested); }

	public double insertPolonium(double requested) { return StfrDigammaController.insertPolonium(this, requested); }

	public int missingDigammaGlass(World world) { return StfrDigammaController.missingDigammaGlass(this, world); }

	public int workingStabilizers() { return StfrReactorStatus.workingStabilizers(this); }

	public boolean electrodeEnabled(StfrElectrodes.Chain chain) { return StfrReactorStatus.electrodeEnabled(this, chain); }

	public int electrodeMask() { return StfrReactorStatus.electrodeMask(this); }

	static boolean phase(long tick, int modulus, int... values) {
		int phase = (int) Math.floorMod(tick, modulus);
		for (int value : values) if (phase == value) return true;
		return false;
	}

	public void refresh(World world) { StfrReactorStructure.refresh(this, world); }

	public String turbineState(EnumFacing direction) {
		String state = StfrSectionState.state(structuralMask, refuelingMask, direction.ordinal());
		return "READY".equals(state) && maintenance.count(maintenanceComponent(direction)) > 0
				? "MAINTENANCE"
				: state;
	}

	public boolean mainElectrodeEnabled(EnumFacing direction) {
		StfrComponentStatus status = componentStatus(direction);
		return status != null && status.mainElectrodeEnabled && !turbineRefueling(direction);
	}

	public int secondaryElectrodeMask(EnumFacing direction) {
		StfrComponentStatus status = componentStatus(direction);
		return status == null ? 0 : status.secondaryElectrodeMask;
	}

	public EnumFacing fuelSlotDirection(BlockPos worldPos) {
		return StfrReactorStructure.fuelSlotDirection(this, worldPos);
	}

	public static int maintenanceComponent(EnumFacing direction) {
		return StfrReactorStructure.maintenanceComponent(direction);
	}

	public EnumFacing portDirection(BlockPos pos) { return StfrReactorStructure.portDirection(this, pos); }

	/** Creative simulation enters the real state machine, including its physical effects. */
	public void simulate(World world, RunState next) { StfrRunController.simulate(this, world, next); }

	public boolean canRemoveCapsule(BlockPos slotPos) { return StfrRunController.canRemoveCapsule(this, slotPos); }

	public LockState lockState() { return StfrRunController.lockState(this); }

	public boolean canLock() { return StfrRunController.canLock(this); }

	public boolean requestLock(World world) { return StfrRunController.requestLock(this, world); }

	public boolean requestUnlock() { return StfrRunController.requestUnlock(this); }

	public boolean requestUnlock(World world) { return StfrRunController.requestUnlock(this, world); }

	public boolean protectsHatch(StfrBlockRole role) { return StfrRunController.protectsHatch(this, role); }

	/** Close each actual hatch once; leave later manual interaction to the operator. */
	public void closeSelfDestructHatches(World world) { StfrReactorStructure.closeSelfDestructHatches(this,world); }

	@SuppressWarnings({"rawtypes", "unchecked"})
	static void setHatchOpen(World world, BlockPos pos, boolean openValue) {
		IBlockState state = world.getBlockState(pos);
		IProperty property = state.getBlock().getBlockState().getProperty("open");
		if (property != null) world.setBlockState(pos, state.withProperty(property, openValue), 3);
	}

	public void emergencyTick(int tick, int speed) {
		stateTicks = tick;
		rotorVelocity = Math.max(0, speed);
		rotorAngleUnits += rotorVelocity;
	}

	public void emergencySimulate(World world) { StfrReactorSimulation.tickSimulation(this, world); }

	public void emergencyStopOutput() {
		generatedPower = 0;
		resources.beginOutputTick(0);
	}

	public void emergencyEnter(World world, RunState next) {
		enter(next);
		announcedState = next;
		if (next == RunState.MELTDOWN) failure.meltdown(world);
		if (next == RunState.DETONATION) {
			StfrReactorStatus.prepareDestruction(this, world);
			failure.detonate(world);
			core.deactivate();
		}
	}

	public enum LockState {
		UNLOCKED,
		LOCKING,
		LOCKED,
		UNLOCKING
	}

	public enum RunState {
		OFFLINE,
		STARTUP,
		STARTUP_CONFIRMED,
		ONLINE,
		SHUTDOWN,
		STARTUP_FAILURE,
		MELTDOWN,
		DETONATION,
		OVERLOAD,
		REACTION_LOSS,
		REIGNITION,
		UNDERLOAD,
		MANUAL_RESTABILIZATION,
		STABILIZER_LOSS,
		SHUTDOWN_FAILURE,
		SELF_DESTRUCT
	}

	public NBTTagCompound write() { return StfrReactorPersistence.write(this); }

	public static StfrReactor read(NBTTagCompound tag) { return StfrReactorPersistence.read(tag); }
}

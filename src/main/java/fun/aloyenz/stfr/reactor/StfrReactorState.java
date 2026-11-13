package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.structure.StfrRotorTemplate;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

abstract class StfrReactorState {
	int emitterMigrationVersion = StfrEmitterMigration.VERSION;

	public final UUID id;
	public final BlockPos center;
	int wrongBlocks;
	int blockedAir;
	int blockedChamberAir;
	int missingPorts;
	int fixedHatchesWrong;
	int workingTurbines;
	int workingMask;
	int structuralMask;
	Map<EnumFacing, StfrComponentStatus> componentStatuses = Collections.emptyMap();
	List<StfrComponentStatus.Defect> globalDefects = Collections.emptyList();

	/** Written after the first authoritative structure scan. */
	boolean structureValidated;

	int refuelingMask;
	LockState lockState = LockState.UNLOCKED;
	RunState runState = RunState.OFFLINE;
	int stateTicks;
	boolean dirtyShutdown;
	int dirtyRadiationTicks;
	long uptimeTicks;
	long rotorAngleUnits;
	int rotorVelocity;
	transient Boolean rotorBlocksSpinning;
	transient RunState lastSentVisualState;
	final Set<Long> rotorPositions = new HashSet<>();
	final Set<Long> caseBreachPositions = new HashSet<>();
	final transient Map<UUID, Long> lightningShockTicks = new HashMap<>();

	final Set<Long> destructiblePositions = new HashSet<>();
	BlockPos liftBoxFirst, liftBoxSecond;
	int liftReleaseLocalY = 32;

	final Map<Long, EnumFacing> rotorAxes = new HashMap<>();
	final Map<Long, Integer> steamFaces = new HashMap<>();
	final Map<Long, IBlockState> hiddenRotors = new HashMap<>();
	final Map<Long, IBlockState> aimedElectrodes = new HashMap<>();
	int chamberLightLevel = -1;
	boolean rotorMapExplicit;
	boolean keyOn;
	boolean sstbOn;
	transient boolean pendingSstbPulse;
	boolean breakersOn;
	int fuelInputLevel;
	double temperature = 20.0;
	double pressure;
	long waterAmount;
	long steamAmount;
	long lubricantAmount;
	long productsAmount;
	long energyAmount;
	long generatedPower;
	long fuelRemaining;
	final StfrResourceNetwork resources = new StfrResourceNetwork();
	final StfrCoreSimulation core = new StfrCoreSimulation();
	int startupPowerShortage;

	int suppression = 90, offset, permeability, lasers, vent;
	transient boolean criticalLogged;
	transient int warningFlags;

	/** Set only while an enrolled, powered autopilot owns the ONLINE controls. */
	transient boolean autopilotActive;
	/** Saved lease. Revalidated against the powered network before controls are locked again. */
	final StfrAutopilotLease autopilotLease = new StfrAutopilotLease();

	boolean digammaActive;
	int digammaHumCountdown, digammaAmbientTicks;
	int digammaSettlingTicks;
	transient StfrDigammaSettlingControl digammaSettlingControl = new StfrDigammaSettlingControl();

	/** 0 idle, 1 arming, 2 seeking, 3 charging, 4 ready, 5 final overload, 6 braking. */
	int destabilizerStage, destabilizerTicks;

	long destabilizerHeartbeat;
	BlockPos armedDestabilizerPos;
	double resonanceTemperature, resonancePressure, digammaCharge;
	final StfrDigammaMatter digammaMatter = new StfrDigammaMatter();
	int savedDigammaFuel,
			savedDigammaSuppression,
			savedDigammaOffset,
			savedDigammaLasers,
			savedDigammaVent,
			savedDigammaPermeability;
	transient boolean warningsInitialized;
	int previousCoolant = -1;
	boolean coolantIntakeLeaking;

	transient long alarmSourcesChecked = Long.MIN_VALUE;
	transient BlockPos alarmMainframe;
	final transient List<BlockPos> alarmRelays = new ArrayList<>();
	final transient Set<UUID> startupMusicAudience = new HashSet<>();
	final StfrAnnouncementOptions announcementOptions = new StfrAnnouncementOptions();

	public boolean announcementMessages() { return announcementOptions.messages; }
	public boolean announcementVoice() { return announcementOptions.voice; }
	public boolean announcementMusic() { return announcementOptions.music; }
	public void setAnnouncementOptions(boolean messages, boolean voice, boolean music) {
		announcementOptions.set(messages, voice, music);
	}
	public void clearStartupMusicAudience() { startupMusicAudience.clear(); }
	final int[] injectionList = new int[6];
	final StfrFuelFeed[] fuelFeeds =
			IntStream.range(0, 6).mapToObj(i -> new StfrFuelFeed()).toArray(StfrFuelFeed[]::new);
	long lastCheck;
	transient RunState announcedState;
	transient LockState announcedLock;
	transient boolean announcedBreakers;

	/** The datapack broadcasts completion at shutdown tick 1120, not at OFFLINE. */
	transient boolean shutdownCompletionAnnounced;

	StfrReactorState(UUID id, BlockPos center) {
		this.id = id;
		this.center = center;
		StfrRotorTemplate.populate(rotorPositions, rotorAxes, steamFaces);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.caseBreach) caseBreachPositions.add(cell.localPos.toLong());
			if (cell.destructible) destructiblePositions.add(cell.localPos.toLong());
		}
	}

	public boolean coolantIntakeLeaking() {
		return coolantIntakeLeaking;
	}

	public RunState runState() {
		return runState;
	}

	public int stateTicks() {
		return stateTicks;
	}

	public long uptimeTicks() {
		return uptimeTicks;
	}

	public boolean keyOn() {
		return keyOn;
	}

	public boolean sstbOn() {
		return sstbOn;
	}

	public boolean breakersOn() {
		return breakersOn;
	}

	public int fuelInputLevel() {
		return fuelInputLevel;
	}

	public int rotorVelocity() {
		return rotorVelocity;
	}

	public double rotorRpm() {
		return rotorVelocity * 0.001D * 20D * 60D / (2D * Math.PI);
	}

	public double temperature() {
		return temperature;
	}

	public double pressure() {
		return pressure;
	}

	public StfrCoreSimulation core() {
		return core;
	}

	public int suppression() {
		return suppression;
	}

	public int offset() {
		return offset;
	}

	public int lasers() {
		return lasers;
	}

	public int vent() {
		return vent;
	}

	public int permeability() {
		return permeability;
	}

	public boolean autopilotActive() {
		return autopilotActive;
	}

	public boolean digammaActive() {
		return digammaActive;
	}

	public int destabilizerStage() {
		return destabilizerStage;
	}

	public int destabilizerTicks() {
		return destabilizerTicks;
	}

	public double digammaCharge() {
		return digammaCharge;
	}

	public double poloniumMb() {
		return digammaMatter.poloniumMb();
	}

	public double yaroniteMb() {
		return digammaMatter.yaroniteMb();
	}

	public long waterAmount() {
		return waterAmount;
	}

	public long steamAmount() {
		return steamAmount;
	}

	public long lubricantAmount() {
		return lubricantAmount;
	}

	public long productsAmount() {
		return productsAmount;
	}

	public long energyAmount() {
		return energyAmount;
	}

	public long generatedPower() {
		return generatedPower;
	}

	public long fuelRemaining() {
		return fuelRemaining;
	}

	public StfrResourceNetwork resources() {
		return resources;
	}

	public int wrongBlocks() {
		return wrongBlocks;
	}

	public int blockedAir() {
		return blockedAir;
	}

	public int blockedChamberAir() {
		return blockedChamberAir;
	}

	public int missingPorts() {
		return missingPorts;
	}

	public int fixedHatchesWrong() {
		return fixedHatchesWrong;
	}

	public int workingTurbines() {
		return workingTurbines;
	}

	public int refuelingTurbines() {
		return Integer.bitCount(refuelingMask);
	}

	public Map<EnumFacing, StfrComponentStatus> componentStatuses() {
		return Collections.unmodifiableMap(componentStatuses);
	}

	public StfrComponentStatus componentStatus(EnumFacing direction) {
		return componentStatuses.get(direction);
	}

	public List<StfrComponentStatus.Defect> globalDefects() {
		return globalDefects;
	}

	public boolean turbineWorks(EnumFacing direction) {
		return (workingMask & 1 << direction.ordinal()) != 0;
	}

	public boolean turbineRefueling(EnumFacing direction) {
		return (refuelingMask & 1 << direction.ordinal()) != 0;
	}

	public int injectionAmount(EnumFacing direction) {
		return injectionList[direction.getIndex()];
	}
}

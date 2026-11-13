package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.reactor.StfrEmitterMigration;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrShutdownWater;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.security.StfrSecurityData;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** One persistent event controller per reactor. No global event timers or world selectors. */
public final class StfrEmergencyController {

	final StfrReactor r;
	int stage, recovery;
	boolean manualFailed, selfWasOffline, internalWarning, recovering;
	UUID manualNetwork;
	String music = "";
	float musicPitch = 1;
	final Set<UUID> musicAudience = new HashSet<>();
	final Map<Long, IBlockState> lenses = new LinkedHashMap<>();
	boolean selfPending, selfCommitted, selfFinale, selfBlast;
	int selfCharge, selfPreviousTicks, emitterMask;
	boolean emitterInstructionSent;
	StfrReactor.RunState selfPreviousState = OFFLINE;
	NBTTagCompound previousEmergencySnapshot = new NBTTagCompound();

	public StfrEmergencyController(StfrReactor reactor) {
		r = reactor;
	}

	public int recoveryTicks() {
		return recovering ? Math.max(1, recovery) : 0;
	}

	public int stage() {
		return stage;
	}

	public void reset() {
		stage = recovery = 0;
		manualFailed = selfWasOffline = internalWarning = recovering = false;
		lenses.clear();
		manualNetwork = null;
		selfPending = selfCommitted = selfFinale = selfBlast = false;
		selfCharge = emitterMask = 0;
		emitterInstructionSent=false;
		previousEmergencySnapshot = new NBTTagCompound();
	}

	public boolean selfFormingOffline() {
		return StfrSelfDestructControl.selfFormingOffline(this);
	}

	public boolean selfPending() {
		return StfrSelfDestructControl.selfPending(this);
	}

	public float selfChargeFraction() {
		return StfrSelfDestructControl.selfChargeFraction(this);
	}

	public boolean selfBlast() {
		return StfrSelfDestructControl.selfBlast(this);
	}

	public double selfMultiplier() {
		return StfrSelfDestructControl.selfMultiplier(this);
	}

	public double selfProjectedMultiplier() {
		return StfrSelfDestructControl.selfProjectedMultiplier(this);
	}

	public boolean canAbortSelfDestruct() {
		return StfrSelfDestructControl.canAbortSelfDestruct(this);
	}

	public boolean beamsActive() {
		switch (r.runState()) {
			case UNDERLOAD:
				return false;
			case SHUTDOWN_FAILURE:
				return !recovering ? r.stateTicks() < 4050 : recovery < 990;
			case SELF_DESTRUCT:
				return !selfWasOffline && r.stateTicks() < 3620;
			case STABILIZER_LOSS:
				return recovering || r.stateTicks() >= 280;
			default:
				return r.emergencyState();
		}
	}

	public void checkOnline(World w) {
		StfrEmergencyDispatch.checkOnline(this, w);
	}

	public boolean mechanismBreach(World w) {
		return r.workingStabilizers() < 6 || r.refuelingTurbines() > 0;
	}

	List<StfrStructureTemplate.Cell> emitterCells(World w) {
		return StfrEmergencyLenses.emitterCells(this, w);
	}

	BlockPos worldPos(long local) {
		return StfrEmergencyLenses.worldPos(this, local);
	}

	void rememberLenses(World w) {
		StfrEmergencyLenses.rememberLenses(this, w);
	}

	public boolean auxiliaryLens(BlockPos local, IBlockState state) {
		return StfrEmergencyLenses.auxiliaryLens(this, local, state);
	}

	int lensesInMode(World w, boolean auxiliary) {
		return StfrEmergencyLenses.lensesInMode(this, w, auxiliary);
	}

	public boolean canConfirm(World w) {
		return StfrEmergencyRecovery.canConfirm(this, w);
	}

	public String confirmationProblem(World w) {
		return StfrEmergencyRecovery.confirmationProblem(this, w);
	}

	public void explainConfirmation(World w) {
		StfrEmergencyRecovery.explainConfirmation(this, w);
	}

	public boolean confirm(World w) {
		return StfrEmergencyRecovery.confirm(this, w);
	}

	public void start(World w, StfrReactor.RunState state) {
		StfrEmergencyDispatch.start(this, w, state);
	}

	public void simulationClock(World world, int tick) {
		StfrSelfDestructControl.simulationClock(this, world, tick);
	}

	public boolean selfDestruct(World w) {
		return StfrSelfDestructControl.selfDestruct(this, w);
	}

	/**
	 * Runs before the current state: a queued administrative protocol supersedes any recoverable
	 * scene.
	 */
	public void pollSelfDestruct(World w) {
		StfrSelfDestructControl.pollSelfDestruct(this, w);
	}

	public boolean abortSelfDestruct(World w) {
		return StfrSelfDestructControl.abortSelfDestruct(this, w);
	}

	public void tick(World w) {
		StfrEmergencyDispatch.tick(this, w);
	}

	void rotorExplosion(World w, EnumFacing face) {
		BlockPos pos = r.center.offset(face, 9);
		r.failure().blastDamage(w, pos, 3F);
		particles(w, pos, EnumParticleTypes.FIREWORKS_SPARK, 250, 1, .45);
		particles(w, pos, EnumParticleTypes.EXPLOSION_HUGE, 2, .5, 0);
		particles(w, pos, EnumParticleTypes.SMOKE_LARGE, 100, 1, .15);
		sound(w, "explosion.bang2", SoundCategory.MASTER, 10, .8F);
		r.failure().shake(w, 128, 30, 100, 25);
	}

	void shutdownRecovery(World w) {
		if (recovery == 395) surge(w);
		if (recovery == 400) r.failure().shake(w, 256, 1000, 100, 20, true);
		if (recovery >= 400 && recovery <= 675) rotorParticles(w, EnumParticleTypes.CLOUD, 10);
		if (recovery >= 675 && recovery <= 990) rotorParticles(w, EnumParticleTypes.FLAME, 10);
		if (recovery >= 695 && recovery <= 970)
			for (EnumFacing side : EnumFacing.VALUES)
				particles(w, r.center.offset(side, 10), EnumParticleTypes.FLAME, 20, .5, .5);
		if (recovery >= 695 && recovery <= 990) {
			r.core().purge(.04);
			particles(w, r.center, EnumParticleTypes.FIREWORKS_SPARK, 20, 1, .2);
		}
		if (recovery >= 990) rotorParticles(w, EnumParticleTypes.SMOKE_LARGE, 10);
		int[] times = {800, 855, 870, 920, 940, 950};
		for (int i = 0; i < times.length; i++)
			if (recovery == times[i]) {
				rotorExplosion(w, EnumFacing.VALUES[i]);
				r.removeRecoveryShielding(w, EnumFacing.VALUES[i]);
				List<TileEntityFuelSlot> slots = fuelSlots(w);
				EnumFacing explodedSide = EnumFacing.VALUES[i];
				slots.removeIf(slot -> r.fuelSlotDirection(slot.getPos()) != explodedSide);
				if (!slots.isEmpty()) {
					TileEntityFuelSlot slot = slots.get(0);
					r.failure().blastDamage(w, slot.getPos(), 1.5F);
					particles(w, slot.getPos(), EnumParticleTypes.CLOUD, 1000, 0, 1);
					particles(w, slot.getPos(), EnumParticleTypes.EXPLOSION_HUGE, 1, 0, 0);
					sound(w, "explosion.bang2", SoundCategory.MASTER, 10, 1);
					sound(w, "explosion.bang2", SoundCategory.MASTER, 10, .5F);
					r.failure().shake(w, 80, 20, 100, 20);
					if (r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE)) {
						slot.removeForBreak();
						w.destroyBlock(slot.getPos(), false);
					}
				}
			}
		if (recovery == 695) r.failure().shake(w, 256, 705, 300, 20, true);
		if (recovery == 1400) r.emergencyTick(r.stateTicks(), 0);
		if (recovery == 990) {
			r.failure().fadeShake(w, 256, 410);
			r.removeRecoveryShielding(w, null);
			r.failure().crashMainframes(w, 1300);
			r.core().deactivate();
			burst(w);
			broadcast(
					w,
					"stfr.shut_fail.restab_end",
					"Shutdown Failure Restabilization Sequence Successful. Reactor Core Is Now"
						+ " Offline.",
					1);
		}
		if (recovery >= 1500) {
			r.emergencyEnter(w, OFFLINE);
			r.requestUnlock();
		}
	}

	List<TileEntityFuelSlot> fuelSlots(World w) {
		List<TileEntityFuelSlot> slots = new ArrayList<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells())
			if (cell.defaultRole == StfrBlockRole.FUEL_SLOT) {
				TileEntity tile = w.getTileEntity(worldPos(cell.localPos.toLong()));
				if (tile instanceof TileEntityFuelSlot) slots.add((TileEntityFuelSlot) tile);
			}
		return slots;
	}

	boolean waterCapsules(World w, boolean check) {
		return StfrEmergencyRecovery.waterCapsules(this, w, check);
	}

	String waterProblem(World w) {
		List<TileEntityFuelSlot> slots = fuelSlots(w);
		if (slots.size() < StfrShutdownWater.CAPSULES) return "slots";
		int water = 0, ready = 0, locked = 0;
		for (TileEntityFuelSlot slot : slots) {
			if (!StfrReactor.isWaterCapsule(ItemFuelCapsule.getFluid(slot.getCapsule()))) continue;
			if (!StfrShutdownWater.enough(
					ItemFuelCapsule.getFluid(slot.getCapsule()).amount)) continue;
			water++;
			if (slot.getPhase() != TileEntityFuelSlot.READY) continue;
			ready++;
			if (slot.isLocked()) locked++;
		}
		if (water < StfrShutdownWater.CAPSULES) return "water";
		if (ready < StfrShutdownWater.CAPSULES) return "capsule_not_ready";
		if (locked < StfrShutdownWater.CAPSULES) return "capsule_unlocked";
		return "ready";
	}

	void announceEmitters(World w) {
		StfrEmergencyLenses.announceEmitters(this, w);
	}

	void notice(World w, String key) {
		StfrEmergencyPresentation.notice(this, w, key);
	}

	void announceExpectedEmitters(World w) {
		StfrEmergencyLenses.announceExpectedEmitters(this, w);
	}

	boolean breakersConnected(World w) {
		for (TileEntity tile : w.loadedTileEntityList)
			if (tile instanceof TileEntityMultiFluidPort) {
				StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, tile.getPos());
				if (net.reactor != r) continue;
				boolean secure =
						manualNetwork != null
								&& StfrSecurityData.get(w).policy(manualNetwork).enabled();
				int count = 0;
				for (BlockPos pos : net.devices) {
					TileEntity device = w.getTileEntity(pos);
					if (device instanceof TileEntityLogicDevice
							&& ((TileEntityLogicDevice) device).kind()
									== BlockLogicDevice.Kind.BREAKER
							&& ((TileEntityLogicDevice) device).breakerClosed()
							&& (!secure || StfrSecurity.enrolled(device, manualNetwork))) count++;
				}
				if (count >= 15) {
					r.setBreakersOn(true);
					return true;
				}
			}
		return false;
	}

	boolean mainframeConnected(World w) {
		for (TileEntity tile : w.loadedTileEntityList)
			if (tile instanceof TileEntityLogicDevice
					&& ((TileEntityLogicDevice) tile).kind() == BlockLogicDevice.Kind.MAINFRAME
					&& StfrLogicNetwork.inspect(w, tile.getPos()).reactor == r
					&& StfrLogicNetwork.inspect(w, tile.getPos()).ioAvailable) return true;
		return false;
	}

	void nextStage(World w) {
		stage++;
		repeated(w, "alarms.amogus", 4, 8, 0);
		String[] voice = {"", "breakers", "reconnect_panel", "replace_lenses", "sstb"};
		String[] objective = {
			"",
			"Engage 15 Reactor Breakers",
			"Reconnect Reactor Control Panel Via. Mainframe",
			"Replace Auxiliary Quartz Lenses With STFR Plasma Lenses",
			"Have At Least 2 Stabilizers Functional, Then Confirm Restabilization By Pressing SSTB"
		};
		broadcast(
				w,
				"stfr.manual_restab." + voice[stage],
				"Current Objective: " + objective[stage],
				1);
	}

	void finish(World w, String advancement) {
		r.core().restabilize();
		r.emergencyEnter(w, ONLINE);
		StfrAdvancements.grant(w, r.center, advancement);
		repeated(w, "charge_up.heavy_shutdown", 4, 15, 1);
	}

	void detonate(World w) {
		r.emergencyEnter(w, DETONATION);
	}

	void surge(World w) {
		repeated(w, "charge_up.heavy_startup", 4, 10, 1.5F);
	}

	void implosion(World w) {
		repeated(w, "explosion.pressuregigaboom", 2, 20, 1);
		if (r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE))
			for (BlockPos p : BlockPos.getAllInBox(r.center.add(-4, -4, -4), r.center.add(4, 4, 4)))
				if (w.getBlockState(p).getMaterial() == Material.GLASS) w.setBlockToAir(p);
		burst(w);
	}

	void burst(World w) {
		StfrEmergencyPresentation.burst(this, w);
	}

	void burst(World w, boolean damage) {
		StfrEmergencyPresentation.burst(this, w, damage);
	}

	void cold(World w, int tick) {
		StfrEmergencyPresentation.cold(this, w, tick);
	}

	void plasma(World w, int tick) {
		StfrEmergencyPresentation.plasma(this, w, tick);
	}

	void fire(World w) {
		StfrEmergencyPresentation.fire(this, w);
	}

	void rotorParticles(World w, EnumParticleTypes particle, int count) {
		StfrEmergencyPresentation.rotorParticles(this, w, particle, count);
	}

	void particles(
			World w, BlockPos p, EnumParticleTypes type, int count, double spread, double speed) {
		StfrEmergencyPresentation.particles(this, w, p, type, count, spread, speed);
	}

	public void sound(World w, String id, float volume, float pitch) {
		StfrEmergencyPresentation.sound(this, w, id, volume, pitch);
	}

	public void sound(World w, String id, SoundCategory category, float volume, float pitch) {
		StfrEmergencyPresentation.sound(this, w, id, category, volume, pitch);
	}

	void repeated(World w, String id, int times, float volume, float pitch) {
		StfrEmergencyPresentation.repeated(this, w, id, times, volume, pitch);
	}

	void music(World w, String id, float volume, float pitch) {
		StfrEmergencyPresentation.music(this, w, id, volume, pitch);
	}

	void resumeMusic(World w) {
		StfrEmergencyPresentation.resumeMusic(this, w);
	}

	public void clearMusicAudience() {
		musicAudience.clear();
	}

	public void stopMusic(World w) {
		StfrEmergencyPresentation.stopMusic(this, w);
	}

	public void broadcast(World w, String voice, String text, int level) {
		StfrEmergencyPresentation.broadcast(this, w, voice, text, level);
	}

	void broadcast(World w, String voice, String text, int level, String key) {
		StfrEmergencyPresentation.broadcast(this, w, voice, text, level, key);
	}

	public NBTTagCompound write() {
		return StfrEmergencyPersistence.write(this);
	}

	/** Update references saved during an in-progress legacy emergency too. */
	public void migrateLegacyLenses() {
		lenses.replaceAll((pos, state) -> StfrEmitterMigration.replacement(state));
		migrateLensSnapshot(previousEmergencySnapshot);
	}

	static void migrateLensSnapshot(NBTTagCompound tag) {
		NBTTagList entries = tag.getTagList("lenses", 10);
		for (int i = 0; i < entries.tagCount(); i++) {
			NBTTagCompound entry = entries.getCompoundTagAt(i);
			IBlockState state = NBTUtil.readBlockState(entry.getCompoundTag("state"));
			entry.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), StfrEmitterMigration.replacement(state)));
		}
		NBTTagCompound previous=StfrEmergencyPersistence.previousSnapshot(tag);
		if (!previous.isEmpty())migrateLensSnapshot(previous);
	}

	public void read(NBTTagCompound tag) {
		StfrEmergencyPersistence.read(this, tag);
	}
}

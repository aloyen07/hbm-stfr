package fun.aloyenz.stfr.reactor;

import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.reactor.StfrReactor.SstbResult;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

final class StfrRunController {

	private StfrRunController() {}

	static void adjustFuelInputLevel(StfrReactor r, int delta) {
		if (r.controlsLocked() || r.catastrophic()) return;
		r.fuelInputLevel = Math.max(0, Math.min(100, r.fuelInputLevel + delta));
	}

	static void setKeyOn(StfrReactor r, boolean value) {
		if (!value && r.core.exists() || !r.controlsLocked() && !r.terminal()) r.keyOn = value;
	}

	static void setSstbOn(StfrReactor r, boolean value) {
		if (r.controlsLocked() && r.destabilizerStage == 0 || r.terminal()) return;
		if (value && !r.sstbOn) r.pendingSstbPulse = true;
		r.sstbOn = value;
	}

	static void setBreakersOn(StfrReactor r, boolean value) {
		if (!r.controlsLocked() && !r.terminal()) r.breakersOn = value;
	}

	static void autopilotAdjust(
			StfrReactor r,
			int fuel,
			int suppress,
			int rotateOffset,
			int laser,
			int venting,
			int permeate) {
		if (r.runState != RunState.ONLINE || r.destabilizerStage > 0) return;
		r.fuelInputLevel = StfrReactor.clamp(r.fuelInputLevel + fuel, 0, 100);
		r.suppression = StfrReactor.clamp(r.suppression + suppress, 0, 100);
		r.offset = StfrReactor.clamp(r.offset + rotateOffset, -100, 100);
		r.lasers = StfrReactor.clamp(r.lasers + laser, 0, 100);
		r.vent = StfrReactor.clamp(r.vent + venting, 0, 100);
		r.permeability = StfrReactor.clamp(r.permeability + permeate, 0, 100);
	}

	static boolean autopilotShutdown(StfrReactor r, World world) {
		if (r.runState != RunState.ONLINE || !r.nominalShutdownAllowed()) return false;
		r.keyOn = false;
		if (!r.caseIntact(world)) StfrDirtyShutdown.begin(r, world);
		else r.enter(RunState.SHUTDOWN);
		return true;
	}

	static void tick(StfrReactor r, World world) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		if (!world.isAreaLoaded(origin, origin.add(28, 28, 28))) return;
		if (!StfrEmitterMigration.migrate(r, world)) return;
		// Component statuses are deliberately derived data and are not written to
		// the reactor save.  A loaded ONLINE reactor therefore starts with an
		// empty map.  Do not let the ONLINE contingency check interpret that
		// transient empty map as six failed stabilizers before the normal
		// twenty-tick structure refresh happens.
		boolean firstStructureValidation = !r.structureValidated;
		if (r.componentStatuses.isEmpty()) r.refresh(world);
		// An interrupted previous cycle may leave the resonance flag in the
		// saved reactor while the core is already offline. A new startup must
		// never inherit digamma physics without a completed destabilization.
		if (r.digammaActive
				&& (r.runState == RunState.OFFLINE
						|| r.runState == RunState.STARTUP
						|| r.runState == RunState.STARTUP_CONFIRMED))
			StfrDigammaController.finishDigammaShutdown(r, world);
		// Builds saved by the first implementation of the contingency states
		// can contain STABILIZER_LOSS solely because the empty derived map was
		// read as "zero stabilizers" on their first loaded tick.  Repair only
		// that pre-validation record; ordinary, already validated failures
		// still require their normal SSTB recovery procedure.
		if (firstStructureValidation
				&& r.runState == RunState.STABILIZER_LOSS
				&& r.workingStabilizers() > 0) {
			r.emergency.stopMusic(world);
			r.emergency.reset();
			r.failure.reset();
			r.enter(RunState.ONLINE);
			r.announcedState = RunState.ONLINE;
		}
		if (r.announcedState == null) r.announcedState = r.runState;
		if (r.runState != RunState.OFFLINE) r.uptimeTicks++;
		if (r.announcedLock == null) {
			r.announcedLock = r.lockState;
			r.announcedBreakers = r.breakersOn;
		}
		if (r.pendingSstbPulse) {
			r.pendingSstbPulse = false;
			r.pulseSstb(world);
		}
		if (r.lockState == LockState.LOCKING
				|| r.lockState == LockState.UNLOCKING
				|| r.lockState == LockState.LOCKED && world.getTotalWorldTime() % 20 == 0)
			StfrReactorStructure.advanceHatchGroup(r, world);
		r.emergency.pollSelfDestruct(world);
		StfrDigammaController.tickDestabilizer(r, world);
		StfrRunController.tickRunState(r, world);
		if (r.lockState == LockState.LOCKED
				&& StfrEmergencyTimeline.instructionsComplete(r.runState, r.stateTicks))
			r.lockState = LockState.UNLOCKING;
		int lightLevel = StfrChamberLight.level(r.runState, r.stateTicks);
		if (r.runState == RunState.SELF_DESTRUCT) lightLevel = r.core.exists() ? 15 : 0;
		if (r.runState == RunState.SHUTDOWN_FAILURE && r.emergency.recoveryTicks() >= 990)
			lightLevel = 0;
		if (r.chamberLightLevel != lightLevel || world.getTotalWorldTime() % 20 == 0) {
			StfrChamberLight.sync(world, r.center, lightLevel);
			r.chamberLightLevel = lightLevel;
		}
		if (r.runState == RunState.OFFLINE) {
			r.core.coolOffline();
			r.core.forceOffline();
			r.temperature = r.pressure = 0;
			r.warningFlags = 0;
			r.warningsInitialized = false;
			r.previousCoolant = -1;
			r.coolantIntakeLeaking = false;
		}
		if (r.runState == RunState.STARTUP_CONFIRMED && r.stateTicks >= 615 && r.stateTicks <= 756)
			StfrReactorSimulation.injectFuel(r, world);
		if (r.core.exists()
				&& (r.runState == RunState.ONLINE
						|| r.runState == RunState.STARTUP_FAILURE
						|| r.runState == RunState.STARTUP_CONFIRMED && r.stateTicks >= 860
						|| r.runState == RunState.SHUTDOWN && r.stateTicks <= 240))
			StfrReactorSimulation.tickSimulation(r, world);
		if (r.digammaActive && r.core.exists()) {
			StfrDigammaController.tickDigammaRadiation(r, world);
			StfrDigammaController.tickDigammaSoundscape(r, world);
		} else {
			r.digammaHumCountdown = 0;
			r.digammaAmbientTicks = 0;
		}
		StfrRotorController.tickRotorEffects(r, world);
		StfrRotorController.tickRotorContacts(r, world);
		StfrLightningRodHazard.tick(r, world);
		StfrDirtyShutdown.tick(r, world);
		r.maintenance.tick(world, r.uptimeTicks);
		StfrAnnouncementController.announceTransitions(r, world);
		if (world.getTotalWorldTime() % 20 == 0) StfrReactorSimulation.refreshTelemetry(r, world);
		if (world.getTotalWorldTime() - r.lastCheck < 20) return;
		r.refresh(world);
	}

	static void tickRunState(StfrReactor r, World world) {
		if (r.emergencyState()) {
			r.emergency.tick(world);
			return;
		}
		switch (r.runState) {
			case OFFLINE:
				return;
			case STARTUP:
				StfrStartupSequence.tickStartup(r,world);
				return;
			case STARTUP_CONFIRMED:
				StfrStartupSequence.tickStartupConfirmed(r,world);
				return;
			case ONLINE:
				if (r.destabilizerStage < 5)
					r.rotorVelocity =
							r.destabilizerStage >= 2
									? Math.min(200, 100 + Math.max(0, r.destabilizerTicks - 40) / 4)
									: 100;
				else if (r.destabilizerStage == 5)
					r.rotorVelocity = Math.min(600, 200 + r.destabilizerTicks * 400 / 460);
				else if (r.destabilizerStage == 6) {
					r.rotorVelocity = Math.max(100, r.rotorVelocity - 2);
					if (++r.destabilizerTicks == 20) StfrAnnouncementController.announceDigamma(r, world, "digamma.online");
					if (r.rotorVelocity == 100) r.destabilizerStage = 0;
				}
				r.rotorAngleUnits += r.rotorVelocity;
				r.emergency.checkOnline(world);
				if (r.runState != RunState.ONLINE) return;
				if (world.getTotalWorldTime() % 160 == 0) {
					world.playSound(
							null, r.center, STFRSounds.HUMMING, SoundCategory.AMBIENT, 3.0F, 1.0F);
					world.playSound(
							null,
							r.center,
							STFRSounds.SYSTEM_NOISE,
							SoundCategory.AMBIENT,
							3.0F,
							1.0F);
				}
				return;
			case SHUTDOWN:
				StfrShutdownSequence.tickShutdown(r,world);
				return;
			case STARTUP_FAILURE:
				StfrFailureSequence.tickStartupFailure(r,world);
				return;
			case MELTDOWN:
				StfrFailureSequence.tickMeltdown(r,world);
				return;
			case DETONATION:
				StfrFailureSequence.tickDetonation(r,world);
				return;

			default:
				return;
		}
	}

	static void tickStartupAlarm(StfrReactor r, World world) { StfrStartupControls.tickStartupAlarm(r, world); }

	static void diagnoseStartupPower(StfrReactor r, World world, long supplied) {
		StfrStartupControls.diagnoseStartupPower(r, world, supplied);
	}

	static void abortStartup(StfrReactor r, World world, boolean automatic) {
		StfrStartupControls.abortStartup(r, world, automatic);
	}

	static void abortStartup(StfrReactor r, World world, String event, boolean automatic) {
		StfrStartupControls.abortStartup(r, world, event, automatic);
	}

	static void stopStartupMusic(StfrReactor r, World world) { StfrStartupControls.stopStartupMusic(r, world); }

	static boolean prime(StfrReactor r) { return StfrStartupControls.prime(r); }

	static boolean confirm(StfrReactor r, World world) { return StfrStartupControls.confirm(r, world); }

	static String startupProblem(StfrReactor r, World world) { return StfrStartupControls.startupProblem(r, world); }

	static SstbResult pulseSstb(StfrReactor r, World world) { return r.pulseSstb(world, r.center); }

	static SstbResult pulseSstb(StfrReactor r, World world, BlockPos soundAt) {
		if (r.dirtyShutdown && r.runState == RunState.SHUTDOWN) return SstbResult.STOPPING;
		if (StfrDirtyShutdown.allowedFrom(r.runState)
				&& !r.keyOn && r.core.exists() && !r.caseIntact(world)) {
			StfrDirtyShutdown.begin(r, world);
			world.playSound(null, soundAt, STFRSounds.ALARM_BEEP_2,
					SoundCategory.AMBIENT, 4.0F, 1.0F);
			return SstbResult.STOPPING;
		}
		if (StfrDigammaController.destabilizerPulse(r, world)) return SstbResult.CONFIRMED;
		if (r.autopilotActive) return SstbResult.BLOCKED;
		if (r.emergencyState()) {
			if (!r.emergency.canConfirm(world)) r.emergency.explainConfirmation(world);
			if (!r.keyOn) return SstbResult.KEY_REQUIRED;
			return r.emergency.confirm(world) ? SstbResult.RECOVERING : SstbResult.NOT_READY;
		}
		if (r.runState == RunState.STARTUP_FAILURE) {
			if (!r.restabilize(world)) return SstbResult.NOT_READY;
			// Recovery is a real SSTB action too: use the same alarm as startup.
			world.playSound(null, soundAt, STFRSounds.ALARM_2, SoundCategory.AMBIENT, 4.0F, 1.0F);
			return SstbResult.RECOVERING;
		}
		if (r.catastrophic()) return SstbResult.BLOCKED;
		RunState before = r.runState;
		SstbResult result;
		if (r.keyOn) {
			if (r.runState == RunState.OFFLINE) {
				boolean started =
						r.lockState == LockState.UNLOCKED ? r.requestLock(world) : r.prime();
				result = started ? SstbResult.STARTED : SstbResult.BLOCKED;
			} else if (r.runState == RunState.STARTUP) {
				result = r.confirm(world) ? SstbResult.CONFIRMED : SstbResult.NOT_READY;
			} else {
				result = SstbResult.NO_ACTION;
			}
		} else if (r.runState == RunState.ONLINE) {
			if (!r.nominalShutdownAllowed()) {
				StfrAnnouncementController.announce(r, world, "shutdown.shield_unsafe", Severity.WARNING, null);
				result = SstbResult.SHUTDOWN_UNSAFE;
			} else result = r.shutdown(world) ? SstbResult.STOPPING : SstbResult.NO_ACTION;
		} else {
			result = SstbResult.KEY_REQUIRED;
		}
		if (r.runState != before)
			world.playSound(
					null, soundAt, STFRSounds.ALARM_BEEP_2, SoundCategory.AMBIENT, 4.0F, 1.0F);
		return result;
	}

	static boolean nominalShutdownAllowed(StfrReactor r) {
		return StfrEmergencyRules.nominalShutdownAllowed(r.core.shield(), r.core.shieldStress());
	}

	static boolean shutdown(StfrReactor r) {
		if (r.controlsLocked()
				|| r.keyOn
				|| r.runState != RunState.ONLINE
				|| !r.nominalShutdownAllowed()) return false;
		r.enter(RunState.SHUTDOWN);
		return true;
	}

	static boolean shutdown(StfrReactor r, World world) {
		if (!StfrDirtyShutdown.allowedFrom(r.runState)) return false;
		if (r.dirtyShutdown && r.runState == RunState.SHUTDOWN) return true;
		if (r.core.exists() && !r.caseIntact(world)) {
			StfrDirtyShutdown.begin(r, world);
			return true;
		}
		return shutdown(r);
	}

	static boolean hasReadyLogicNetwork(StfrReactor r, World world) {
		for (TileEntity tile : world.loadedTileEntityList) {
			if (!(tile instanceof TileEntityLogicDevice)) continue;
			TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
			if (device.kind() != BlockLogicDevice.Kind.MAINFRAME) continue;
			StfrLogicNetwork.Snapshot snapshot = StfrLogicNetwork.inspect(world, tile.getPos());
			if (snapshot.ready() && snapshot.reactor == r) return true;
		}
		return false;
	}

	static void debugReset(StfrReactor r) {
		r.failure.reset();
		r.enter(RunState.OFFLINE);
		r.keyOn = false;
		r.sstbOn = false;
		r.breakersOn = false;
		if (r.lockState == LockState.LOCKED || r.lockState == LockState.LOCKING)
			r.lockState = LockState.UNLOCKING;
	}

	static int sealFailures(StfrReactor r) {
		int failures = 0;
		for (StfrComponentStatus status : r.componentStatuses.values())
			failures += status.sealFailures;
		return failures;
	}

	static boolean caseIntact(StfrReactor r, World world) {
		if (r.sealFailures() > 0) return false;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!r.operatingCaseCell(cell)) continue;
			BlockPos pos = origin.add(cell.localPos);
			if (!StfrStructureValidator.matchesCell(
					cell, cell.defaultRole, world.getBlockState(pos), world, pos)) return false;
		}
		return true;
	}

	static boolean operatingCaseCell(StfrReactor r, StfrStructureTemplate.Cell cell) {
		int depth =
				Math.max(
						Math.abs(cell.localPos.getX() - 14),
						Math.max(
								Math.abs(cell.localPos.getY() - 14),
								Math.abs(cell.localPos.getZ() - 14)));
		return r.caseBreachBlock(cell.localPos)
				|| cell.defaultRole == StfrBlockRole.SEAL
				|| depth <= 5
						&& (cell.defaultRole == StfrBlockRole.RADIATION_SHIELD
								|| cell.defaultRole == StfrBlockRole.HATCH_FIXED);
	}

	static Map<BlockPos, StfrBlockRole> chamberAirRoles(StfrReactor r, World world) {
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, r.center);
		return analyzer == null ? StfrStructureTemplate.airRoles() : analyzer.airRoles();
	}

	static boolean sectionHasPort(StfrReactor r, World world, EnumFacing side) {
		for (TileEntityMultiFluidPort port : StfrReactorSimulation.portsForDirection(r, world, side))
			if (r.portOperational(port.getPos())) return true;
		return false;
	}

	static boolean hasReadyFuel(StfrReactor r, World world) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		int readySections = 0;
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.FUEL_SLOT) continue;
			TileEntity tile = world.getTileEntity(origin.add(cell.localPos));
			if (!(tile instanceof TileEntityFuelSlot)) continue;
			TileEntityFuelSlot slot = (TileEntityFuelSlot) tile;
			if (slot.getPhase() != TileEntityFuelSlot.READY || !slot.isLocked()) continue;
			FluidStack fluid = ItemFuelCapsule.getFluid(slot.getCapsule());
			StfrCoreSimulation.Fuel incoming = r.fuelOf(fluid);
			if (incoming != StfrCoreSimulation.Fuel.NONE && r.core.accepts(incoming)) {
				readySections++;
			}
		}
		// confirm_startup in TERF requires a non-empty injection list, not two
		// completed directions. Mixtures are tracked independently by the core.
		return readySections >= 1;
	}

	static void simulate(StfrReactor r, World world, RunState next) {
		if (next == RunState.DETONATION && r.runState == next && r.failure.detonated()) return;
		r.failure.stop(world, "hbmstfr:reactor.startup_music", 512);
		for (String name :
				new String[] {
					"music.idunno_reactor_meltdown_sequence",
					"music.hacknet_ost_paki_trak",
					"music.explosionmusic",
					"music.hacknet_music_end",
					"charge_up.reactordetonationcharging"
				}) r.failure.stop(world, "hbmstfr:failure." + name, 512);
		r.failure.reset();
		r.emergency.reset();
		r.emergency.stopMusic(world);
		r.refresh(world);
		if (next == RunState.OFFLINE) {
			r.debugReset();
			r.core.deactivate();
			return;
		}
		r.keyOn = next != RunState.SHUTDOWN;
		r.sstbOn = false;
		r.breakersOn = true;
		r.lockState = LockState.LOCKED;
		if (next == RunState.STARTUP || next == RunState.STARTUP_CONFIRMED) r.core.beginStartup();
		else if (next != RunState.SELF_DESTRUCT || r.core.exists()) r.core.simulationPreset();
		if (next.ordinal() >= RunState.OVERLOAD.ordinal()) {
			r.emergency.start(world, next);
			r.emergency.simulationClock(world, StfrConfig.simulationInitialTicks(next));
			return;
		}
		r.enter(next);
		if (next == RunState.ONLINE) r.rotorVelocity = 200;
		else if (next == RunState.STARTUP_FAILURE) {
			r.rotorVelocity = 300;
			r.failure.startup(world, "SIMULATION");
			r.announcedState = next;
		} else if (next == RunState.MELTDOWN) {
			r.failure.meltdown(world);
			r.announcedState = next;
		} else if (next == RunState.DETONATION) {
			StfrReactorStatus.prepareDestruction(r, world);
			r.failure.detonate(world);
			r.core.deactivate();
			r.announcedState = next;
		}
		r.stateTicks = StfrConfig.simulationInitialTicks(next);
	}

	static boolean canRemoveCapsule(StfrReactor r, BlockPos slotPos) {
		if (r.runState == RunState.SHUTDOWN_FAILURE && r.emergency.recoveryTicks() == 0) return true;
		if (r.catastrophic()) return false;
		if (r.lockState == LockState.UNLOCKED || r.lockState == LockState.UNLOCKING) return true;
		EnumFacing own = r.fuelSlotDirection(slotPos);
		if (own == null) return true;
		return (r.refuelingMask & ~(1 << own.ordinal())) == 0;
	}

	static LockState lockState(StfrReactor r) { return r.lockState; }

	static boolean canLock(StfrReactor r) {
		return r.workingTurbines >= 2 && r.blockedChamberAir == 0;
	}

	static boolean requestLock(StfrReactor r, World world) {
		r.refresh(world);
		if (!r.keyOn
				|| r.runState != RunState.OFFLINE
				|| r.lockState != LockState.UNLOCKED
				|| !r.canLock()) return false;
		r.lockState = LockState.LOCKING;
		r.enter(RunState.STARTUP);
		return true;
	}

	static boolean requestUnlock(StfrReactor r) {
		if (r.autopilotActive || r.catastrophic()) return false;
		if (r.lockState != LockState.LOCKED && r.lockState != LockState.LOCKING) return false;
		if (r.runState == RunState.ONLINE || r.runState == RunState.STARTUP_CONFIRMED) {
			if (!r.nominalShutdownAllowed()) return false;
			r.enter(RunState.SHUTDOWN);
		} else if (r.runState == RunState.STARTUP) r.enter(RunState.OFFLINE);
		r.lockState = LockState.UNLOCKING;
		return true;
	}

	static boolean requestUnlock(StfrReactor r, World world) {
		if (r.catastrophic()) return false;
		if (r.dirtyShutdown && r.runState == RunState.SHUTDOWN) return true;
		if (r.core.exists() && !r.caseIntact(world)) {
			StfrDirtyShutdown.begin(r, world);
			return true;
		}
		return requestUnlock(r);
	}

	static boolean protectsHatch(StfrReactor r, StfrBlockRole role) {
		return r.lockState != LockState.UNLOCKED
				&& r.lockState != LockState.UNLOCKING
				&& (role == StfrBlockRole.HATCH_FIXED || role == StfrBlockRole.HATCH_FREE);
	}
}

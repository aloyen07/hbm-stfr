package fun.aloyenz.stfr.reactor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructMechanics;
import fun.aloyenz.stfr.reactor.states.StfrSectionState;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

final class StfrReactorStructure {

	private StfrReactorStructure() {}

	static int maintenanceComponent(EnumFacing direction) {
		switch (direction) {
			case UP:
				return 1;
			case SOUTH:
				return 2;
			case EAST:
				return 3;
			case NORTH:
				return 4;
			case WEST:
				return 5;
			default:
				return 6;
		}
	}

	static void refresh(StfrReactor r, World world) {
		boolean previouslyValidated = r.structureValidated;
		Map<EnumFacing, StfrComponentStatus> previousComponents = r.componentStatuses;
		List<StfrComponentStatus.Defect> previousGlobal = r.globalDefects;
		int previousRefuelingMask = r.refuelingMask;
		int previousStructuralMask = r.structuralMask;
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, r.center);
		if (analyzer != null) {
			r.caseBreachPositions.clear();
			r.caseBreachPositions.addAll(analyzer.caseBreachBlocks());
			r.destructiblePositions.clear();
			r.destructiblePositions.addAll(analyzer.destructibleBlocks());
			r.liftBoxFirst = analyzer.liftBoxFirst();
			r.liftBoxSecond = analyzer.liftBoxSecond();
			r.liftReleaseLocalY = analyzer.liftReleaseY();
		}
		StfrStructureValidator.Result result = StfrStructureValidator.inspect(world, r.center);
		r.wrongBlocks = result.wrongBlocks;
		r.blockedAir = result.blockedAir;
		r.blockedChamberAir = result.blockedChamberAir;
		r.missingPorts = result.missingPorts;
		r.fixedHatchesWrong = result.fixedHatchesWrong;
		r.workingTurbines = result.workingTurbines;
		r.structuralMask = result.workingMask;
		r.componentStatuses = result.components;
		StfrLogicNetwork.invalidate(world);
		r.structureValidated = true;
		r.globalDefects = result.globalDefects;
		r.workingMask = r.structuralMask;
		r.refuelingMask = 0;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.FUEL_SLOT) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			TileEntity tile = world.getTileEntity(origin.add(cell.localPos));
			boolean ready = false;
			if (tile instanceof TileEntityFuelSlot) {
				TileEntityFuelSlot slot = (TileEntityFuelSlot) tile;
				FluidStack fluid = ItemFuelCapsule.getFluid(slot.getCapsule());
				ready =
						slot.getPhase() == TileEntityFuelSlot.READY
								&& slot.isLocked()
								&& (r.isFusionFuel(fluid)
										|| r.runState == RunState.SHUTDOWN_FAILURE
												&& r.isWaterCapsule(fluid));
			}
			if (!ready) r.refuelingMask |= 1 << direction.ordinal();
		}
		r.refuelingMask &= r.structuralMask;
		for (EnumFacing direction : EnumFacing.VALUES)
			if ((StfrSectionState.enteredRefueling(
											previousStructuralMask,
											previousRefuelingMask,
											r.refuelingMask)
									& (1 << direction.ordinal()))
							!= 0
					&& !r.catastrophic()) {
				int component = r.maintenanceComponent(direction);
				StfrAnnouncementController.announce(r, world,
						"refueling." + component,
						Severity.NOTICE,
						STFRSounds.REFUELING[component - 1]);
			}
		r.workingMask &= ~r.refuelingMask;
		r.workingTurbines = Integer.bitCount(r.workingMask);
		if (previouslyValidated)
			for (EnumFacing direction : EnumFacing.VALUES) {
				int bit = 1 << direction.ordinal(), component = r.maintenanceComponent(direction);
				boolean refueled =
						(previousRefuelingMask & bit) != 0
								&& (r.refuelingMask & bit) == 0
								&& (r.structuralMask & bit) != 0;
				StfrComponentStatus before = previousComponents.get(direction),
						after = r.componentStatuses.get(direction);
				if (refueled) StfrAnnouncementController.announce(r, world, "refueled." + component, Severity.NOTICE, null);
				if (before != null
						&& after != null
						&& after.restorationLevel() > before.restorationLevel()) {
					boolean sealed = after.restorationLevel() == 2;
					StfrAnnouncementController.announce(r, world,
							(sealed ? "sealed." : "restored.") + component,
							Severity.NOTICE,
							null);
				}
			}
		if (previouslyValidated
				&& previousGlobal.stream()
						.anyMatch(
								old ->
										r.globalDefects.stream()
												.noneMatch(
														current ->
																current.localPos.equals(
																		old.localPos)))) {
			StfrAnnouncementController.announce(r, world, "restored.0", Severity.NOTICE, null);
		}
		r.lastCheck = world.getTotalWorldTime();
	}

	static EnumFacing fuelSlotDirection(StfrReactor r, BlockPos worldPos) {
		BlockPos local = worldPos.subtract(r.center.subtract(StfrStructureTemplate.CENTER));
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		return cell != null && (cell.defaultRole == StfrBlockRole.FUEL_SLOT
				|| StfrStructureValidator.optionalFuelSlot(cell, null))
				? StfrRadialSymmetry.sourceDirection(local)
				: null;
	}

	static EnumFacing portDirection(StfrReactor r, BlockPos pos) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			BlockPos port = origin.add(cell.localPos);
			if (port.equals(pos) || port.offset(direction).equals(pos)) return direction;
		}
		return null;
	}

	static void closeSelfDestructHatches(StfrReactor r,World world) {
		if(world.isRemote)return;
		BlockPos origin=r.center.subtract(StfrStructureTemplate.CENTER);
		TileEntityStructureAnalyzer analyzer=StfrStructureValidator.matchingAnalyzer(world,r.center);
		for(StfrStructureTemplate.Cell cell:StfrStructureTemplate.cells()) {
			StfrBlockRole role=analyzer==null?cell.defaultRole:analyzer.getRole(cell.localPos);
			if(!StfrSelfDestructMechanics.accessHatch(role))continue;
			BlockPos pos=origin.add(cell.localPos);
			if(!world.isBlockLoaded(pos))continue;
			IBlockState state=world.getBlockState(pos);
			if(state.getBlock()!=cell.expected.getBlock())continue;
			boolean target=StfrSelfDestructMechanics.lockedHatchOpen(role,StfrStructureValidator.isOpen(cell.expected));
			if(StfrStructureValidator.isOpen(state)==target)continue;
			StfrReactor.setHatchOpen(world,pos,target);
			world.playSound(null,pos,SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,SoundCategory.BLOCKS,.9F,1F);
		}
	}

	static void advanceHatchGroup(StfrReactor r, World world) {
		if(r.runState()==StfrReactor.RunState.SELF_DESTRUCT
				&& StfrSelfDestructMechanics.hatchesHandedOff(r.stateTicks(),r.emergency().selfFormingOffline()))return;
		Map<BlockPos, Boolean> pending = new HashMap<>();
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			StfrBlockRole role = cell.defaultRole;
			if (role != StfrBlockRole.HATCH_FIXED
					&& role != StfrBlockRole.HATCH_FREE
					&& role != StfrBlockRole.HATCH) continue;
			if (r.lockState == LockState.LOCKED && role == StfrBlockRole.HATCH) continue;
			BlockPos pos = origin.add(cell.localPos);
			IBlockState actual = world.getBlockState(pos);
			if (actual.getBlock() != cell.expected.getBlock()) continue;
			boolean expected = StfrStructureValidator.isOpen(cell.expected);
			boolean target =
					r.lockState == LockState.UNLOCKING
							? expected
							: StfrSelfDestructMechanics.lockedHatchOpen(role,expected);
			if (StfrStructureValidator.isOpen(actual) != target) pending.put(pos, target);
		}
		if (pending.isEmpty()) {
			if (r.lockState == LockState.LOCKING) r.lockState = LockState.LOCKED;
			else if (r.lockState == LockState.UNLOCKING) r.lockState = LockState.UNLOCKED;
			return;
		}
		List<BlockPos> candidates = new ArrayList<>(pending.keySet());
		BlockPos seed = candidates.get(world.rand.nextInt(candidates.size()));
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(seed);
		List<BlockPos> group = new ArrayList<>();
		while (!queue.isEmpty()) {
			BlockPos current = queue.removeFirst();
			if (!pending.containsKey(current) || group.contains(current)) continue;
			group.add(current);
			for (EnumFacing side : EnumFacing.HORIZONTALS) {
				BlockPos neighbor = current.offset(side);
				if (pending.containsKey(neighbor) && !group.contains(neighbor)) queue.add(neighbor);
			}
		}
		for (BlockPos pos : group) r.setHatchOpen(world, pos, pending.get(pos));
		world.playSound(
				null,
				seed,
				SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
				SoundCategory.BLOCKS,
				0.9F,
				0.9F + world.rand.nextFloat() * 0.2F);
	}
}

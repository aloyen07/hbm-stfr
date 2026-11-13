package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.Fluids;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;
import java.util.List;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;

final class StfrReactorSimulation {

	private StfrReactorSimulation() {}

	static StfrCoreSimulation.Fuel fuelOf(FluidStack fluid) {
		if (fluid == null || fluid.amount <= 0) return StfrCoreSimulation.Fuel.NONE;
		if (fluid.getFluid() == Fluids.DEUTERIUM.getFF()) return StfrCoreSimulation.Fuel.DEUTERIUM;
		if (fluid.getFluid() == Fluids.TRITIUM.getFF()) return StfrCoreSimulation.Fuel.TRITIUM;
		if (fluid.getFluid() == Fluids.HELIUM3.getFF()) return StfrCoreSimulation.Fuel.HELIUM3;
		return StfrCoreSimulation.Fuel.NONE;
	}

	static void tickSimulation(StfrReactor r, World world) {
		boolean ordinary =
				r.runState == RunState.ONLINE
						|| r.runState == RunState.REIGNITION && r.stateTicks >= 380;
		boolean direct = ordinary && StfrConfig.directGeneration && r.destabilizerStage != 5;
		double coverage = StfrReactorStatus.electrodeCoverage(r, world);
		double poison = Math.exp(-.288D * r.poloniumMb() / 96D);
		r.core.setDigammaPhysics(r.digammaActive, poison);
		r.core.setShieldOverdrive(r.destabilizerStage >= 1 && r.destabilizerStage <= 5);
		r.core.tick(
				r.resources,
				r.workingStabilizers(),
				r.fuelInputLevel,
				r.suppression,
				r.offset,
				r.permeability,
				r.lasers,
				r.vent,
				!r.catastrophic() || ordinary,
				coverage,
				!r.caseIntact(world));
		if (ordinary || r.runState == RunState.STARTUP_CONFIRMED && r.stateTicks >= 860)
			StfrReactorSimulation.injectFuel(r, world);
		StfrOperatingEffects.tick(world, r);
		if (r.runState == RunState.STARTUP_FAILURE
				&& r.stateTicks >= 783
				&& !r.failure.recovering()) r.core.failureHeat();
		r.temperature = r.core.caseTemperature();
		r.pressure = r.core.pressure();
		if (direct) {
			long raw =
					StfrElectrodeGeneration.power(
							r.core.reactionPower(),
							coverage,
							StfrConfig.directHePerMw,
							StfrConfig.maxOnlineGenerationPerTick);
			r.generatedPower = r.resources.addEnergy((long) (raw * (r.digammaActive ? 1.8D : 1D)));
			r.resources.beginOutputTick(
					(long) (StfrConfig.maxOnlineGenerationPerTick * (r.digammaActive ? 1.8D : 1D)));
		} else {
			r.generatedPower = 0;
			r.resources.beginOutputTick(0);
		}
		StfrAnnouncementController.tickOperatingAlarms(r, world);
		if (r.runState == RunState.ONLINE && world.getTotalWorldTime() % 20 == 0)
			StfrAnnouncementController.checkOperatingWarnings(r, world);
		if (r.core.shield() <= 0
				|| r.core.pressure() >= 5_000_000
				|| r.core.caseTemperature() >= 1010) StfrReactorSimulation.logCritical(r, "CORE_LIMIT", world);
		else r.criticalLogged = false;
		StfrDigammaController.tickDigammaChemistry(r, world);
		StfrDigammaController.tickDigammaSettling(r);
	}

	static void injectFuel(StfrReactor r, World world) {
		if (r.fuelInputLevel <= 0) return;
		EnumSet<EnumFacing> fed = EnumSet.noneOf(EnumFacing.class);
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		TileEntityStructureAnalyzer analyzer = StfrStructureValidator.matchingAnalyzer(world, r.center);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!fuelSlotCell(r, world, cell, analyzer)) continue;
			EnumFacing side = StfrRadialSymmetry.sourceDirection(cell.localPos);
			if (side == null || !r.turbineWorks(side)) continue;
			TileEntity tile = world.getTileEntity(origin.add(cell.localPos));
			if (!(tile instanceof TileEntityFuelSlot)) continue;
			TileEntityFuelSlot slot = (TileEntityFuelSlot) tile;
			StfrCoreSimulation.Fuel incoming =
					r.fuelOf(ItemFuelCapsule.getFluid(slot.getCapsule()));
			if (!slot.isLocked() || slot.getPhase() != TileEntityFuelSlot.READY || !fed.add(side)) continue;
			r.fuelFeeds[side.getIndex()].tick(
					r.core, r.fuelInputLevel * 24D / 10_000D, incoming, () -> slot.drainFuel(1));
		}
	}

	private static boolean fuelSlotCell(
			StfrReactor r, World world, StfrStructureTemplate.Cell cell,
			TileEntityStructureAnalyzer analyzer) {
		if (cell.defaultRole == StfrBlockRole.FUEL_SLOT) return true;
		if (!StfrStructureValidator.optionalFuelSlot(cell, analyzer)) return false;
		BlockPos pos = r.center.subtract(StfrStructureTemplate.CENTER).add(cell.localPos);
		return world.getTileEntity(pos) instanceof TileEntityFuelSlot
				&& StfrStructureValidator.matchesCell(
						cell, cell.defaultRole, world.getBlockState(pos), world, pos, analyzer);
	}

	static void logCritical(StfrReactor r, String reason, World world) {
		if (r.criticalLogged) return;
		r.criticalLogged = true;
		LogManager.getLogger("HBM-STFR")
				.error(
						"STFR critical condition {} at {} in dimension {} (shield={}, pressure={},"
							+ " case={}C)",
						reason,
						r.center,
						world.provider.getDimension(),
						r.core.shield(),
						r.core.pressure(),
						r.core.caseTemperature());
	}

	static void refreshTelemetry(StfrReactor r, World world) {
		Arrays.fill(r.injectionList, 0);
		r.fuelRemaining = 0;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		TileEntityStructureAnalyzer analyzer = StfrStructureValidator.matchingAnalyzer(world, r.center);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!fuelSlotCell(r, world, cell, analyzer)) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			TileEntity tile = world.getTileEntity(origin.add(cell.localPos));
			if (!(tile instanceof TileEntityFuelSlot)) continue;
			TileEntityFuelSlot slot = (TileEntityFuelSlot) tile;
			FluidStack fluid = ItemFuelCapsule.getFluid(slot.getCapsule());
			if (!r.isFusionFuel(fluid)
					|| slot.getPhase() != TileEntityFuelSlot.READY
					|| !slot.isLocked()) continue;
			r.fuelRemaining += fluid.amount;
			r.injectionList[direction.getIndex()] = Math.max(0, r.fuelInputLevel * 100);
		}
		// Migrate legacy port contents before reading the common tanks.
		for (EnumFacing direction : EnumFacing.VALUES)
			for (TileEntityMultiFluidPort port : StfrReactorSimulation.portsForDirection(r, world, direction))
				if (r.portOperational(port.getPos())) port.getAllTanks();
		r.waterAmount = r.resources.water().getFill();
		r.steamAmount = r.resources.steam().getFill();
		r.lubricantAmount = r.resources.lubricant().getFill();
		r.productsAmount = r.resources.helium4().getFill();
		r.energyAmount = r.resources.energy();
	}

	static List<TileEntityMultiFluidPort> portsForDirection(
			StfrReactor r, World world, EnumFacing direction) {
		List<TileEntityMultiFluidPort> result = new ArrayList<>(2);
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT
					|| StfrRadialSymmetry.sourceDirection(cell.localPos) != direction) continue;
			BlockPos base = origin.add(cell.localPos);
			for (int i = 0; i < 2; i++) {
				TileEntity tile = world.getTileEntity(i == 0 ? base : base.offset(direction));
				if (tile instanceof TileEntityMultiFluidPort && !result.contains(tile))
					result.add((TileEntityMultiFluidPort) tile);
			}
		}
		return result;
	}
}

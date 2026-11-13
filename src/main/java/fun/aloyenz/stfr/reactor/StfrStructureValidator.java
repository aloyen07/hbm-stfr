package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.block.BlockSolidInjector;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Checks the packaged role map without depending on an analyzer block. */
public final class StfrStructureValidator {

	private StfrStructureValidator() {}

	public static Result inspect(World world, BlockPos center) {
		BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
		int wrong = 0;
		int blockedAir = 0;
		int blockedChamberAir = 0;
		int missingPorts = 0;
		int fixedHatchesWrong = 0;
		Map<EnumFacing, StfrComponentStatus.Builder> builders = new EnumMap<>(EnumFacing.class);
		for (EnumFacing direction : EnumFacing.values())
			builders.put(direction, new StfrComponentStatus.Builder(direction));
		TileEntityStructureAnalyzer analyzer = matchingAnalyzer(world, center);
		List<StfrComponentStatus.Defect> globalDefects = new ArrayList<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			BlockPos pos = origin.add(cell.localPos);
			IBlockState actual = world.getBlockState(pos);
			actual = physicalState(world, pos, cell, actual, analyzer);
			StfrBlockRole role =
					analyzer == null ? cell.defaultRole : analyzer.getRole(cell.localPos);
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			StfrComponentStatus.Builder builder =
					direction == null ? null : builders.get(direction);
			if (role == StfrBlockRole.OPTIONAL_EFFICIENCY && builder != null)
				builder.optionalTotal++;
			boolean matches = matchesCell(cell, role, actual, world, pos, analyzer);
			if (!matches) {
				wrong++;
				if (role == StfrBlockRole.PORT) missingPorts++;
				IBlockState reference = referenceState(cell, analyzer, actual);
				String reason =
						actual.getBlock() != reference.getBlock()
								? "WRONG_BLOCK"
								: role == StfrBlockRole.HATCH_FIXED
												&& isOpen(actual) != isOpen(cell.expected)
										? "REFERENCE_OPEN_STATE"
										: "WRONG_STATE";
				if (builder != null)
					builder.defect(
							cell.localPos, role, reason, reference.toString(), actual.toString());
				else
					globalDefects.add(
							new StfrComponentStatus.Defect(
									cell.localPos,
									-1,
									role,
									reason,
									reference.toString(),
									actual.toString()));
			}
			if (role == StfrBlockRole.HATCH_FIXED
					&& actual.getBlock() == cell.expected.getBlock()
					&& isOpen(actual) != isOpen(cell.expected)) fixedHatchesWrong++;
		}
		Map<BlockPos, StfrBlockRole> airRoles =
				analyzer == null ? StfrStructureTemplate.airRoles() : analyzer.airRoles();
		for (Map.Entry<BlockPos, StfrBlockRole> entry : airRoles.entrySet()) {
			if (world.isAirBlock(origin.add(entry.getKey()))) continue;
			blockedAir++;
			if (entry.getValue() == StfrBlockRole.MAYBE_AIR) blockedChamberAir++;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(entry.getKey());
			StfrComponentStatus.Builder builder =
					direction == null ? null : builders.get(direction);
			if (entry.getValue() == StfrBlockRole.MUST_AIR && builder != null)
				builder.defect(
						entry.getKey(),
						entry.getValue(),
						"OCCUPIED_AIR",
						"minecraft:air",
						world.getBlockState(origin.add(entry.getKey())).toString());
			else
				globalDefects.add(
						new StfrComponentStatus.Defect(
								entry.getKey(),
								-1,
								entry.getValue(),
								"OCCUPIED_AIR",
								"minecraft:air",
								world.getBlockState(origin.add(entry.getKey())).toString()));
		}
		int workingTurbines = 0;
		for (StfrElectrodes.Chain chain : StfrElectrodes.chains()) {
			StfrComponentStatus.Builder builder = builders.get(chain.direction);
			if (chain.index < 0) builder.mainElectrodeEnabled = chain.works(world, center);
			else if (!chain.works(world, center))
				builder.secondaryElectrodeMask &= ~(1 << chain.index);
		}
		int workingMask = 0;
		Map<EnumFacing, StfrComponentStatus> components = new EnumMap<>(EnumFacing.class);
		for (EnumFacing direction : EnumFacing.values()) {
			StfrComponentStatus status = new StfrComponentStatus(builders.get(direction));
			components.put(direction, status);
			if (status.turbineStructureValid) {
				workingTurbines++;
				workingMask |= 1 << direction.ordinal();
			}
		}
		return new Result(
				wrong,
				blockedAir,
				blockedChamberAir,
				missingPorts,
				fixedHatchesWrong,
				workingTurbines,
				workingMask,
				components,
				globalDefects);
	}

	/** Animation proxies retain the physical block used by structural checks. */
	public static IBlockState physicalState(
			World world,
			BlockPos pos,
			StfrStructureTemplate.Cell cell,
			IBlockState actual,
			TileEntityStructureAnalyzer analyzer) {
		TileEntity tile = world.getTileEntity(pos);
		IBlockState original = originalAnimationState(tile);
		if (original != null) return original;
		if (tile instanceof TileEntityRotorPlaceholder
				&& analyzer != null
				&& analyzer.originalRotorState(cell.localPos) != null)
			return analyzer.originalRotorState(cell.localPos);
		// Missing proxy data remains a defect; never assume an expected block exists.
		return actual;
	}

	static IBlockState originalAnimationState(TileEntity tile) {
		if (tile instanceof TileEntityRotorPlaceholder)
			return ((TileEntityRotorPlaceholder) tile).original();
		if (tile instanceof TileEntityElectrodeProxy)
			return ((TileEntityElectrodeProxy) tile).original();
		return null;
	}

	public static TileEntityStructureAnalyzer matchingAnalyzer(World world, BlockPos center) {
		for (TileEntity tile : world.loadedTileEntityList) {
			if (tile instanceof TileEntityStructureAnalyzer) {
				TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
				if (analyzer.getOrigin().add(StfrStructureTemplate.CENTER).equals(center))
					return analyzer;
			}
		}
		return null;
	}

	public static boolean matchesCell(
			StfrStructureTemplate.Cell cell, IBlockState actual, World world, BlockPos pos) {
		return matchesCell(cell, cell.defaultRole, actual, world, pos);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static boolean matchesCell(
			StfrStructureTemplate.Cell cell,
			StfrBlockRole role,
			IBlockState actual,
			World world,
			BlockPos pos) {
		return matchesCell(
				cell,
				role,
				actual,
				world,
				pos,
				matchingAnalyzer(
						world, pos.subtract(cell.localPos).add(StfrStructureTemplate.CENTER)));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static boolean matchesCell(
			StfrStructureTemplate.Cell cell,
			StfrBlockRole role,
			IBlockState actual,
			World world,
			BlockPos pos,
			TileEntityStructureAnalyzer analyzer) {
		if (optionalFuelSlot(cell, analyzer) && actual.getBlock() == STFRBlocks.FUEL_SLOT) {
			IBlockState injector = analyzer == null ? cell.injectorState : analyzer.injectorState(cell.localPos);
			return injector != null && actual.getValue(BlockFuelSlot.FACING)
					== injector.getValue(BlockSolidInjector.FACING);
		}
		StfrMiscRole misc = miscRole(cell, analyzer);
		IBlockState expected = referenceState(cell, analyzer, actual);
		if (misc == StfrMiscRole.INJECTOR_PLACEABLE
				&& actual.getBlock() == STFRBlocks.SOLID_INJECTOR)
			return TileEntityStructureAnalyzer.matches(expected, actual, world, pos);
		if (role == StfrBlockRole.EMITTER_BLOCK) {
			StfrReactor reactor =
					StfrManager.get(world)
							.get(pos.subtract(cell.localPos).add(StfrStructureTemplate.CENTER));
			if (reactor != null && reactor.emergency().auxiliaryLens(cell.localPos, actual))
				return true;
		}
		if (expected.getBlock() != actual.getBlock()) return false;
		if (role == StfrBlockRole.PORT && !misc.isRotable()) {
			IProperty facing = expected.getBlock().getBlockState().getProperty("facing");
			if (facing != null) expected = expected.withProperty(facing, actual.getValue(facing));
		}
		if (role == StfrBlockRole.HATCH || role == StfrBlockRole.HATCH_FREE) {
			IProperty open = expected.getBlock().getBlockState().getProperty("open");
			if (open != null) expected = expected.withProperty(open, actual.getValue(open));
		}
		return TileEntityStructureAnalyzer.matches(expected, actual, world, pos);
	}

	public static boolean optionalFuelSlot(
			StfrStructureTemplate.Cell cell, TileEntityStructureAnalyzer analyzer) {
		EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
		return direction != null && direction.getAxis().isVertical()
				&& miscRole(cell, analyzer) == StfrMiscRole.INJECTOR_PLACEABLE;
	}

	public static StfrMiscRole miscRole(
			StfrStructureTemplate.Cell cell, TileEntityStructureAnalyzer analyzer) {
		return analyzer == null ? cell.miscRole : analyzer.getMiscRole(cell.localPos);
	}

	public static IBlockState referenceState(
			StfrStructureTemplate.Cell cell,
			TileEntityStructureAnalyzer analyzer,
			IBlockState actual) {
		if (optionalFuelSlot(cell, analyzer) && actual.getBlock() == STFRBlocks.FUEL_SLOT) {
			IBlockState injector = analyzer == null ? cell.injectorState : analyzer.injectorState(cell.localPos);
			if (injector != null)
				return actual.withProperty(BlockFuelSlot.FACING, injector.getValue(BlockSolidInjector.FACING));
		}
		if (miscRole(cell, analyzer) == StfrMiscRole.INJECTOR_PLACEABLE
				&& actual.getBlock() == STFRBlocks.SOLID_INJECTOR) {
			IBlockState injector =
					analyzer == null ? cell.injectorState : analyzer.injectorState(cell.localPos);
			if (injector != null) return injector;
		}
		return cell.expected;
	}

	/** Copies only orientation properties, preserving operational state and tile data. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IBlockState correctedRotation(IBlockState actual, IBlockState reference) {
		if (actual.getBlock() != reference.getBlock()) return actual;
		IBlockState corrected = actual;
		for (IProperty property : actual.getPropertyKeys()) {
			String name = property.getName();
			if (property.getValueClass() == EnumFacing.class
					|| name.equals("axis")
					|| name.equals("rotation"))
				corrected = corrected.withProperty(property, reference.getValue(property));
		}
		return corrected;
	}

	public static boolean isHatchRole(StfrBlockRole role) {
		return role == StfrBlockRole.HATCH
				|| role == StfrBlockRole.HATCH_FIXED
				|| role == StfrBlockRole.HATCH_FREE;
	}

	public static IBlockState correctedHatch(IBlockState actual, IBlockState reference) {
		return actual.getBlock() == reference.getBlock() ? reference : actual;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IBlockState toggleHatch(IBlockState state) {
		IProperty open = state.getBlock().getBlockState().getProperty("open");
		return open != null && open.getValueClass() == Boolean.class
				? state.withProperty(open, !Boolean.TRUE.equals(state.getValue(open)))
				: state;
	}

	public static boolean isOpen(IBlockState state) {
		IProperty<?> open = state.getBlock().getBlockState().getProperty("open");
		return open != null && Boolean.TRUE.equals(state.getValue(open));
	}

	public static final class Result {

		public final int wrongBlocks;
		public final int blockedAir;
		public final int blockedChamberAir;
		public final int missingPorts;
		public final int fixedHatchesWrong;
		public final int workingTurbines;
		public final int workingMask;
		public final Map<EnumFacing, StfrComponentStatus> components;
		public final List<StfrComponentStatus.Defect> globalDefects;

		private Result(
				int wrongBlocks,
				int blockedAir,
				int blockedChamberAir,
				int missingPorts,
				int fixedHatchesWrong,
				int workingTurbines,
				int workingMask,
				Map<EnumFacing, StfrComponentStatus> components,
				List<StfrComponentStatus.Defect> globalDefects) {
			this.wrongBlocks = wrongBlocks;
			this.blockedAir = blockedAir;
			this.blockedChamberAir = blockedChamberAir;
			this.missingPorts = missingPorts;
			this.fixedHatchesWrong = fixedHatchesWrong;
			this.workingTurbines = workingTurbines;
			this.workingMask = workingMask;
			this.components = Collections.unmodifiableMap(new EnumMap<>(components));
			this.globalDefects = Collections.unmodifiableList(new ArrayList<>(globalDefects));
		}

		public StfrComponentStatus component(EnumFacing direction) {
			return components.get(direction);
		}

		public boolean valid() {
			return wrongBlocks == 0 && blockedAir == 0;
		}
	}
}

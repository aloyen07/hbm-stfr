package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.misc.EnumSymbol;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.crafting.StfrManualRecipes;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Orientation/state preservation, annotation/tank persistence and redstone. */
public final class StfrWandInjectorAcceptanceTest {

	private static void check(boolean condition, String message) {
		Assertions.assertTrue(condition, message);
	}

	@Test
	void acceptanceScenarios() {
		Bootstrap.register();
		TileEntity.register("hbmstfr:solid_injector", TileEntitySolidInjector.class);
		IBlockState reference =
				Blocks.OAK_STAIRS
						.getDefaultState()
						.withProperty(BlockStairs.FACING, EnumFacing.NORTH);
		IBlockState wrong =
				reference
						.withProperty(BlockStairs.FACING, EnumFacing.WEST)
						.withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP);
		IBlockState repaired = StfrStructureValidator.correctedRotation(wrong, reference);
		check(
				repaired.getValue(BlockStairs.FACING) == EnumFacing.NORTH,
				"Direction repaired regardless of clicked face");
		check(
				repaired.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP,
				"Repair preserves non-orientation properties");
		check(
				StfrStructureValidator.correctedRotation(repaired, reference).equals(repaired),
				"Correctly oriented blocks stay unchanged");
		check(
				StfrStructureValidator.correctedRotation(Blocks.STONE.getDefaultState(), reference)
						.equals(Blocks.STONE.getDefaultState()),
				"Wrong block types are not replaced");
		IBlockState processed = StfrManualRecipes.preserveOrientation(wrong, reference);
		check(
				processed.getValue(BlockStairs.FACING) == EnumFacing.WEST
						&& processed.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP,
				"Configurable manual processing preserves stair placement");
		IBlockState lever = Blocks.LEVER.getDefaultState().withProperty(BlockLever.POWERED, true);
		check(
				StfrStructureValidator.correctedRotation(lever, Blocks.LEVER.getDefaultState())
						.getValue(BlockLever.POWERED),
				"Repair cannot flip a switch");
		IBlockState log =
				Blocks.LOG.getDefaultState().withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.X);
		check(
				StfrStructureValidator.correctedRotation(log, Blocks.LOG.getDefaultState())
								.getValue(BlockLog.LOG_AXIS)
						== BlockLog.EnumAxis.Y,
				"Axis orientation can be repaired");
		IBlockState hatch = Blocks.TRAPDOOR.getDefaultState();
		IBlockState badHatch =
				hatch.withProperty(BlockTrapDoor.FACING, EnumFacing.WEST)
						.withProperty(BlockTrapDoor.HALF, BlockTrapDoor.DoorHalf.TOP)
						.withProperty(BlockTrapDoor.OPEN, true);
		check(
				StfrStructureValidator.correctedHatch(badHatch, hatch).equals(hatch),
				"Hatch facing, half and openness restore together");
		IBlockState toggled = StfrStructureValidator.toggleHatch(badHatch);
		check(
				!toggled.getValue(BlockTrapDoor.OPEN)
						&& toggled.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP
						&& toggled.getValue(BlockTrapDoor.FACING) == EnumFacing.WEST,
				"Shift toggle preserves hatch placement");
		// At startup tick 375 the renderer swaps electrodes for proxy blocks.
		// A proxy is still its stored physical electrode, including its orientation.
		TileEntity.register("hbmstfr:electrode_proxy", TileEntityElectrodeProxy.class);
		TileEntityElectrodeProxy electrode = new TileEntityElectrodeProxy();
		check(
				StfrStructureValidator.originalAnimationState(electrode) == null,
				"Empty animation data cannot invent a critical block");
		electrode.configure(wrong, BlockPos.ORIGIN, true);
		check(
				StfrStructureValidator.originalAnimationState(electrode).equals(wrong),
				"Active electrode retains its real orientation for port validation");
		TileEntityElectrodeProxy reloadedElectrode = new TileEntityElectrodeProxy();
		reloadedElectrode.readFromNBT(electrode.writeToNBT(new NBTTagCompound()));
		check(
				StfrStructureValidator.originalAnimationState(reloadedElectrode).equals(wrong),
				"Reloaded animated electrodes cannot disconnect startup power");
		check(
				StfrStructureValidator.originalAnimationState(new TileEntity() {}) == null,
				"Unrelated tiles cannot impersonate an electrode");
		TileEntitySolidInjector tile = new TileEntitySolidInjector();
		NBTTagCompound initial = new NBTTagCompound();
		initial.setDouble("polonium", 15999.5);
		initial.setInteger("dose", 16000);
		initial.setInteger("interval", 72000);
		initial.setInteger("redstoneMode", 2);
		tile.readFromNBT(initial);
		NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
		TileEntitySolidInjector restored = new TileEntitySolidInjector();
		restored.readFromNBT(saved);
		check(
				restored.polonium() == 16000
						&& saved.getDouble("polonium") == 15999.5
						&& restored.dose() == 16000
						&& restored.interval() == 72000,
				"Fractional tank contents and metering settings survive saves");
		check(
				restored.mode() == TileEntitySolidInjector.RedstoneMode.LOW,
				"Redstone mode persists");
		initial.setInteger("polonium", Integer.MAX_VALUE);
		initial.setInteger("dose", -1);
		initial.setInteger("interval", Integer.MAX_VALUE);
		tile.readFromNBT(initial);
		check(
				tile.polonium() == 16000 && tile.dose() == 1 && tile.interval() == 72000,
				"Corrupt saves cannot overflow tank or disable timer");
		tile.readFromNBT(new NBTTagCompound());
		check(
				tile.polonium() == 0 && tile.dose() == 2 && tile.interval() == 600,
				"Empty saves get slow feed defaults");
		initial.setInteger("polonium", 48);
		initial.setInteger("dose", 96);
		initial.setInteger("interval", 20);
		tile.readFromNBT(initial);
		check(
				tile.polonium() == 48 && tile.dose() == 2 && tile.interval() == 600,
				"Old default injector settings migrate without discarding the tank");
		for (boolean power : new boolean[] {false, true}) {
			check(
					TileEntitySolidInjector.RedstoneMode.IGNORE.permits(power),
					"Ignore accepts either redstone state");
			check(
					TileEntitySolidInjector.RedstoneMode.HIGH.permits(power) == power,
					"High mode requires power");
			check(
					TileEntitySolidInjector.RedstoneMode.LOW.permits(power) != power,
					"Low mode requires no power");
		}
		NBTTagList roles = new NBTTagList(), misc = new NBTTagList();
		NBTTagCompound primary = new NBTTagCompound(), extra = new NBTTagCompound();
		long position = new BlockPos(14, 8, 13).toLong();
		primary.setLong("pos", position);
		primary.setString("role", "RADIATION_SHIELD");
		roles.appendTag(primary);
		extra.setLong("pos", position);
		extra.setString("role", "ROTABLE");
		misc.appendTag(extra);
		NBTTagCompound annotations = new NBTTagCompound();
		annotations.setTag("roles", roles);
		annotations.setTag("miscRoles", misc);
		annotations.setTag("caseBreach", new NBTTagList());
		annotations.setTag("rotors", new NBTTagList());
		TileEntity.register("hbmstfr:structure_analyzer", TileEntityStructureAnalyzer.class);
		TileEntityStructureAnalyzer analyzer = new TileEntityStructureAnalyzer();
		analyzer.readFromNBT(annotations);
		analyzer.readFromNBT(analyzer.writeToNBT(new NBTTagCompound()));
		check(
				analyzer.getRole(BlockPos.fromLong(position)) == StfrBlockRole.RADIATION_SHIELD,
				"Misc annotations cannot overwrite a primary role during save/reload");
		check(
				analyzer.getMiscRole(BlockPos.fromLong(position)) == StfrMiscRole.ROTABLE,
				"Independent rotation annotation persists");
		extra.setString("role", "UNDEFINED");
		analyzer.readFromNBT(annotations);
		check(
				analyzer.getMiscRole(BlockPos.fromLong(position)) == StfrMiscRole.UNDEFINED
						&& analyzer.getRole(BlockPos.fromLong(position))
								== StfrBlockRole.RADIATION_SHIELD,
				"Clearing misc role preserves the shield role");
		Fluids.WATER = new FluidType("STFR_TEST_WATER", 0, 0, 0, 0, EnumSymbol.NONE);
		StfrCoreSimulation core = new StfrCoreSimulation();
		core.setDigammaPhysics(true, 1D);
		core.ignite();
		core.tick(
				new StfrThermalResources() {
					public int waterAmount() {
						return 0;
					}

					public int steamRoom() {
						return 0;
					}

					public int productRoom() {
						return 0;
					}

					public void consumeWater(int n) {
						check(n == 0, "Cold test cannot consume coolant");
					}

					public int produceSteam(int n) {
						return 0;
					}

					public int produceHelium(int n) {
						return 0;
					}
				},
				0,
				0,
				100,
				0,
				0,
				0,
				0,
				false);
		check(core.shieldStress() > 0, "Stress regression uses an active core");
		check(
				core.shieldStress()
						== core.pressureStress() + core.spinStress() + core.rigidityStress(),
				"Digamma no longer multiplies shield stress");
		System.out.println("STFR wand and injector acceptance checks passed");
	}
}

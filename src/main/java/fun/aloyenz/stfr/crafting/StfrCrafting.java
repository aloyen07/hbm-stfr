package fun.aloyenz.stfr.crafting;

import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.items.ItemEnums.EnumCircuitType.*;
import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.item.STFRItems.*;

import com.hbm.blocks.BlockEnums;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import fun.aloyenz.stfr.reactor.StfrFluids;

/** Register after HBM has rebuilt its machine recipe tables in postInit. */
public final class StfrCrafting {

	private StfrCrafting() {}

	public static List<GenericRecipe> recipes() {
		List<GenericRecipe> all = new ArrayList<>(StfrMachineRecipes.ASSEMBLY.recipeOrderedList);
		all.addAll(StfrMachineRecipes.CHEMICAL.recipeOrderedList);
		return Collections.unmodifiableList(all);
	}

	private static AStack ore(String key, int count) {
		return new OreDictStack(key, count);
	}

	private static AStack part(Item item, int count) {
		return new ComparableStack(item, count);
	}

	private static AStack part(Block block, int count) {
		return new ComparableStack(block, count);
	}

	private static AStack part(Block block, int count, Enum<?> variant) {
		return new ComparableStack(new ItemStack(block, count, variant.ordinal()));
	}

	private static AStack coil(int count, int meta) {
		return new ComparableStack(new ItemStack(ModBlocks.fusion_component, count, meta));
	}

	private static AStack circuit(EnumCircuitType type, int count) {
		return new ComparableStack(ModItems.circuit, count, type);
	}

	private static GenericRecipe assembly(ItemStack out, int seconds, long power, AStack... in) {
		GenericRecipe recipe =
				new GenericRecipe("ass.stfr." + out.getItem().getRegistryName().getPath())
						.setup(seconds * 20, power)
						.inputItems(in)
						.outputItems(out)
						.setPools528(GenericRecipes.POOL_PREFIX_528 + "tcalloy");
		if (in.length > AssemblyMachineRecipes.INSTANCE.inputItemLimit())
			throw new IllegalArgumentException("Too many STFR inputs");
		StfrMachineRecipes.ASSEMBLY.register(recipe);
		return recipe;
	}

	private static GenericRecipe assembly(Block out, int seconds, long power, AStack... in) {
		return assembly(new ItemStack(out), seconds, power, in);
	}

	private static GenericRecipe assembly(Item out, int seconds, long power, AStack... in) {
		return assembly(new ItemStack(out), seconds, power, in);
	}

	private static void ion(GenericRecipe recipe, int amount) {
		recipe.inputFluids(new FluidStack(Fluids.IONGEL, amount));
		if (GeneralConfig.enable528) {
			String old = GenericRecipes.POOL_PREFIX_528 + "tcalloy";
			GenericRecipes.blueprintPools.get(old).remove(recipe.getInternalName());
			recipe.setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum");
		}
	}

	public static void register() {
		StfrRecipeLoader.initialize(StfrMachineRecipes.ASSEMBLY);
		StfrRecipeLoader.initialize(StfrMachineRecipes.CHEMICAL);
		StfrRecipeLoader.initialize(StfrManualRecipes.INSTANCE);
		PrecAssRecipes.INSTANCE.register(
				new GenericRecipe("precass.stfr.operator_book_mk2")
						.setup(400, 5000)
						.inputItems(part(OPERATOR_BOOK, 1), part(Items.PAPER, 4),
								part(ModItems.plate_polymer, 2), circuit(ADVANCED, 1))
						.outputItems(new ItemStack(OPERATOR_BOOK_MK2)));
		HbmDarkFusionCrafting.disableMachineRecipes();
		validate();
	}

	static void registerDefaults(boolean chemical) {
		if (chemical) {
			StfrMachineRecipes.CHEMICAL.register(
					new GenericRecipe("chem.stfr.liquid_nitrogen")
							.setup(400, 4000)
							.inputFluids(new FluidStack(Fluids.AIR, 10_000))
							.outputFluids(new FluidStack(StfrFluids.LIQUID_NITROGEN, 8000)));
			GenericRecipe coating =
					new GenericRecipe("chem.stfr.euphemium_coated_electrode")
							.setup(1200, 8000)
							.inputItems(
									part(SCHRABIDATE_ELECTRODE, 1),
									part(ModItems.powder_euphemium, 2),
									part(ModItems.plate_polymer, 4))
							.inputFluids(new FluidStack(Fluids.IONGEL, 1000))
							.outputItems(new ItemStack(EUPHEMIUM_COATED_ELECTRODE))
							.setPools528(GenericRecipes.POOL_PREFIX_528 + "chip_quantum");
			StfrMachineRecipes.CHEMICAL.register(coating);
			return;
		}
		assembly(STFR_PLASMA_LENS, 30, 4000,
				part(ModBlocks.glass_quartz, 4), ore(TCALLOY.plateCast(), 4),
				ore(CU.wireDense(), 4), part(ModItems.laser_crystal_co2, 1),
				circuit(ANALOG, 1), circuit(CAPACITOR_BOARD, 1),
				part(Items.GLOWSTONE_DUST, 6));
		assembly(STFR_AUXILIARY_QUARTZ_LENS, 15, 2000,
				part(ModBlocks.glass_quartz, 4), ore(STEEL.plate(), 4),
				ore(CU.wireDense(), 2), ore(QUARTZ.dust(), 8));
		assembly(
				new ItemStack(BSCCO_COIL_STAIRS, 2),
				10,
				1000,
				coil(1, 0),
				ore(CU.pipe(), 2),
				part(ModItems.plate_polymer, 2));
		assembly(
				new ItemStack(BSCCO_STAIRS, 2),
				15,
				2000,
				coil(1, 2),
				ore(CU.pipe(), 2),
				part(ModItems.plate_polymer, 2));
		assembly(
				new ItemStack(BSCCO_COIL_STAIRS_WELDED, 2),
				15,
				2000,
				coil(1, 1),
				ore(CU.pipe(), 2),
				part(ModItems.plate_polymer, 2));
		assembly(
				new ItemStack(COMBINE_STEEL_STAIRS, 2),
				10,
				1000,
				part(ModBlocks.block_combine_steel, 1));
		assembly(
				TECHNETIUM_ELECTRODE,
				20,
				2000,
				ore(TCALLOY.ingot(), 4),
				ore(CU.wireDense(), 4),
				ore(STEEL.plateCast(), 2));
		assembly(
				SCHRABIDATE_ELECTRODE,
				30,
				4000,
				ore(SBD.ingot(), 4),
				ore(GOLD.wireDense(), 4),
				ore(TCALLOY.plateCast(), 2));
		assembly(
				BSCCO_ELECTRODE_HANDLER,
				45,
				4000,
				coil(4, 0),
				ore(BSCCO.wireDense(), 8),
				ore(TCALLOY.plateCast(), 4),
				circuit(BISMOID, 1),
				circuit(CONTROLLER, 1),
				part(ModItems.motor_desh, 2),
				circuit(CAPACITOR_BOARD, 1));
		assembly(
				ELECTRODE_MECHANISM,
				60,
				4000,
				ore(TCALLOY.ingot(), 8),
				ore(CU.wireDense(), 8),
				circuit(BISMOID, 1),
				circuit(ADVANCED, 1),
				circuit(CAPACITOR_BOARD, 1),
				part(ModItems.motor_desh, 2),
				ore(BSCCO.wireDense(), 2));
		assembly(
				MULTI_FLUID_PORT,
				60,
				4000,
				ore(TCALLOY.plateCast(), 16),
				ore(CU.pipe(), 8),
				ore(STEEL.pipe(), 8),
				part(ModBlocks.cm_port, 1, BlockEnums.EnumCMMaterials.TCALLOY),
				ore(BSCCO.wireDense(), 16),
				circuit(BISMOID, 1),
				part(ModBlocks.cm_engine, 1, BlockEnums.EnumCMEngines.BISMUTH),
				circuit(CAPACITOR_BOARD, 1),
				ore(RUBBER.ingot(), 8));
		assembly(
				FUEL_SLOT,
				45,
				4000,
				part(ModBlocks.brick_compound, 2),
				coil(1, 0),
				ore(TCALLOY.plateCast(), 8),
				ore(W.plateCast(), 4),
				ore(ANY_HARDPLASTIC.ingot(), 8),
				circuit(ANALOG, 1),
				part(ModItems.motor_desh, 2));
		assembly(
				CAPSULE_FILLER,
				45,
				4000,
				part(ModBlocks.machine_fluidtank, 1),
				ore(STEEL.pipe(), 4),
				ore(TCALLOY.plateCast(), 8),
				ore(ANY_HARDPLASTIC.ingot(), 4),
				circuit(BISMOID, 1),
				part(ModItems.motor_desh, 2));
		assembly(
				FUEL_CAPSULE,
				45,
				2000,
				ore(DESH.plateCast(), 4),
				ore(W.plateCast(), 4),
				ore(TCALLOY.plateCast(), 8),
				ore(PB.plateCast(), 4),
				ore(RUBBER.ingot(), 8),
				ore(CU.pipe(), 4),
				part(ModBlocks.glass_quartz, 2));
		assembly(
				STRUCTURE_ANALYZER,
				60,
				4000,
				ore(TCALLOY.plateCast(), 8),
				ore(ANY_HARDPLASTIC.ingot(), 8),
				circuit(BISMOID, 4),
				circuit(CONTROLLER, 2),
				ore(GOLD.wireDense(), 4),
				part(ModBlocks.glass_quartz, 2));
		assembly(
				SOLID_INJECTOR,
				45,
				4000,
				ore(TCALLOY.plateCast(), 8),
				ore(PB.plateCast(), 8),
				ore(STEEL.shell(), 4),
				ore(STEEL.pipe(), 4),
				part(ModItems.laser_crystal_co2, 1),
				circuit(BISMOID, 1),
				part(ModItems.motor_desh, 2),
				circuit(CAPACITOR_BOARD, 1),
				part(ModBlocks.glass_quartz, 2));
		assembly(
				new ItemStack(DATA_CABLE, 8),
				10,
				1000,
				ore(MINGRADE.wireDense(), 8),
				part(ModItems.plate_polymer, 8),
				circuit(BASIC, 4));
		assembly(
				new ItemStack(COVERED_LOGIC_CABLE, 8),
				20,
				2000,
				part(DATA_CABLE, 8),
				part(ModItems.plate_polymer, 16),
				ore(STEEL.plate(), 8),
				ore(RUBBER.ingot(), 8));
		assembly(
				new ItemStack(RADIATION_SHIELDED_LOGIC_CABLE, 8),
				30,
				4000,
				part(COVERED_LOGIC_CABLE, 8),
				ore(PB.plateCast(), 16),
				ore(STEEL.plate(), 8),
				ore(RUBBER.ingot(), 8));
		assembly(
				MAINFRAME,
				60,
				4000,
				ore(TCALLOY.plateCast(), 8),
				ore(ANY_HARDPLASTIC.ingot(), 8),
				circuit(BISMOID, 4),
				circuit(ADVANCED, 2),
				circuit(CONTROLLER, 1),
				circuit(CAPACITOR_BOARD, 2),
				ore(GOLD.wireDense(), 8),
				ore(BSCCO.wireDense(), 4),
				circuit(ATOMIC_CLOCK, 1));
		assembly(
				LOGIC_TRANSMITTER,
				10,
				1000,
				ore(STEEL.plate(), 4),
				circuit(BASIC, 4),
				circuit(ANALOG, 2),
				ore(MINGRADE.wireFine(), 4),
				part(DATA_CABLE, 2));
		assembly(
				LOGIC_RECEIVER,
				10,
				1000,
				ore(STEEL.plate(), 4),
				circuit(BASIC, 4),
				circuit(ANALOG, 2),
				ore(GOLD.wireFine(), 4),
				part(DATA_CABLE, 2));
		assembly(
				LOGIC_MONITOR,
				30,
				2000,
				ore(AL.plate(), 4),
				part(ModBlocks.glass_quartz, 2),
				part(ModItems.plate_polymer, 4),
				part(ModItems.powder_polymer, 4),
				ore(GOLD.wireFine(), 8),
				circuit(SILICON, 1),
				circuit(CHIP_BISMOID, 1),
				circuit(ADVANCED, 1),
				part(Items.REDSTONE, 2),
				part(Items.GLOWSTONE_DUST, 2));
		assembly(
				LOGIC_BREAKER,
				10,
				1000,
				ore(STEEL.plate(), 4),
				ore(MINGRADE.wireDense(), 4),
				circuit(BASIC, 1),
				circuit(CAPACITOR_BOARD, 1),
				ore(RUBBER.ingot(), 4));
		assembly(
				STFR_ALARM,
				10,
				1000,
				ore(STEEL.plate(), 4),
				circuit(BASIC, 1),
				part(ModItems.coil_copper, 2),
				part(ModItems.plate_polymer, 4));
		assembly(
				PRIVILEGES,
				30,
				2000,
				ore(TCALLOY.plateCast(), 4),
				circuit(BISMOID, 2),
				circuit(ADVANCED, 1),
				circuit(CONTROLLER, 1),
				part(ModItems.plate_polymer, 4),
				ore(GOLD.wireFine(), 4),
				part(DATA_CABLE, 2));
		assembly(
				KEY_CARD,
				15,
				1000,
				circuit(ADVANCED, 1),
				part(ModItems.plate_polymer, 2),
				ore(GOLD.wireFine(), 2),
				circuit(SILICON, 1));
		assembly(
				DETONATOR_CONNECTOR,
				60,
				8000,
				ore(TCALLOY.plateCast(), 8),
				ore(BSCCO.wireDense(), 4),
				circuit(BISMOID, 2),
				circuit(ADVANCED, 2),
				circuit(CONTROLLER_QUANTUM, 1),
				circuit(ATOMIC_CLOCK, 1),
				part(DATA_CABLE, 2));
		ion(
				assembly(
						DETONATOR,
						120,
						16000,
						ore(TCALLOY.plateCast(), 4),
						ore(ANY_HARDPLASTIC.ingot(), 8),
						circuit(CONTROLLER_QUANTUM, 2),
						circuit(CHIP_QUANTUM, 2),
						circuit(ADVANCED, 2),
						circuit(ATOMIC_CLOCK, 2),
						ore(GOLD.wireFine(), 8),
						circuit(CAPACITOR_BOARD, 2),
						part(ModItems.plate_polymer, 4)),
				1000);
		assembly(
				AUTOPILOT_1,
				120,
				8000,
				part(MAINFRAME, 4),
				circuit(ADVANCED, 16),
				circuit(ANALOG, 8),
				circuit(ATOMIC_CLOCK, 8),
				part(ModItems.plate_polymer, 16),
				ore(TCALLOY.plateWelded(), 4),
				ore(GOLD.wireDense(), 16));
		assembly(
				AUTOPILOT_2,
				180,
				16000,
				part(AUTOPILOT_1, 1),
				circuit(BISMOID, 16),
				circuit(ADVANCED, 16),
				ore(BSCCO.wireDense(), 16),
				circuit(ATOMIC_CLOCK, 4),
				circuit(CONTROLLER, 8),
				ore(TCALLOY.plateWelded(), 4));
		ion(
				assembly(
						AUTOPILOT_3,
						240,
						32000,
						part(AUTOPILOT_2, 1),
						circuit(QUANTUM, 8),
						circuit(CHIP_QUANTUM, 8),
						ore(BSCCO.wireDense(), 32),
						circuit(CONTROLLER_QUANTUM, 8),
						ore(TCALLOY.plateWelded(), 4),
						ore(GOLD.wireDense(), 16)),
				4000);
		ion(
				assembly(
						DESTABILIZER,
						300,
						64000,
						part(AUTOPILOT_3, 1),
						part(ModItems.laser_crystal_digamma, 1),
						ore(TCALLOY.plateWelded(), 8),
						ore(BSCCO.wireDense(), 64),
						circuit(CONTROLLER_QUANTUM, 8),
						circuit(QUANTUM, 16),
						part(ModItems.powder_euphemium, 16),
						circuit(CAPACITOR_BOARD, 16)),
				4000);
		assembly(
				NETWORK_LOAD_ANALYZER,
				20,
				1000,
				ore(STEEL.plate(), 4),
				circuit(ANALOG, 2),
				circuit(BASIC, 2),
				part(ModBlocks.glass_quartz, 2),
				ore(CU.wireFine(), 4));
		assembly(
				REACTOR_ANALYSIS_WAND,
				20,
				1000,
				ore(TCALLOY.ingot(), 4),
				circuit(ANALOG, 2),
				circuit(ADVANCED, 2),
				part(ModBlocks.glass_quartz, 2));
		assembly(
				SIGNAL_MODIFIER,
				20,
				1000,
				ore(STEEL.plate(), 4),
				circuit(ANALOG, 1),
				circuit(CONTROLLER, 1),
				ore(GOLD.wireFine(), 4));
		assembly(
				MONITOR_PRESET_WAND,
				20,
				1000,
				part(ModItems.plate_polymer, 4),
				circuit(ADVANCED, 1),
				circuit(CONTROLLER, 1),
				part(ModBlocks.glass_quartz, 2));
	}

	private static void validate() {
		// Fail early if an HBM version lacks a required material shape.
		List<String> missing = new ArrayList<>();
		for (GenericRecipe recipe : recipes()) {
			// A fluid-only recipe has no item array in HBM's GenericRecipe.
			if (recipe.inputItem == null) continue;
			for (AStack input : recipe.inputItem)
				if (input.extractForJEI().isEmpty())
					missing.add(recipe.getInternalName() + ": " + input);
		}
		if (!missing.isEmpty())
			throw new IllegalStateException("Missing STFR ingredients: " + missing);
	}
}

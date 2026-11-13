package fun.aloyenz.stfr.crafting;

import static fun.aloyenz.stfr.block.STFRBlocks.*;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.NTMToolHandler;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.*;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.util.InventoryUtil;
import com.hbm.util.Tuple.Pair;
import java.io.IOException;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Configurable prices for the existing torch/boltgun operations in the world. */
public final class StfrManualRecipes extends GenericRecipes<StfrManualRecipes.ManualRecipe> {

	public static final StfrManualRecipes INSTANCE = new StfrManualRecipes();

	public static final class ManualRecipe extends GenericRecipe {

		public ToolType tool = ToolType.TORCH;

		ManualRecipe(String name) {
			super(name);
		}
	}

	@Override
	public String getFileName() {
		return "stfrManualProcessing.json";
	}

	@Override
	public int inputItemLimit() {
		return 12;
	}

	@Override
	public int inputFluidLimit() {
		return 0;
	}

	@Override
	public int outputItemLimit() {
		return 1;
	}

	@Override
	public int outputFluidLimit() {
		return 0;
	}

	@Override
	public boolean hasDuration() {
		return false;
	}

	@Override
	public boolean hasPower() {
		return false;
	}

	@Override
	public ManualRecipe instantiateRecipe(String name) {
		return new ManualRecipe(name);
	}

	@Override
	public void readExtraData(JsonElement element, ManualRecipe recipe) {
		recipe.tool = ToolType.valueOf(element.getAsJsonObject().get("tool").getAsString());
	}

	@Override
	public void writeExtraData(ManualRecipe recipe, JsonWriter writer) throws IOException {
		writer.name("tool").value(recipe.tool.name());
	}

	@Override
	public String getComment() {
		return "Manual HBM processing. inputItem[0] is the placed source block; remaining inputs"
				   + " are consumed materials. The tool is not consumed. Stair orientation is"
				   + " preserved. Remove underscore to enable customization.";
	}

	@Override
	public void register(ManualRecipe recipe) {
		if (recipe.tool != ToolType.TORCH && recipe.tool != ToolType.BOLT)
			throw new IllegalArgumentException("Unsupported STFR tool");
		if (recipe.inputItem == null
				|| recipe.inputItem.length == 0
				|| !(recipe.inputItem[0] instanceof ComparableStack))
			throw new IllegalArgumentException("Manual processing needs an explicit source block");
		ItemStack source = recipe.inputItem[0].getStack();
		ItemStack target =
				recipe.outputItem == null ? ItemStack.EMPTY : recipe.outputItem[0].getSingle();
		if (source.getCount() != 1
				|| target == null
				|| target.getCount() != 1
				|| Block.getBlockFromItem(source.getItem()) == Blocks.AIR
				|| Block.getBlockFromItem(target.getItem()) == Blocks.AIR)
			throw new IllegalArgumentException(
					"Manual processing must transform one block into one block");
		super.register(recipe);
		// HBM owns the full-block hooks. STFR stairs consult this same table.
		if (source.getItem() == Item.getItemFromBlock(ModBlocks.block_combine_steel)
				|| source.getItem() == Item.getItemFromBlock(COMBINE_STEEL_RIVETED)) {
			NTMToolHandler.getConversions()
					.put(
							new Pair<>(
									recipe.tool,
									new MetaBlock(
											Block.getBlockFromItem(source.getItem()),
											source.getMetadata())),
							new Pair<>(
									materials(recipe),
									new MetaBlock(
											Block.getBlockFromItem(target.getItem()),
											target.getMetadata())));
		}
	}

	@Override
	public void deleteRecipes() {
		for (ManualRecipe recipe : recipeOrderedList) {
			ItemStack source = recipe.inputItem[0].getStack();
			NTMToolHandler.getConversions()
					.remove(
							new Pair<>(
									recipe.tool,
									new MetaBlock(
											Block.getBlockFromItem(source.getItem()),
											source.getMetadata())));
		}
		NTMToolHandler.getConversions()
				.remove(new Pair<>(ToolType.BOLT, new MetaBlock(ModBlocks.block_combine_steel, 0)));
		NTMToolHandler.getConversions()
				.remove(new Pair<>(ToolType.TORCH, new MetaBlock(COMBINE_STEEL_RIVETED, 0)));
		super.deleteRecipes();
	}

	private void operation(
			String name, ItemStack source, Block target, int meta, ToolType tool, AStack... cost) {
		ManualRecipe recipe = new ManualRecipe("manual.stfr." + name);
		recipe.tool = tool;
		AStack[] inputs = new AStack[cost.length + 1];
		inputs[0] = new ComparableStack(source);
		System.arraycopy(cost, 0, inputs, 1, cost.length);
		recipe.inputItems(inputs).outputItems(new ItemStack(target, 1, meta));
		register(recipe);
	}

	@Override
	public void registerDefaults() {
		AStack steel = new OreDictStack(OreDictManager.STEEL.plateCast());
		AStack ferro = new ComparableStack(Mats.MAT_FERRO.make(ModItems.plate_cast));
		AStack bolts = new OreDictStack(OreDictManager.DURA.bolt(), 4);
		operation(
				"bscco_stairs",
				new ItemStack(BSCCO_COIL_STAIRS),
				BSCCO_COIL_STAIRS_WELDED,
				0,
				ToolType.TORCH,
				steel);
		operation(
				"bscco_vertical_stairs",
				new ItemStack(VERTICAL_BSCCO_COIL_STAIRS),
				VERTICAL_BSCCO_COIL_STAIRS_WELDED,
				0,
				ToolType.TORCH,
				steel);
		operation(
				"alliance_rivet",
				new ItemStack(ModBlocks.block_combine_steel),
				COMBINE_STEEL_RIVETED,
				0,
				ToolType.BOLT,
				bolts);
		operation(
				"alliance_weld",
				new ItemStack(COMBINE_STEEL_RIVETED),
				COMBINE_STEEL_RIVETED_WELDED,
				0,
				ToolType.TORCH,
				ferro);
		operation(
				"alliance_stairs_rivet",
				new ItemStack(COMBINE_STEEL_STAIRS),
				COMBINE_STEEL_RIVETED_STAIRS,
				0,
				ToolType.BOLT,
				bolts);
		operation(
				"alliance_stairs_weld",
				new ItemStack(COMBINE_STEEL_RIVETED_STAIRS),
				COMBINE_STEEL_RIVETED_WELDED_STAIRS,
				0,
				ToolType.TORCH,
				ferro);
		operation(
				"alliance_vertical_rivet",
				new ItemStack(COMBINE_STEEL_VERTICAL_STAIRS),
				COMBINE_STEEL_RIVETED_VERTICAL_STAIRS,
				0,
				ToolType.BOLT,
				bolts);
		operation(
				"alliance_vertical_weld",
				new ItemStack(COMBINE_STEEL_RIVETED_VERTICAL_STAIRS),
				COMBINE_STEEL_RIVETED_WELDED_VERTICAL_STAIRS,
				0,
				ToolType.TORCH,
				ferro);
	}

	public static AStack[] materials(ManualRecipe recipe) {
		return Arrays.copyOfRange(recipe.inputItem, 1, recipe.inputItem.length);
	}

	public static ManualRecipe find(Block source, ToolType tool) {
		for (ManualRecipe recipe : INSTANCE.recipeOrderedList)
			if (recipe.tool == tool
					&& Block.getBlockFromItem(recipe.inputItem[0].getStack().getItem()) == source)
				return recipe;
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static IBlockState preserveOrientation(IBlockState source, IBlockState target) {
		for (IProperty property : source.getPropertyKeys()) {
			IProperty dest = target.getBlock().getBlockState().getProperty(property.getName());
			if (dest != null && dest.getAllowedValues().contains(source.getValue(property)))
				target = target.withProperty(dest, source.getValue(property));
		}
		return target;
	}

	public static boolean apply(World world, EntityPlayer player, BlockPos pos, ToolType tool) {
		if (world.isRemote) return false;
		IBlockState state = world.getBlockState(pos);
		ManualRecipe recipe = find(state.getBlock(), tool);
		if (recipe == null) return false;
		ItemStack output = recipe.outputItem[0].getSingle();
		IBlockState next =
				preserveOrientation(
						state,
						Block.getBlockFromItem(output.getItem())
								.getStateFromMeta(output.getMetadata()));
		if (next.equals(state)) return false;
		if (!player.capabilities.isCreativeMode
				&& !InventoryUtil.doesPlayerHaveAStacks(
						player, Arrays.asList(materials(recipe)), true)) return false;
		player.inventoryContainer.detectAndSendChanges();
		return world.setBlockState(pos, next, 3);
	}
}

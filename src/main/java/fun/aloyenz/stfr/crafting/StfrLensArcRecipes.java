package fun.aloyenz.stfr.crafting;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm.items.machine.ItemScraps;
import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrLensThawing;

public final class StfrLensArcRecipes {
	private StfrLensArcRecipes() {}

	public static void register() {
		ArcFurnaceRecipes.register(new ComparableStack(STFRBlocks.STFR_FROZEN_PLASMA_LENS),
				new ArcFurnaceRecipe().solid(new ItemStack(STFRBlocks.STFR_PLASMA_LENS)));
		ArcFurnaceRecipes.register(new ComparableStack(STFRBlocks.STFR_PLASMA_LENS),
				new ArcFurnaceRecipe().solid(slag()));
	}

	public static ItemStack slag() {
		ItemStack output = ItemScraps.create(
				new Mats.MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(1)));
		output.setCount(6);
		return output;
	}

	/** Only the process() call uses this. Recipe previews and ingredient checks are pure. */
	public static ArcFurnaceRecipe completedPass(ItemStack input, boolean liquid, Random random) {
		if (liquid || input.getItem() != Item.getItemFromBlock(STFRBlocks.STFR_FROZEN_PLASMA_LENS))
			return ArcFurnaceRecipes.getOutput(input, liquid);
		NBTTagCompound data = input.hasTagCompound()
				? input.getTagCompound().copy() : new NBTTagCompound();
		boolean thawed = StfrLensThawing.process(data, random) == 0;
		ItemStack output = new ItemStack(thawed ? STFRBlocks.STFR_PLASMA_LENS
				: STFRBlocks.STFR_FROZEN_PLASMA_LENS);
		if (thawed) data.removeTag(StfrLensThawing.KEY);
		if (!data.isEmpty()) output.setTagCompound(data);
		return new ArcFurnaceRecipe().solid(output);
	}
}

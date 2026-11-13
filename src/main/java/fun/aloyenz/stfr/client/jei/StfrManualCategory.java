package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import com.hbm.api.block.IToolable;
import com.hbm.handler.jei.JEIGenericRecipeHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import fun.aloyenz.stfr.crafting.StfrManualRecipes;

final class StfrManualCategory extends JEIGenericRecipeHandler {

	private final IDrawable icon;

	StfrManualCategory(IGuiHelper helper) {
		super(
				helper,
				TOOLS,
				"jei.hbmstfr.manual_processing",
				new StfrDisplayRecipes(),
				new ItemStack(ModItems.blowtorch));
		icon = helper.createDrawableIngredient(new ItemStack(ModItems.blowtorch));
		recipes.clear();
		for (StfrManualRecipes.ManualRecipe manual :
				StfrManualRecipes.INSTANCE.recipeOrderedList) {
			List<List<ItemStack>> inputs = new ArrayList<>();
			for (AStack ingredient : manual.inputItem) inputs.add(ingredient.extractForJEI());
			List<List<ItemStack>> outputs =
					Collections.singletonList(
							Collections.singletonList(manual.outputItem[0].getSingle()));
			ItemStack tool =
					new ItemStack(
							manual.tool == IToolable.ToolType.BOLT
									? ModItems.boltgun
									: ModItems.blowtorch);
			GenericRecipe display =
					new GenericRecipe(manual.getInternalName()) {
						@Override
						public void printNEIExtras() {}
					};
			recipes.add(
					new JeiGenericRecipe(
							display, inputs, outputs, new ItemStack[] {tool}, null, 0, 0, 0));
		}
	}

	@Override
	public String getTitle() {
		return I18n.format("jei.hbmstfr.manual_processing");
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public String getModName() {
		return "STFR";
	}
}

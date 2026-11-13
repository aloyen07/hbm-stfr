package fun.aloyenz.stfr.crafting;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import java.util.Arrays;

/** Changes the native assembly default before HBM exports/synchronizes it. */
public final class StfrHbmRecipes {

	private StfrHbmRecipes() {}

	public static void addCoilWires(GenericRecipe recipe) {
		if (recipe == null || recipe.inputItem == null)
			throw new IllegalStateException(
					"Missing HBM fusion_component:2 assembly recipe (ass.fusionblanket)");
		AStack[] inputs = Arrays.copyOf(recipe.inputItem, recipe.inputItem.length + 1);
		inputs[inputs.length - 1] = new OreDictStack(OreDictManager.BSCCO.wireDense(), 8);
		recipe.inputItems(inputs);
	}
}

package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;

final class StfrDisplayRecipes extends GenericRecipes<GenericRecipe> {

	@Override
	public String getFileName() {
		return "";
	}

	@Override
	public int inputItemLimit() {
		return 1;
	}

	@Override
	public int inputFluidLimit() {
		return 0;
	}

	@Override
	public int outputItemLimit() {
		return 0;
	}

	@Override
	public int outputFluidLimit() {
		return 1;
	}

	@Override
	public void registerDefaults() {}

	@Override
	public GenericRecipe instantiateRecipe(String name) {
		return new GenericRecipe(name);
	}
}

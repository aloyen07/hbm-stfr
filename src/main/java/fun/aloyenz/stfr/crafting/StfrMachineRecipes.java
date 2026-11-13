package fun.aloyenz.stfr.crafting;

import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.ArrayList;

/** HBM's JSON format and synchronization, scoped to STFR additions. */
public final class StfrMachineRecipes extends GenericRecipes<GenericRecipe> {

	public static final StfrMachineRecipes ASSEMBLY = new StfrMachineRecipes(false);
	public static final StfrMachineRecipes CHEMICAL = new StfrMachineRecipes(true);
	private final boolean chemical;

	private StfrMachineRecipes(boolean chemical) {
		this.chemical = chemical;
	}

	private GenericRecipes<GenericRecipe> target() {
		return chemical ? ChemicalPlantRecipes.INSTANCE : AssemblyMachineRecipes.INSTANCE;
	}

	@Override
	public String getFileName() {
		return chemical ? "stfrChemicalPlant.json" : "stfrAssembly.json";
	}

	@Override
	public int inputItemLimit() {
		return target().inputItemLimit();
	}

	@Override
	public int inputFluidLimit() {
		return target().inputFluidLimit();
	}

	@Override
	public int outputItemLimit() {
		return target().outputItemLimit();
	}

	@Override
	public int outputFluidLimit() {
		return target().outputFluidLimit();
	}

	@Override
	public GenericRecipe instantiateRecipe(String name) {
		return new GenericRecipe(name);
	}

	@Override
	public void registerDefaults() {
		StfrCrafting.registerDefaults(chemical);
	}

	@Override
	public void register(GenericRecipe recipe) {
		String prefix = chemical ? "chem.stfr." : "ass.stfr.";
		if (!recipe.getInternalName().startsWith(prefix))
			throw new IllegalArgumentException("STFR recipe name must start with " + prefix);
		super.register(recipe);
		target().register(recipe);
	}

	@Override
	public void deleteRecipes() {
		for (GenericRecipe recipe : new ArrayList<>(recipeOrderedList))
			target().removeRecipeByName(recipe.getInternalName());
		super.deleteRecipes();
	}

	@Override
	public String getComment() {
		return "STFR machine recipes. Remove the leading underscore to enable customization. Uses"
				   + " the HBM recipe format; this file replaces only STFR recipes.";
	}
}

package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import com.hbm.handler.jei.JEIGenericRecipeHandler;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.reactor.StfrFluids;

final class StfrConversionCategory extends JEIGenericRecipeHandler {

	private final IDrawable icon;

	StfrConversionCategory(IGuiHelper helper) {
		super(
				helper,
				REACTOR,
				"jei.hbmstfr.structure_analyzer",
				new StfrDisplayRecipes(),
				new ItemStack(DESTABILIZER));
		icon = new StfrScaledIcon(
				helper.createDrawableIngredient(new ItemStack(STFRItems.STFR_REACTOR)), 1.25F);
		recipes.clear();
		recipes.add(new Conversion());
	}

	@Override
	public String getTitle() {
		return I18n.format("jei.hbmstfr.structure_analyzer");
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public String getModName() {
		return "STFR";
	}

	private final class Conversion extends JeiGenericRecipe {

		Conversion() {
			super(
					new GenericRecipe("stfr.digamma_conversion") {
						@Override
						public void printNEIExtras() {}
					},
					Collections.singletonList(
							Collections.singletonList(new ItemStack(ModItems.billet_polonium))),
					Collections.singletonList(
							Collections.singletonList(
									ItemFluidIcon.make(
											new com.hbm.inventory.fluid.FluidStack(
													StfrFluids.YARONITE, 96)))),
					new ItemStack[] {new ItemStack(DESTABILIZER)},
					null,
					0,
					0,
					0);
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			super.getIngredients(ingredients);
			// Equipment is indexed for usages but drawn in the machine position.
			ingredients.setInputLists(
					VanillaTypes.ITEM,
					Arrays.asList(
							inputs.get(0),
							Collections.singletonList(new ItemStack(DESTABILIZER))));
			ingredients.setOutput(
					VanillaTypes.FLUID,
					new FluidStack(StfrFluids.YARONITE.getFF(), 96));
		}
	}

	@Override
	public void setRecipe(
			IRecipeLayout layout, JeiGenericRecipe wrapper, IIngredients ingredients) {
		IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 47, 23);
		stacks.set(0, ingredients.getInputs(VanillaTypes.ITEM).get(0));
		stacks.init(1, true, 74, 29);
		stacks.set(1, new ItemStack(DESTABILIZER));
		stacks.init(2, false, 101, 23);
		stacks.set(2, ingredients.getOutputs(VanillaTypes.ITEM).get(0));
	}
}

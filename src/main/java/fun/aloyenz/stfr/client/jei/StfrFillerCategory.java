package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import com.hbm.handler.jei.JEIGenericRecipeHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import fun.aloyenz.stfr.item.ItemFuelCapsule;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.reactor.StfrFluids;

final class StfrFillerCategory extends JEIGenericRecipeHandler {
	private final IDrawable icon;

	StfrFillerCategory(IGuiHelper helper) {
		super(helper, FILLER, "jei.hbmstfr.capsule_filler", new StfrDisplayRecipes(),
				new ItemStack(CAPSULE_FILLER));
		icon = helper.createDrawableIngredient(new ItemStack(CAPSULE_FILLER));
		recipes.clear();
		add("deuterium", Fluids.DEUTERIUM.getFF(),
				ItemFluidIcon.make(new com.hbm.inventory.fluid.FluidStack(
						Fluids.DEUTERIUM, 100_000, 5)));
		add("water", Fluids.WATER.getFF(),
				ItemFluidIcon.make(new com.hbm.inventory.fluid.FluidStack(
						Fluids.WATER, 100_000, 1)),
				ItemFluidIcon.make(new com.hbm.inventory.fluid.FluidStack(
						StfrFluids.LIQUID_NITROGEN, 10_000, 5)));
		FluidType spaceNitrogen = StfrFluids.spaceNitrogen();
		if (spaceNitrogen != null)
			add("water_space", Fluids.WATER.getFF(),
					ItemFluidIcon.make(new com.hbm.inventory.fluid.FluidStack(
							Fluids.WATER, 100_000, 1)),
					ItemFluidIcon.make(new com.hbm.inventory.fluid.FluidStack(
							spaceNitrogen, 10_000, 5)));
	}

	private void add(String name, Fluid fluid, ItemStack... inputs) {
		ItemStack filled = new ItemStack(STFRItems.FUEL_CAPSULE);
		if (ItemFuelCapsule.fillInternal(
				filled, new FluidStack(fluid, 100_000)) <= 0) return;
		List<List<ItemStack>> items = new ArrayList<>();
		items.add(Collections.singletonList(new ItemStack(STFRItems.FUEL_CAPSULE)));
		for (ItemStack input : inputs) items.add(Collections.singletonList(input));
		recipes.add(new JeiGenericRecipe(
				new GenericRecipe("stfr.capsule_filler." + name).setup(100, 0),
				items,
				Collections.singletonList(Collections.singletonList(filled)),
				new ItemStack[] {new ItemStack(CAPSULE_FILLER)}, null, 0, 0, 0));
	}

	@Override public IDrawable getIcon() { return icon; }
	@Override public String getModName() { return "STFR"; }
}

package fun.aloyenz.stfr.crafting;

import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.DFCRecipes;
import com.hbm.items.ModItems;
import net.minecraft.item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistry;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.Tags;

/** Only HBM recipes are removed; the existing items and blocks remain registered. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class HbmDarkFusionCrafting {

	private static final Set<String> CRAFTING_OUTPUTS = new HashSet<>(Arrays.asList(
			"ams_core_sing", "ams_core_wormhole", "ams_core_eyeofharmony",
			"ams_core_thingy"));

	private HbmDarkFusionCrafting() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void removeWorkbenchRecipes(RegistryEvent.Register<IRecipe> event) {
		if (!StfrConfig.disableHbmDarkFusionCrafting) return;
		ForgeRegistry<IRecipe> registry = (ForgeRegistry<IRecipe>) event.getRegistry();
		for (Map.Entry<ResourceLocation, IRecipe> entry : new ArrayList<>(registry.getEntries())) {
			ResourceLocation name = entry.getKey();
			ItemStack output = entry.getValue().getRecipeOutput();
			if (!"hbm".equals(name.getNamespace()) || output.isEmpty()
					|| output.getItem().getRegistryName() == null) continue;
			ResourceLocation itemName = output.getItem().getRegistryName();
			if ("hbm".equals(itemName.getNamespace())
					&& CRAFTING_OUTPUTS.contains(itemName.getPath())) registry.remove(name);
		}
	}

	public static void disableMachineRecipes() {
		if (StfrConfig.disableHbmDfcYaroniteRecipe) removeDfcOutput(ModItems.billet_yharonite);
		if (!StfrConfig.disableHbmDarkFusionCrafting) return;
		for (String name : Arrays.asList("plsm.dfccore", "plsm.dfcemitter", "plsm.dfcreceiver",
				"plsm.dfcinjector", "plsm.dfcstabilizer"))
			PlasmaForgeRecipes.INSTANCE.removeRecipeByName(name);
	}
	static void removeDfcOutput(Item item) {
		boolean removed = DFCRecipes.dfcRecipes.entrySet().removeIf(entry ->
				entry.getValue()[1] instanceof ItemStack output
						&& !output.isEmpty() && output.getItem() == item);
		if (removed) DFCRecipes.jeiDFCRecipes = null;
	}
}

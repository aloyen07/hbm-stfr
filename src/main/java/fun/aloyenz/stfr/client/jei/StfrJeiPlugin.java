package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;

import com.hbm.handler.jei.JEIConfig;
import com.hbm.handler.jei.JeiRecipes.JeiUniversalRecipe;
import com.hbm.items.ModItems;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import fun.aloyenz.stfr.crafting.StfrConstructionMaterials;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.reactor.StfrPoloniumFeed;

/** Optional, discovered by JEI/HEI only; never referenced by common/server code. */
@JEIPlugin
@SideOnly(Side.CLIENT)
public final class StfrJeiPlugin implements IModPlugin {

	static final String REACTOR = "hbmstfr.structure_analyzer";
	static final String TOOLS = "hbmstfr.manual_processing";
	static final String INJECTOR = "hbmstfr.polonium_injector";
	static final String FILLER = "hbmstfr.capsule_filler";
	private StfrConversionCategory conversionCategory;
	private StfrManualCategory manualCategory;
	private StfrFillerCategory fillerCategory;

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		conversionCategory = new StfrConversionCategory(gui);
		manualCategory = new StfrManualCategory(gui);
		fillerCategory = new StfrFillerCategory(gui);
		registration.addRecipeCategories(
				conversionCategory,
				manualCategory,
				fillerCategory,
				new StfrInjectorCategory(gui, INJECTOR, new ItemStack(SOLID_INJECTOR)));
	}

	@Override
	public void register(IModRegistry registry) {
		registry.addIngredientInfo(new ItemStack(STFR_FROZEN_PLASMA_LENS),
				VanillaTypes.ITEM, "jei.hbmstfr.lens_thawing");
		registry.addIngredientInfo(new ItemStack(STFR_PLASMA_LENS),
				VanillaTypes.ITEM, "jei.hbmstfr.lens_thawing");
		for (Item wand :
				new Item[] {
					STFRItems.REACTOR_MISC_WAND,
					STFRItems.REACTOR_DEBUG_WAND,
					STFRItems.REACTOR_ROTOR_WAND,
					STFRItems.REACTOR_PORT_WAND,
					STFRItems.REACTOR_CONTROL_WAND,
					STFRItems.REACTOR_SIMULATION_WAND,
					STFRItems.CORE_LIGHT_WAND,
					STFRItems.STFR_SCREAM_WAND,
					STFRItems.REACTOR_DESTRUCTION_WAND,
					STFRItems.REACTOR_MACHINE_DEBUG_WAND,
					STFRItems.REACTOR_RESET_WAND
				})
			registry.getJeiHelpers()
					.getIngredientBlacklist()
					.addIngredientToBlacklist(new ItemStack(wand));
		registry.handleRecipes(Card.class, card -> card, INJECTOR);
		registry.addRecipeCatalyst(new ItemStack(STRUCTURE_ANALYZER), REACTOR);
		registry.addRecipeCatalyst(new ItemStack(STRUCTURE_ANALYZER), JEIConfig.CONSTRUCTION);
		registry.addRecipeCatalyst(new ItemStack(STFRItems.STFR_REACTOR), JEIConfig.CONSTRUCTION);
		registry.addRecipeCatalyst(new ItemStack(SOLID_INJECTOR), INJECTOR);
		registry.addRecipeCatalyst(new ItemStack(CAPSULE_FILLER), FILLER);
		registry.addRecipeCatalyst(new ItemStack(ModItems.blowtorch), TOOLS);
		registry.addRecipeCatalyst(new ItemStack(ModItems.boltgun), TOOLS);
		registry.addRecipes(conversionCategory.getRecipes(), REACTOR);
		registry.addRecipes(manualCategory.getRecipes(), TOOLS);
		registry.addRecipes(fillerCategory.getRecipes(), FILLER);
		registry.addRecipes(constructionRecipes(), JEIConfig.CONSTRUCTION);
		List<Card> feeds = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String ore : OreDictionary.getOreNames())
			for (ItemStack stack : OreDictionary.getOres(ore)) addFeed(feeds, seen, stack);
		for (Item item :
				new Item[] {
					ModItems.insert_polonium,
					ModItems.billet_po210be,
					ModItems.rbmk_fuel_po210be,
					ModItems.pellet_rtg_polonium
				}) addFeed(feeds, seen, new ItemStack(item));
		for (int tier = 0; tier < 5; tier++)
			addFeed(feeds, seen, new ItemStack(ModItems.rbmk_pellet_po210be, 1, tier));
		registry.addRecipes(feeds, INJECTOR);
		registry.addIngredientInfo(
				new ItemStack(SOLID_INJECTOR), VanillaTypes.ITEM, "jei.hbmstfr.injector.info");
		registry.addIngredientInfo(
				new ItemStack(STFRItems.YARONITE_DUST_TINY),
				VanillaTypes.ITEM,
				"jei.hbmstfr.dust.info");
	}

	/** HBM's native machine recipe layout with pressurized fluid input icons. */

	static void addFeed(List<Card> cards, Set<String> seen, ItemStack stack) {
		int volume = StfrPoloniumFeed.volume(stack);
		if (volume <= 0 || !seen.add(stack.getItem().getRegistryName() + ":" + stack.getMetadata()))
			return;
		ItemStack input = stack.copy();
		input.setCount(1);
		Card card =
				new Card(
						INJECTOR,
						Collections.singletonList(Collections.singletonList(input)),
						ItemStack.EMPTY,
						"jei.hbmstfr.injector");
		card.volume = volume;
		cards.add(card);
	}

	public static final class Card implements IRecipeWrapper {

		final String uid, text;
		final List<List<ItemStack>> inputs;
		final ItemStack output;
		int volume;

		Card(String uid, List<List<ItemStack>> inputs, ItemStack output, String text) {
			this.uid = uid;
			this.inputs = inputs;
			this.output = output;
			this.text = text;
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			ingredients.setInputLists(VanillaTypes.ITEM, inputs);
			if (!output.isEmpty()) ingredients.setOutput(VanillaTypes.ITEM, output);
		}

		@Override
		public void drawInfo(Minecraft mc, int width, int height, int mouseX, int mouseY) {
			label(mc, volume + " mB / 16000 mB", 35, 18);
			line(mc, text + ".settings", 0, 51);
			line(mc, text + ".redstone", 0, 65);
			line(mc, text + ".depletion", 0, 91);
		}

		static void line(Minecraft mc, String key, int x, int y) {
			label(mc, I18n.format(key), x, y);
		}

		static void label(Minecraft mc, String text, int x, int y) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, 0);
			GlStateManager.scale(.75F, .75F, 1F);
			mc.fontRenderer.drawString(text, 0, 0, 0x404040);
			GlStateManager.popMatrix();
		}
	}

	/** HBM's native assembly layout, with the hand tool in the machine slot. */

	/** Use HBM's real machine layout, slot artwork and normal font sizing. */

	static List<JeiUniversalRecipe> constructionRecipes() {
		List<List<ItemStack>> materials = StfrConstructionMaterials.ingredients();
		List<JeiUniversalRecipe> pages = new ArrayList<>();
		int total = (materials.size() + 8) / 9;
		for (int start = 0; start < materials.size(); start += 9) {
			int page = start / 9 + 1;
			pages.add(new JeiUniversalRecipe(
					new ArrayList<>(materials.subList(start, Math.min(start + 9, materials.size()))),
					new ItemStack[] {new ItemStack(STFRItems.STFR_REACTOR)},
					new ItemStack[] {new ItemStack(ModItems.blowtorch), new ItemStack(ModItems.boltgun)}) {
				@Override
				public void drawInfo(Minecraft mc, int width, int height, int mouseX, int mouseY) {
					super.drawInfo(mc, width, height, mouseX, mouseY);
					mc.fontRenderer.drawString("STFR", 104, 9, 0x404040);
					mc.fontRenderer.drawString(page + "/" + total, 106, 48, 0x404040);
				}
			});
		}
		return pages;
	}

}

package fun.aloyenz.stfr.crafting;

import com.hbm.blocks.generic.BlockMeta;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** A construction bill derived from the reference structure and placed-block conversions. */
public final class StfrConstructionMaterials {

	private StfrConstructionMaterials() {}

	public static List<List<ItemStack>> ingredients() {
		Map<String, StfrManualRecipes.ManualRecipe> conversions = new HashMap<>();
		for (StfrManualRecipes.ManualRecipe recipe : StfrManualRecipes.INSTANCE.recipeOrderedList)
			conversions.put(key(recipe.outputItem[0].getSingle()), recipe);

		Map<String, Entry> totals = new LinkedHashMap<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			Block block = cell.expected.getBlock();
			// HBM's BlockMeta drops its state meta via getDrops(), but leaves
			// damageDropped() at Block's default (zero). Preserve the item variant.
			int itemMeta = block instanceof BlockMeta
					? block.getMetaFromState(cell.expected)
					: block.damageDropped(cell.expected);
			ItemStack stack = new ItemStack(block, 1, itemMeta);
			if (stack.isEmpty())
				throw new IllegalStateException("No construction item for " + block.getRegistryName());
			add(stack, 1, conversions, totals, new HashSet<>());
		}

		List<List<ItemStack>> result = new ArrayList<>();
		for (Entry entry : totals.values()) {
			List<ItemStack> alternatives = new ArrayList<>();
			for (ItemStack variant : entry.variants) {
				ItemStack display = variant.copy();
				display.setCount(Math.toIntExact(entry.count));
				ItemStackUtil.addStackSizeLabel(display);
				alternatives.add(display);
			}
			result.add(Collections.unmodifiableList(alternatives));
		}
		return Collections.unmodifiableList(result);
	}

	private static void add(
			ItemStack stack,
			long count,
			Map<String, StfrManualRecipes.ManualRecipe> conversions,
			Map<String, Entry> totals,
			Set<String> visiting) {
		String itemKey = key(stack);
		StfrManualRecipes.ManualRecipe recipe = conversions.get(itemKey);
		if (recipe == null) {
			accumulate(itemKey, Collections.singletonList(stack), count, totals);
			return;
		}
		if (!visiting.add(itemKey))
			throw new IllegalStateException("Circular STFR manual recipe for " + itemKey);
		for (AStack ingredient : recipe.inputItem) {
			if (ingredient == null) continue;
			if (ingredient instanceof OreDictStack) {
				OreDictStack ore = (OreDictStack) ingredient;
				accumulate("ore:" + ore.name, ore.extractForJEI(), count * ore.count(), totals);
			} else {
				ItemStack input = ingredient.getStack();
				if (input == null || input.isEmpty())
					throw new IllegalStateException("Empty ingredient in " + recipe.getInternalName());
				add(input, count * ingredient.count(), conversions, totals, visiting);
			}
		}
		visiting.remove(itemKey);
	}

	private static void accumulate(
			String key, List<ItemStack> variants, long count, Map<String, Entry> totals) {
		if (variants.isEmpty()) throw new IllegalStateException("No items for STFR material " + key);
		Entry entry = totals.get(key);
		if (entry == null) totals.put(key, new Entry(variants, count));
		else entry.count += count;
	}

	private static String key(ItemStack stack) {
		return stack.getItem().getRegistryName() + ":" + stack.getMetadata()
				+ ":" + (stack.hasTagCompound() ? stack.getTagCompound() : "");
	}

	private static final class Entry {
		final List<ItemStack> variants;
		long count;

		Entry(List<ItemStack> variants, long count) {
			this.variants = variants;
			this.count = count;
		}
	}
}

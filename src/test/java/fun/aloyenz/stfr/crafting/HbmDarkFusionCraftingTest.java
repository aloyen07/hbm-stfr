package fun.aloyenz.stfr.crafting;

import static org.junit.jupiter.api.Assertions.*;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.DFCRecipes;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

final class HbmDarkFusionCraftingTest {
	@Test
	void removesOnlyMatchingDfcOutputsAndInvalidatesJeiCache() {
		Bootstrap.register();
		LinkedHashMap<ComparableStack, Object[]> saved = DFCRecipes.dfcRecipes;
		List<DFCRecipes.DFCRecipe> savedJei = DFCRecipes.jeiDFCRecipes;
		try {
			DFCRecipes.dfcRecipes = new LinkedHashMap<>();
			DFCRecipes.jeiDFCRecipes = null;
			DFCRecipes.setRecipe(100L, Items.STICK, new ItemStack(Items.DIAMOND));
			DFCRecipes.setRecipe(200L, Items.APPLE, new ItemStack(Items.DIAMOND, 2));
			DFCRecipes.setRecipe(300L, Items.COAL, new ItemStack(Items.EMERALD));
			assertEquals(3, DFCRecipes.getDFCRecipes().size());
			HbmDarkFusionCrafting.removeDfcOutput(Items.DIAMOND);
			assertTrue(DFCRecipes.getOutput(new ItemStack(Items.STICK)).isEmpty());
			assertTrue(DFCRecipes.getOutput(new ItemStack(Items.APPLE)).isEmpty());
			assertEquals(Items.EMERALD, DFCRecipes.getOutput(new ItemStack(Items.COAL)).getItem());
			assertNull(DFCRecipes.jeiDFCRecipes);
			assertEquals(1, DFCRecipes.getDFCRecipes().size());
		} finally {
			DFCRecipes.dfcRecipes = saved;
			DFCRecipes.jeiDFCRecipes = savedJei;
		}
	}
}

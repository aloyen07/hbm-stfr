package fun.aloyenz.stfr.block;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.RecipesCommon.AStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.crafting.StfrManualRecipes;

final class ManualProcessingOverlay {

	private ManualProcessingOverlay() {}

	@SideOnly(Side.CLIENT)
	static void print(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null) return;
		for (ItemStack held :
				new ItemStack[] {mc.player.getHeldItemMainhand(), mc.player.getHeldItemOffhand()}) {
			ToolType tool = ToolType.getType(held);
			StfrManualRecipes.ManualRecipe recipe =
					StfrManualRecipes.find(world.getBlockState(pos).getBlock(), tool);
			if (recipe == null) continue;
			List<String> lines = new ArrayList<>();
			for (AStack material : StfrManualRecipes.materials(recipe)) {
				List<ItemStack> variants = material.extractForJEI();
				if (!variants.isEmpty())
					lines.add(variants.get(0).getDisplayName() + " x" + material.count());
			}
			ILookOverlay.printGeneric(event, held.getDisplayName(), 0xffff00, 0x404000, lines);
			return;
		}
	}
}

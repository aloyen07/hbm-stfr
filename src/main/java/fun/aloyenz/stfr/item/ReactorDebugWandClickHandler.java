package fun.aloyenz.stfr.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;

/** Handle the wand before interactive HBM blocks consume the right click. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class ReactorDebugWandClickHandler {

	private ReactorDebugWandClickHandler() {}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getEntityPlayer().getHeldItem(event.getHand());
		if (!(held.getItem() instanceof ItemReactorDebugWand)
				&& !(held.getItem() instanceof ItemReactorPortWand)
				&& !(held.getItem() instanceof ItemReactorSimulationWand)) return;
		EnumActionResult result =
				held.getItem()
						.onItemUse(
								event.getEntityPlayer(),
								event.getWorld(),
								event.getPos(),
								event.getHand(),
								event.getFace(),
								event.getHitVec() == null
										? 0.5F
										: (float) (event.getHitVec().x - event.getPos().getX()),
								event.getHitVec() == null
										? 0.5F
										: (float) (event.getHitVec().y - event.getPos().getY()),
								event.getHitVec() == null
										? 0.5F
										: (float) (event.getHitVec().z - event.getPos().getZ()));
		if (result != EnumActionResult.PASS) {
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		ItemStack held = event.getEntityPlayer().getHeldItemMainhand();
		if (!(held.getItem() instanceof ItemReactorSimulationWand)) return;
		EnumActionResult result =
				((ItemReactorSimulationWand) held.getItem())
						.click(
								event.getEntityPlayer(),
								event.getWorld(),
								event.getPos(),
								EnumHand.MAIN_HAND,
								true);
		if (result != EnumActionResult.PASS) event.setCanceled(true);
	}
}

package fun.aloyenz.stfr.reactor;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import fun.aloyenz.stfr.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrWorldTicker {

	private StfrWorldTicker() {}

	@SubscribeEvent
	public static void onRepair(PlayerInteractEvent.RightClickBlock event) {
		if (event.getWorld().isRemote
				|| event.getEntityPlayer().isSpectator()
				|| event.getHand() != EnumHand.MAIN_HAND) return;
		ItemStack held = event.getItemStack();
		if (held.isEmpty()
				|| held.getItem().getRegistryName() == null
				|| !held.getItem().getRegistryName().getPath().contains("wrench")) return;
		for (StfrReactor reactor : StfrManager.get(event.getWorld()).reactors())
			if (reactor.maintenance().repair(event.getWorld(), event.getPos())) {
				StfrManager.get(event.getWorld()).markDirty();
				event.setCancellationResult(EnumActionResult.SUCCESS);
				event.setCanceled(true);
				return;
			}
	}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase == TickEvent.Phase.END && !event.world.isRemote)
			StfrManager.get(event.world).tick(event.world);
	}
}

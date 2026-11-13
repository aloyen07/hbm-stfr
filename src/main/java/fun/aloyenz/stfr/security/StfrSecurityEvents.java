package fun.aloyenz.stfr.security;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Enforce protection on normal world interactions as well as custom packets. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrSecurityEvents {

	private static int right(TileEntityLogicDevice tile) {
		return tile.kind() == BlockLogicDevice.Kind.PRIVILEGES
				? StfrAccessPolicy.ADMIN
				: StfrAccessPolicy.CONTROL;
	}

	@SubscribeEvent
	public static void breakBlock(BlockEvent.BreakEvent e) {
		if (e.getWorld().isRemote) return;
		TileEntity tile = e.getWorld().getTileEntity(e.getPos());
		if (tile instanceof TileEntityLogicDevice
				&& !StfrSecurity.allows(
						e.getPlayer(),
						e.getWorld(),
						e.getPos(),
						right((TileEntityLogicDevice) tile))) {
			e.setCanceled(true);
			StfrSecurity.message(e.getPlayer(), "denied");
		}
	}

	@SubscribeEvent
	public static void interact(PlayerInteractEvent.RightClickBlock e) {
		if (e.getWorld().isRemote) return;
		// These two items perform their own CONNECT/DETONATE checks.
		if (e.getItemStack().getItem() == STFRItems.KEY_CARD
				|| e.getItemStack().getItem() == STFRItems.DETONATOR) return;
		TileEntity tile = e.getWorld().getTileEntity(e.getPos());
		if (tile instanceof TileEntityLogicDevice
				&& !StfrSecurity.allows(
						e.getEntityPlayer(),
						e.getWorld(),
						e.getPos(),
						right((TileEntityLogicDevice) tile))) {
			e.setCanceled(true);
			StfrSecurity.message(e.getEntityPlayer(), "denied");
		}
	}
}

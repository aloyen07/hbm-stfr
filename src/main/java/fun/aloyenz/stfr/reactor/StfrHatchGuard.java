package fun.aloyenz.stfr.reactor;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;

/** Reject manual clicks on hatches during reactor locking and locked operation. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrHatchGuard {

	private StfrHatchGuard() {}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
		if (event.getWorld().isRemote) return;
		BlockPos clicked = event.getPos();
		for (StfrReactor reactor : StfrManager.get(event.getWorld()).reactors()) {
			BlockPos local =
					clicked.subtract(reactor.center.subtract(StfrStructureTemplate.CENTER));
			if (local.getX() < 0
					|| local.getX() >= 29
					|| local.getY() < 0
					|| local.getY() >= 29
					|| local.getZ() < 0
					|| local.getZ() >= 29) continue;
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			if (cell == null
					|| !reactor.protectsHatch(cell.defaultRole)
					|| event.getWorld().getBlockState(clicked).getBlock()
							!= cell.expected.getBlock()) continue;
			event.setCanceled(true);
			if (event.getEntityPlayer() instanceof EntityPlayerMP)
				((EntityPlayerMP) event.getEntityPlayer())
						.connection.sendPacket(
								new SPacketBlockChange((WorldServer) event.getWorld(), clicked));
			event.getEntityPlayer()
					.sendMessage(
							new TextComponentTranslation("message.hbmstfr.port_wand.hatch_locked"));
			return;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBreak(BlockEvent.BreakEvent event) {
		if (event.getWorld().isRemote || event.getState().getBlock() != STFRBlocks.FUEL_SLOT)
			return;
		TileEntity tile = event.getWorld().getTileEntity(event.getPos());
		if (!(tile instanceof TileEntityFuelSlot)
				|| ((TileEntityFuelSlot) tile).getCapsule().isEmpty()) return;
		StfrManager manager = StfrManager.get((World) event.getWorld());
		StfrReactor reactor = manager.forFuelSlot(event.getPos());
		if (reactor == null) return;
		reactor.refresh((World) event.getWorld());
		if (!reactor.canRemoveCapsule(event.getPos())) {
			event.setCanceled(true);
			event.getPlayer()
					.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.fuel_slot.concurrent_refueling"));
		}
	}
}

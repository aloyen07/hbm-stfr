package fun.aloyenz.stfr.logic;

import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;

/** A removed frame/cable or critical reactor block takes effect in the same tick. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrLogicNetworkEvents {

	private StfrLogicNetworkEvents() {}

	@SubscribeEvent
	public static void blockChanged(BlockEvent event) {
		StfrLogicNetwork.invalidate(event.getWorld());
	}

	@SubscribeEvent
	public static void chunkChanged(ChunkEvent event) {
		StfrLogicNetwork.invalidate(event.getWorld());
	}

	@SubscribeEvent
	public static void worldUnloaded(WorldEvent.Unload event) {
		StfrLogicNetwork.invalidate(event.getWorld());
	}
}

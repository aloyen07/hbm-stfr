package fun.aloyenz.stfr.reactor;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import fun.aloyenz.stfr.STFR;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Keeps the 29x29 reactor footprint loaded while its hatch lock is active. */
public final class StfrChunkLoader {

	private StfrChunkLoader() {}

	public static ForgeChunkManager.Ticket request(World world, BlockPos center) {
		ForgeChunkManager.Ticket ticket =
				ForgeChunkManager.requestTicket(
						STFR.instance, world, ForgeChunkManager.Type.NORMAL);
		if (ticket == null) return null;
		ticket.getModData().setLong("center", center.toLong());
		forceFootprint(ticket, center);
		return ticket;
	}

	public static void restoreTickets(List<ForgeChunkManager.Ticket> tickets, World world) {
		StfrManager manager = StfrManager.get(world);
		for (ForgeChunkManager.Ticket ticket : tickets) {
			if (!ticket.getModData().hasKey("center")) {
				ForgeChunkManager.releaseTicket(ticket);
				continue;
			}
			BlockPos center = BlockPos.fromLong(ticket.getModData().getLong("center"));
			StfrReactor reactor = manager.get(center);
			if (reactor == null || reactor.lockState() == StfrReactor.LockState.UNLOCKED) {
				ForgeChunkManager.releaseTicket(ticket);
				continue;
			}
			manager.bindTicket(center, ticket);
			forceFootprint(ticket, center);
		}
	}

	private static void forceFootprint(ForgeChunkManager.Ticket ticket, BlockPos center) {
		BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
		int x0 = origin.getX() >> 4;
		int x1 = (origin.getX() + 28) >> 4;
		int z0 = origin.getZ() >> 4;
		int z1 = (origin.getZ() + 28) >> 4;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) ForgeChunkManager.forceChunk(ticket, new ChunkPos(x, z));
		}
	}
}

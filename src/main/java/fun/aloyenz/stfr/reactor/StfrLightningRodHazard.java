package fun.aloyenz.stfr.reactor;

import com.hbm.lib.ModDamageSource;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;

final class StfrLightningRodHazard {

	private StfrLightningRodHazard() {}

	static int interval(int brokenRods) {
		return brokenRods <= 0 ? Integer.MAX_VALUE : Math.max(1, 200 / brokenRods);
	}

	static float damage(int brokenRods) {
		return 2F * Math.max(0, brokenRods);
	}

	static void tick(StfrReactor reactor, World world) {
		if (world.isRemote || reactor.componentStatuses.isEmpty()) return;
		long now = world.getTotalWorldTime();
		Map<UUID, Long> lastShock = reactor.lightningShockTicks;
		lastShock.entrySet().removeIf(entry -> now - entry.getValue() > 200);
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		for (EntityPlayer player : world.playerEntities) {
			if (player.isSpectator() || player.capabilities.isFlying || !player.onGround
					|| player.getDistanceSq(reactor.center) > 32D * 32D) continue;
			BlockPos foot = new BlockPos(player.posX,
					player.getEntityBoundingBox().minY - .08D, player.posZ);
			BlockPos local = foot.subtract(origin);
			if (!reactor.rotorPositions.contains(local.toLong())) continue;
			EnumFacing side = StfrRadialSymmetry.sourceDirection(local);
			StfrComponentStatus status = reactor.componentStatuses.get(side);
			if (status == null || status.lightningRodFailures <= 0) continue;
			if (world.getBlockState(foot).getBlock() == STFRBlocks.ROTOR_PLACEHOLDER) {
				TileEntity tile = world.getTileEntity(foot);
				if (!(tile instanceof TileEntityRotorPlaceholder)
						|| !((TileEntityRotorPlaceholder) tile).center().equals(reactor.center))
					continue;
			} else {
				StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
				if (cell == null || !StfrStructureValidator.matchesCell(cell,
						cell.defaultRole, world.getBlockState(foot), world, foot)) continue;
			}
			long previous = lastShock.getOrDefault(player.getUniqueID(), Long.MIN_VALUE / 2);
			if (now - previous < interval(status.lightningRodFailures)) continue;
			player.attackEntityFrom(ModDamageSource.electricity,
					damage(status.lightningRodFailures));
			lastShock.put(player.getUniqueID(), now);
		}
	}
}

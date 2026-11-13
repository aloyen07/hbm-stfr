package fun.aloyenz.stfr.reactor;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;

final class StfrDirtyShutdown {

	private static final double MAX_RADIATION = 10_000D;

	private StfrDirtyShutdown() {}

	/** A damaged vessel must not bypass an active contingency and its recovery protocol. */
	static boolean allowedFrom(RunState state) {
		return state == RunState.ONLINE || state == RunState.STARTUP_CONFIRMED
				|| state == RunState.SHUTDOWN;
	}

	static void begin(StfrReactor reactor, World world) {
		if (!allowedFrom(reactor.runState)) return;
		reactor.dirtyShutdown = true;
		reactor.dirtyRadiationTicks = 0;
		reactor.emergency.stopMusic(world);
		reactor.emergency.reset();
		reactor.failure.reset();
		if (reactor.digammaActive) StfrDigammaController.finishDigammaShutdown(reactor, world);
		reactor.setAutopilotActive(false);
		reactor.keyOn = false;
		reactor.enter(RunState.SHUTDOWN);
		StfrAnnouncementController.announce(reactor, world, "shutdown.dirty", Severity.CRITICAL, null);
	}

	static void coreDestroyed(StfrReactor reactor) {
		reactor.dirtyRadiationTicks = StfrConfig.dirtyShutdownSourceDurationSeconds * 20 + 1;
	}

	static void tick(StfrReactor reactor, World world) {
		if (!(world instanceof WorldServer)) return;
		WorldServer server = (WorldServer) world;
		if (reactor.dirtyShutdown && reactor.runState == RunState.SHUTDOWN
				&& reactor.stateTicks < 1140) {
			if (reactor.lockState == LockState.LOCKED) reactor.lockState = LockState.UNLOCKING;
		}
		if (reactor.dirtyShutdown && reactor.dirtyRadiationTicks > 0
				&& !reactor.core.exists() && reactor.runState == RunState.SHUTDOWN
				&& reactor.stateTicks < 1140) {
			for (int i = 0; i < 16; i++)
				server.spawnParticle(EnumParticleTypes.END_ROD, true,
					reactor.center.getX() - 14 + world.rand.nextDouble() * 29,
					reactor.center.getY() - 14 + world.rand.nextDouble() * 29,
					reactor.center.getZ() - 14 + world.rand.nextDouble() * 29,
					1, 0, 0, 0, .01);
		}
		if (reactor.dirtyRadiationTicks <= 0 || reactor.core.exists()) return;
		reactor.dirtyRadiationTicks--;
		if (reactor.dirtyRadiationTicks % 100 != 0) return;
		emit(server, reactor);
	}

	private static void emit(WorldServer server, StfrReactor reactor) {
		int radius = StfrConfig.dirtyShutdownRadiationRadius;
		int gridRadius = (radius + 15) / 16;
		for (int ix = -gridRadius; ix <= gridRadius; ix++)
			for (int iy = -gridRadius; iy <= gridRadius; iy++)
				for (int iz = -gridRadius; iz <= gridRadius; iz++) {
					int x = ix * 16, y = iy * 16, z = iz * 16;
					int distanceSquared = x * x + y * y + z * z;
					if (distanceSquared > radius * radius) continue;
					BlockPos target = reactor.center.add(x, y, z);
					if (target.getY() < 0 || target.getY() > 255 || !server.isBlockLoaded(target))
						continue;
					for (EnumFacing side : EnumFacing.VALUES) {
						BlockPos source = reactor.center.offset(side, 15);
						if (!shielded(server, source, target)) {
							RadiationSystemNT.incrementRad(server, target,
									StfrConfig.dirtyShutdownRadiationStrength
											/ (1D + distanceSquared / 512D),
									MAX_RADIATION);
							break;
						}
					}
				}
	}

	private static boolean shielded(WorldServer world, BlockPos start, BlockPos end) {
		if (!world.isBlockLoaded(start)) return true;
		Block sourceBlock = world.getBlockState(start).getBlock();
		if (sourceBlock instanceof IRadResistantBlock
				&& ((IRadResistantBlock) sourceBlock).isRadResistant(world, start)) return true;
		int dx = end.getX() - start.getX();
		int dy = end.getY() - start.getY();
		int dz = end.getZ() - start.getZ();
		int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
		if (steps == 0) return false;
		BlockPos previous = start;
		for (int i = 1; i <= steps; i++) {
			BlockPos pos = start.add(
					Math.round(dx * i / (float) steps),
					Math.round(dy * i / (float) steps),
					Math.round(dz * i / (float) steps));
			if (pos.equals(previous)) continue;
			if (!world.isBlockLoaded(pos)) return true;
			Block block = world.getBlockState(pos).getBlock();
			if (block instanceof IRadResistantBlock
					&& ((IRadResistantBlock) block).isRadResistant(world, pos)) return true;
			previous = pos;
		}
		return false;
	}
}

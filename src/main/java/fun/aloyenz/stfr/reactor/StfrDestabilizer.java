package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Resolves the physical, redstone-powered destabilizer which owns a reactor transition. */
public final class StfrDestabilizer {

	private StfrDestabilizer() {}

	public static void keepAlive(World world, TileEntityLogicDevice device) {
		if (!world.isBlockPowered(device.getPos())) return;
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspect(world, device.getPos());
		if (net.ioAvailable && net.reactor != null)
			net.reactor.destabilizerHeartbeat(world, device.getPos());
	}

	public static boolean powered(World world, StfrReactor reactor) {
		return poweredPosition(world, reactor) != null;
	}

	/** The armed device can keep running if a casing blast cuts only its data cable. */
	public static boolean powered(World world, StfrReactor reactor, BlockPos armedPosition) {
		if (armedPosition != null) {
			TileEntity tile = world.getTileEntity(armedPosition);
			if (tile instanceof TileEntityLogicDevice
					&& !tile.isInvalid()
					&& ((TileEntityLogicDevice) tile).kind() == BlockLogicDevice.Kind.DESTABILIZER
					&& world.isBlockPowered(armedPosition)) return true;
		}
		return powered(world, reactor);
	}

	public static BlockPos poweredPosition(World world, StfrReactor reactor) {
		for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
			if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
			TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
			if (device.kind() != BlockLogicDevice.Kind.DESTABILIZER
					|| !world.isBlockPowered(tile.getPos())) continue;
			StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspect(world, tile.getPos());
			if (net.ioAvailable && net.reactor == reactor) return tile.getPos().toImmutable();
		}
		return null;
	}
}

package fun.aloyenz.stfr.logic;

import java.util.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** A vertical rack shares energy; each physical member is billed exactly once per tick. */
public final class StfrMainframePower {

	public static final long BUFFER_PER_BLOCK = 100000L;
	public static final int CREATIVE_SLOTS = 9999;

	public static int slots(boolean creative) {
		return creative ? CREATIVE_SLOTS : StfrConfig.mainframeSlots;
	}

	public static int energyCost(boolean creative) {
		return creative ? 0 : StfrConfig.mainframePowerPerTick;
	}

	private StfrMainframePower() {}

	private static boolean isFrame(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return false;
		TileEntity tile = world.getTileEntity(pos);
		return tile instanceof TileEntityLogicDevice
				&& ((TileEntityLogicDevice) tile).kind() == BlockLogicDevice.Kind.MAINFRAME;
	}

	public static List<TileEntityLogicDevice> column(TileEntityLogicDevice source) {
		World world = source.getWorld();
		if (world == null) return Collections.singletonList(source);
		BlockPos bottom = source.getPos();
		while (bottom.getY() > 0 && isFrame(world, bottom.down())) bottom = bottom.down();
		List<TileEntityLogicDevice> result = new ArrayList<>();
		for (BlockPos p = bottom; p.getY() < world.getHeight() && isFrame(world, p); p = p.up())
			result.add((TileEntityLogicDevice) world.getTileEntity(p));
		return result;
	}

	public static long stored(TileEntityLogicDevice source) {
		long total = 0;
		for (TileEntityLogicDevice member : column(source)) total += member.mainframeStoredEnergy();
		return total;
	}

	public static long capacity(TileEntityLogicDevice source) {
		return BUFFER_PER_BLOCK * column(source).size();
	}

	public static void setStored(TileEntityLogicDevice source, long amount) {
		distribute(column(source), amount);
	}

	private static void distribute(List<TileEntityLogicDevice> members, long amount) {
		amount = Math.max(0, Math.min(BUFFER_PER_BLOCK * members.size(), amount));
		for (TileEntityLogicDevice member : members) {
			long part = Math.min(BUFFER_PER_BLOCK, amount);
			member.setMainframeStoredEnergy(part);
			amount -= part;
		}
	}

	public static void tick(TileEntityLogicDevice source) {
		World world = source.getWorld();
		if (world == null || world.isRemote) return;
		billTick(column(source), world.getTotalWorldTime());
	}

	public static void billTick(List<TileEntityLogicDevice> members, long tick) {
		long remaining = 0;
		for (TileEntityLogicDevice member : members) remaining += member.mainframeStoredEnergy();
		boolean changed = false;
		for (TileEntityLogicDevice member : members) {
			if (member.mainframePowerTick() == tick) continue;
			int cost = member.mainframeEnergyCost();
			boolean powered = remaining >= cost;
			if (powered) remaining -= cost;
			member.setMainframePowerState(tick, powered);
			changed = true;
		}
		if (changed) distribute(members, remaining);
	}
}

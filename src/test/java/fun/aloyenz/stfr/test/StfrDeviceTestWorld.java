package fun.aloyenz.stfr.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

/** Small world fixture for device interactions without a chunk or rendering engine. */
public final class StfrDeviceTestWorld extends World {
	private final Map<BlockPos, IBlockState> states = new HashMap<>();
	private final Map<BlockPos, TileEntity> tiles = new HashMap<>();
	public final Set<BlockPos> powered = new HashSet<>();
	public final List<BlockPos> broken = new ArrayList<>();
	public final List<Entity> drops = new ArrayList<>();
	public long time = 100;

	public StfrDeviceTestWorld() {
		super(null, new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false);
	}

	public void put(BlockPos pos, IBlockState state, TileEntity tile) {
		states.put(pos, state);
		tiles.put(pos, tile);
		tile.setWorld(this);
		tile.setPos(pos);
	}

	@Override public IBlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, Blocks.AIR.getDefaultState()); }
	@Override public TileEntity getTileEntity(BlockPos pos) { return tiles.get(pos); }
	@Override public void removeTileEntity(BlockPos pos) { tiles.remove(pos); }
	@Override public boolean isBlockLoaded(BlockPos pos) { return true; }
	@Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
	@Override protected IChunkProvider createChunkProvider() { return null; }
	@Override public long getTotalWorldTime() { return time; }
	@Override public boolean isBlockPowered(BlockPos pos) { return powered.contains(pos); }
	@Override public void markChunkDirty(BlockPos pos, TileEntity tile) {}
	@Override public void updateComparatorOutputLevel(BlockPos pos, Block block) {}
	@Override public boolean spawnEntity(Entity entity) { drops.add(entity); return true; }
	@Override public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
		broken.add(pos);
		TileEntity source = tiles.get(pos);
		if (source instanceof TileEntityFuelSlot slot) {
			ItemStack capsule = slot.removeForBreak();
			if (!capsule.isEmpty()) spawnEntity(new EntityItem(this, pos.getX(), pos.getY(), pos.getZ(), capsule));
		}
		TileEntity tile = tiles.remove(pos);
		if (tile != null) tile.invalidate();
		states.remove(pos);
		return true;
	}
}

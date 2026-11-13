package fun.aloyenz.stfr.reactor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.ForgeChunkManager;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** One saved registry per dimension. Removing every port does not remove a reactor. */
public final class StfrManager extends WorldSavedData {

	private static final String NAME = "hbmstfr_reactors";
	private final Map<Long, StfrReactor> reactors = new HashMap<>();
	private final Map<Long, ForgeChunkManager.Ticket> tickets = new HashMap<>();

	public StfrManager() {
		super(NAME);
	}

	public StfrManager(String name) {
		super(name);
	}

	public static StfrManager get(World world) {
		StfrManager manager =
				(StfrManager) world.getPerWorldStorage().getOrLoadData(StfrManager.class, NAME);
		if (manager == null) {
			manager = new StfrManager();
			world.getPerWorldStorage().setData(NAME, manager);
		}
		return manager;
	}

	public StfrReactor get(BlockPos center) {
		return reactors.get(center.toLong());
	}

	public StfrReactor forFuelSlot(BlockPos pos) {
		for (StfrReactor reactor : reactors.values())
			if (reactor.fuelSlotDirection(pos) != null) return reactor;
		return null;
	}

	public StfrReactor forPort(BlockPos pos) {
		for (StfrReactor reactor : reactors.values()) {
			BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
				if (cell.defaultRole != StfrBlockRole.PORT) continue;
				BlockPos expected = origin.add(cell.localPos);
				if (expected.equals(pos)) return reactor;
				EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
				if (expected.offset(direction).equals(pos)) return reactor;
			}
		}
		return null;
	}

	public StfrReactor forSolidInjector(World world, BlockPos pos) {
		for (StfrReactor reactor : reactors.values()) {
			BlockPos local = pos.subtract(reactor.center.subtract(StfrStructureTemplate.CENTER));
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			TileEntityStructureAnalyzer analyzer =
					StfrStructureValidator.matchingAnalyzer(world, reactor.center);
			if (cell != null
					&& StfrStructureValidator.miscRole(cell, analyzer)
							== StfrMiscRole.INJECTOR_PLACEABLE
					&& StfrStructureValidator.matchesCell(
							cell,
							analyzer == null ? cell.defaultRole : analyzer.getRole(local),
							world.getBlockState(pos),
							world,
							pos,
							analyzer)) return reactor;
		}
		return null;
	}

	public StfrReactor getOrCreate(BlockPos center) {
		StfrReactor reactor = reactors.get(center.toLong());
		if (reactor == null) {
			reactor = new StfrReactor(center);
			reactors.put(center.toLong(), reactor);
			markDirty();
		}
		return reactor;
	}

	public Collection<StfrReactor> reactors() {
		return Collections.unmodifiableCollection(reactors.values());
	}

	public boolean ensureTicket(World world, BlockPos center) {
		if (tickets.containsKey(center.toLong())) return true;
		ForgeChunkManager.Ticket ticket = StfrChunkLoader.request(world, center);
		if (ticket == null) return false;
		tickets.put(center.toLong(), ticket);
		return true;
	}

	void bindTicket(BlockPos center, ForgeChunkManager.Ticket ticket) {
		ForgeChunkManager.Ticket previous = tickets.put(center.toLong(), ticket);
		if (previous != null && previous != ticket) ForgeChunkManager.releaseTicket(previous);
	}

	public void tick(World world) {
		for (StfrReactor reactor : reactors.values()) {
			StfrReactor.LockState before = reactor.lockState();
			if (reactor.runState() != StfrReactor.RunState.DETONATION
					&& (reactor.lockState() != StfrReactor.LockState.UNLOCKED
							|| reactor.runState() != StfrReactor.RunState.OFFLINE))
				ensureTicket(world, reactor.center);
			reactor.tick(world);
			if (reactor.catastrophic()) markDirty();
			if (before != StfrReactor.LockState.UNLOCKED || before != reactor.lockState())
				markDirty();
			if (reactor.runState() == StfrReactor.RunState.DETONATION
					|| reactor.lockState() == StfrReactor.LockState.UNLOCKED
							&& reactor.runState() == StfrReactor.RunState.OFFLINE) {
				ForgeChunkManager.Ticket ticket = tickets.remove(reactor.center.toLong());
				if (ticket != null) ForgeChunkManager.releaseTicket(ticket);
			}
		}
		if (!reactors.isEmpty() && world.getTotalWorldTime() % 100 == 0) markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		reactors.clear();
		NBTTagList list = tag.getTagList("reactors", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			StfrReactor reactor = StfrReactor.read(list.getCompoundTagAt(i));
			reactors.put(reactor.center.toLong(), reactor);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setInteger("schemaVersion", 1);
		NBTTagList list = new NBTTagList();
		for (StfrReactor reactor : reactors.values()) list.appendTag(reactor.write());
		tag.setTag("reactors", list);
		return tag;
	}
}

package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Original marker lifecycle; a warning is not a broken stabilizer. */
public final class StfrMaintenance {

	public static final int DEADLINE = 6000, REPAIR_CLICKS = 5;

	public static final class Defect {

		public final BlockPos pos;
		public final int component;
		public int age, repairs, particle;

		Defect(BlockPos pos, int component) {
			this.pos = pos;
			this.component = component;
		}

		public boolean overdue() {
			return age >= DEADLINE;
		}

		public boolean repair() {
			return ++repairs >= REPAIR_CLICKS;
		}
	}

	private final StfrReactor reactor;
	private final List<Defect> defects = new ArrayList<>();
	private static final int[][] CASE = {
		{1, 1, 4}, {2, 1, 4}, {3, 1, 3}, {4, 1, 2}, {4, 1, 1}, {1, 2, 4}, {1, 3, 3}, {1, 4, 2},
		{1, 4, 1}, {2, 4, 1}, {3, 3, 1}, {4, 2, 1}, {3, 2, 2}, {2, 2, 3}, {2, 3, 2}
	};
	private static final int[][] STABILIZER = {
		{0, 0, 12},
		{0, 0, 13},
		{1, -1, 5},
		{-1, 1, 5},
		{-1, -1, 5},
		{1, 1, 7},
		{1, -1, 7},
		{-1, 1, 7},
		{-1, -1, 7},
		{0, 1, 7},
		{0, -1, 7},
		{1, 0, 7},
		{-1, 0, 7},
		{0, 1, 6},
		{0, -1, 6},
		{1, 0, 6},
		{-1, 0, 6},
		{1, 1, 6},
		{1, -1, 6},
		{-1, 1, 6},
		{-1, -1, 6},
		{2, 0, 4},
		{-2, 0, 4},
		{0, 2, 4},
		{0, -2, 4},
		{2, 0, 5},
		{-2, 0, 5},
		{0, 2, 5},
		{0, -2, 5},
		{1, 0, 8},
		{-1, 0, 8},
		{0, 1, 8},
		{0, -1, 8},
		{2, 0, 3},
		{-2, 0, 3},
		{0, 2, 3},
		{0, -2, 3},
		{1, 0, 11},
		{-1, 0, 11},
		{0, 1, 11},
		{0, -1, 11},
		{1, 0, 12},
		{-1, 0, 12},
		{0, 1, 12},
		{0, -1, 12},
		{1, 0, 13},
		{-1, 0, 13},
		{0, 1, 13},
		{0, -1, 13}
	};
	private static final EnumParticleTypes[] PARTICLES = {
		EnumParticleTypes.FIREWORKS_SPARK,
		EnumParticleTypes.FIREWORKS_SPARK,
		EnumParticleTypes.CRIT_MAGIC,
		EnumParticleTypes.SMOKE_LARGE,
		EnumParticleTypes.LAVA,
		EnumParticleTypes.CLOUD,
		EnumParticleTypes.FLAME,
		EnumParticleTypes.SMOKE_NORMAL,
		EnumParticleTypes.SPELL_INSTANT,
		EnumParticleTypes.SUSPENDED_DEPTH
	};
	private boolean lubricantWarned;

	StfrMaintenance(StfrReactor reactor) {
		this.reactor = reactor;
	}

	public int count() {
		return defects.size();
	}

	public int count(int component) {
		int n = 0;
		for (Defect d : defects) if (d.component == component) n++;
		return n;
	}

	public static double probability(boolean hardcore) {
		return hardcore ? .002D : .0001D;
	}

	public void tick(World world, long uptime) {
		Function<BlockPos, StfrBlockRole> roles = defects.isEmpty() ? null : roles(world);
		for (Iterator<Defect> it = defects.iterator(); it.hasNext(); ) {
			Defect d = it.next();
			if (world.isAirBlock(d.pos) || !serviceable(d.pos, roles)) {
				it.remove();
				continue;
			}
			d.age = Math.min(DEADLINE, d.age + 1);
			if (world instanceof WorldServer)
				((WorldServer) world)
						.spawnParticle(
								PARTICLES[d.particle],
								d.pos.getX() + .5,
								d.pos.getY() + .7,
								d.pos.getZ() + .5,
								2,
								.4,
								.4,
								.4,
								.02);
			if (d.overdue()) {
				// Protection prevents destruction, not the overdue warning or repair.
				if (world.getBlockState(d.pos).getBlockHardness(world, d.pos) >= 0
						&& reactor.failure().allowsPhysical(world, StfrFailurePolicy.STRUCTURE)) {
					world.destroyBlock(d.pos, true);
					world.playSound(
							null,
							d.pos,
							SoundEvents.ENTITY_GENERIC_EXPLODE,
							SoundCategory.BLOCKS,
							1F,
							1.5F);
					reactor.serviceNotice(world, "maintenance.broken." + d.component, true);
					it.remove();
				}
			}
		}
		if (reactor.runState() != StfrReactor.RunState.ONLINE) return;
		if (!defects.isEmpty()) reactor.serviceAlarm(world, uptime);
		StfrResourceNetwork resources = reactor.resources();
		boolean lubricated = resources.lubricant().getFill() > 0;
		if (lubricated && uptime % 10 == 0) resources.take(resources.lubricant(), 1);
		if (!lubricated && !lubricantWarned)
			reactor.serviceNotice(world, "maintenance.no_lubricant", true);
		lubricantWarned = !lubricated;
		if (lubricated
				|| world.playerEntities.stream()
						.noneMatch(
								p ->
										!p.isSpectator()
												&& p.getDistanceSq(reactor.center) <= 512D * 512D))
			return;
		if (world.rand.nextDouble() >= probability(StfrConfig.hardcoreMaintenance)) return;
		int component = world.rand.nextInt(7);
		simulate(world, component);
	}

	/** Add a real repairable defect, bypassing lubricant and the random failure roll. */
	public boolean simulate(World world, int component) {
		if (component < 0 || component > 6) return false;
		List<BlockPos> candidates = candidates(reactor.center, component);
		Function<BlockPos, StfrBlockRole> roles = roles(world);
		candidates.removeIf(
				target ->
						world.isAirBlock(target)
								|| world.getBlockState(target).getBlockHardness(world, target) < 0
								|| !serviceable(target, roles)
								|| defects.stream().anyMatch(d -> d.pos.equals(target)));
		if (candidates.isEmpty()) return false;
		Defect defect =
				new Defect(candidates.get(world.rand.nextInt(candidates.size())), component);
		defect.particle = world.rand.nextInt(PARTICLES.length);
		defects.add(defect);
		reactor.serviceNotice(world, "maintenance.required." + component, true);
		return true;
	}

	static boolean serviceable(BlockPos target, Function<BlockPos, StfrBlockRole> roles) {
		StfrBlockRole own = roles.apply(target);
		if (own == StfrBlockRole.UNASSIGNED || own.requiresAir()) return false;
		for (EnumFacing face : EnumFacing.VALUES)
			if (roles.apply(target.offset(face)) == StfrBlockRole.UNASSIGNED) return true;
		return false;
	}

	private Function<BlockPos, StfrBlockRole> roles(World world) {
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, reactor.center);
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		return worldPos -> {
			BlockPos local = worldPos.subtract(origin);
			if (analyzer != null) return analyzer.getRole(local);
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			return cell == null
					? StfrStructureTemplate.airRoles().getOrDefault(local, StfrBlockRole.UNASSIGNED)
					: cell.defaultRole;
		};
	}

	static List<BlockPos> candidates(BlockPos center, int component) {
		List<BlockPos> result = new ArrayList<>();
		if (component == 0)
			for (int[] v : CASE)
				for (int side = 0; side < 8; side++) {
					int x = v[0], y = side < 4 ? v[1] : -v[1], z = side < 4 ? v[2] : -v[2];
					switch (side % 4) {
						case 0:
							result.add(center.add(x, y, z));
							break;
						case 1:
							result.add(center.add(-z, y, x));
							break;
						case 2:
							result.add(center.add(-x, y, -z));
							break;
						default:
							result.add(center.add(z, y, -x));
					}
				}
		else
			for (int[] v : STABILIZER) {
				int x = v[0], y = v[1], z = v[2];
				switch (component) {
					case 1:
						result.add(center.add(x, z, -y));
						break;
					case 2:
						result.add(center.add(x, y, z));
						break;
					case 3:
						result.add(center.add(z, y, -x));
						break;
					case 4:
						result.add(center.add(-x, y, -z));
						break;
					case 5:
						result.add(center.add(-z, y, x));
						break;
					case 6:
						result.add(center.add(x, -z, y));
						break;
					default:
						throw new IllegalArgumentException("component");
				}
			}
		return result;
	}

	public boolean repair(World world, BlockPos pos) {
		for (Iterator<Defect> it = defects.iterator(); it.hasNext(); ) {
			Defect d = it.next();
			if (!d.pos.equals(pos)) continue;
			boolean done = d.repair();
			world.playSound(
					null,
					pos,
					done ? SoundEvents.BLOCK_ANVIL_LAND : SoundEvents.BLOCK_ANVIL_USE,
					SoundCategory.BLOCKS,
					1F,
					2F);
			if (done) {
				it.remove();
				reactor.serviceNotice(world, "maintenance.fixed." + d.component, false);
			}
			return true;
		}
		return false;
	}

	public NBTTagCompound write() {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		for (Defect d : defects) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", d.pos.toLong());
			item.setInteger("component", d.component);
			item.setInteger("age", d.age);
			item.setInteger("repairs", d.repairs);
			item.setInteger("particle", d.particle);
			list.appendTag(item);
		}
		tag.setTag("defects", list);
		return tag;
	}

	public void read(NBTTagCompound tag) {
		defects.clear();
		NBTTagList list = tag.getTagList("defects", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound item = list.getCompoundTagAt(i);
			int c = item.getInteger("component");
			if (c < 0 || c > 6) continue;
			Defect d = new Defect(BlockPos.fromLong(item.getLong("pos")), c);
			d.age = Math.max(0, Math.min(DEADLINE, item.getInteger("age")));
			d.repairs = Math.max(0, Math.min(4, item.getInteger("repairs")));
			d.particle = Math.max(0, Math.min(PARTICLES.length - 1, item.getInteger("particle")));
			defects.add(d);
		}
	}
}

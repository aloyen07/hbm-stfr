package fun.aloyenz.stfr.reactor.physics;

import java.util.*;
import java.util.function.Function;
import net.minecraft.util.math.BlockPos;

/** Radial shells, with equal-distance cells interleaved between all six sectors. */
public final class StfrDisassemblyOrder {
	private StfrDisassemblyOrder() {}
	public static <T> List<T> centerOut(List<T> input, Function<T, BlockPos> position, BlockPos center) {
		Map<Integer, List<Deque<T>>> shells = new TreeMap<>();
		for (T entry : input) {
			BlockPos p = position.apply(entry).subtract(center);
			int x = p.getX(), y = p.getY(), z = p.getZ();
			int sector = Math.abs(x) >= Math.abs(y) && Math.abs(x) >= Math.abs(z)
					? (x >= 0 ? 0 : 1) : Math.abs(y) >= Math.abs(z) ? (y >= 0 ? 2 : 3) : (z >= 0 ? 4 : 5);
			List<Deque<T>> sectors = shells.computeIfAbsent(x*x+y*y+z*z, key -> {
				List<Deque<T>> list = new ArrayList<>();
				for (int i = 0; i < 6; i++) list.add(new ArrayDeque<>());
				return list;
			});
			sectors.get(sector).add(entry);
		}
		List<T> result = new ArrayList<>();
		for (List<Deque<T>> sectors : shells.values()) {
			boolean more;
			do {
				more = false;
				for (Deque<T> sector : sectors) if (!sector.isEmpty()) {
					result.add(sector.removeFirst()); more = true;
				}
			} while (more);
		}
		return Collections.unmodifiableList(result);
	}
}

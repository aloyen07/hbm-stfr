package fun.aloyenz.stfr.debug;

import java.util.*;
import net.minecraft.util.math.BlockPos;

/** Transient positions only. No reactor, tile, block, explosion or damage references. */
public final class StfrFakeCoreRegistry {
    private final Map<Integer,Set<BlockPos>> dimensions=new HashMap<>();
    public boolean add(int dimension,BlockPos pos) {
        return dimensions.computeIfAbsent(dimension,key -> new LinkedHashSet<>()).add(pos.toImmutable());
    }
    public Set<BlockPos> positions(int dimension) {
        return Collections.unmodifiableSet(dimensions.getOrDefault(dimension,Collections.emptySet()));
    }
    public int clear() {
        int count=dimensions.values().stream().mapToInt(Set::size).sum();dimensions.clear();return count;
    }
}

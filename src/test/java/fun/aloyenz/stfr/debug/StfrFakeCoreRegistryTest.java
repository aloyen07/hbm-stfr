package fun.aloyenz.stfr.debug;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class StfrFakeCoreRegistryTest {
    @Test void globalClearRemovesEveryDimensionAndEveryAuthor() {
        StfrFakeCoreRegistry registry=new StfrFakeCoreRegistry();
        assertTrue(registry.add(0,new BlockPos(1,2,3)));
        assertTrue(registry.add(-1,new BlockPos(1,2,3)));
        assertTrue(registry.add(42,new BlockPos(10,20,30)));
        assertFalse(registry.add(0,new BlockPos(1,2,3)));
        assertEquals(3,registry.clear());
        for(int dim:new int[]{0,-1,42})assertTrue(registry.positions(dim).isEmpty());
        assertEquals(0,registry.clear());
    }
    @Test void positionsAreImmutableAndNewServerHasNoSavedPreviews() {
        StfrFakeCoreRegistry registry=new StfrFakeCoreRegistry();
        BlockPos.MutableBlockPos position=new BlockPos.MutableBlockPos(4,5,6);
        registry.add(0,position);position.setPos(9,9,9);
        assertTrue(registry.positions(0).contains(new BlockPos(4,5,6)));
        assertThrows(UnsupportedOperationException.class,() -> registry.positions(0).clear());
        assertTrue(new StfrFakeCoreRegistry().positions(0).isEmpty());
    }
}

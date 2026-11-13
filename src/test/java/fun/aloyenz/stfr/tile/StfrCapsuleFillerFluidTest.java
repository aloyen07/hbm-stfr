package fun.aloyenz.stfr.tile;

import static org.junit.jupiter.api.Assertions.*;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.misc.EnumSymbol;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.StfrFluids;

final class StfrCapsuleFillerFluidTest {
	private static FluidType fluid(String name) {
		return new FluidType(name, 0xFFFFFF, 0, 0, 0, EnumSymbol.CROYGENIC);
	}

	@Test
	void optionalSpaceAliasNormalizesWithoutLoadingSpaceClasses() {
		Bootstrap.register();
		FluidType oldWater = Fluids.WATER, oldDeuterium = Fluids.DEUTERIUM;
		FluidType oldNitrogen = StfrFluids.LIQUID_NITROGEN;
		try {
			Fluids.WATER = fluid("TEST_STFR_WATER");
			Fluids.DEUTERIUM = fluid("TEST_STFR_DEUTERIUM");
			StfrFluids.LIQUID_NITROGEN = fluid("STFR_LIQUID_NITROGEN");
			assertFalse(StfrFluids.isLiquidNitrogen(Fluids.NONE));
			assertNull(StfrFluids.spaceNitrogen(), "Base HBM has no Space nitrogen");
			assertSame(StfrFluids.LIQUID_NITROGEN,
					TileEntityCapsuleFiller.canonical(StfrFluids.LIQUID_NITROGEN));
			FluidType space = fluid("NITROGEN");
			assertSame(space, StfrFluids.spaceNitrogen());
			assertTrue(StfrFluids.isLiquidNitrogen(space));
			assertFalse(TileEntityCapsuleFiller.Mode.UNSELECTED.accepts(Fluids.WATER));
			assertTrue(TileEntityCapsuleFiller.Mode.WATER.accepts(Fluids.WATER));
			assertTrue(TileEntityCapsuleFiller.Mode.WATER.accepts(space));
			assertTrue(TileEntityCapsuleFiller.Mode.WATER.accepts(StfrFluids.LIQUID_NITROGEN));
			assertFalse(TileEntityCapsuleFiller.Mode.WATER.accepts(Fluids.DEUTERIUM));
			assertFalse(TileEntityCapsuleFiller.Mode.DEUTERIUM.accepts(space));
			assertTrue(TileEntityCapsuleFiller.Mode.DEUTERIUM.accepts(Fluids.DEUTERIUM));
			assertSame(StfrFluids.LIQUID_NITROGEN, TileEntityCapsuleFiller.canonical(space));
			assertSame(Fluids.WATER, TileEntityCapsuleFiller.canonical(Fluids.WATER));
			assertSame(Fluids.DEUTERIUM, TileEntityCapsuleFiller.canonical(Fluids.DEUTERIUM));
			assertFalse(StfrFluids.isLiquidNitrogen(fluid("TEST_UNRELATED_COOLANT")));
		} finally {
			Fluids.WATER = oldWater;
			Fluids.DEUTERIUM = oldDeuterium;
			StfrFluids.LIQUID_NITROGEN = oldNitrogen;
		}
	}
}

package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.render.misc.EnumSymbol;
import net.minecraft.util.ResourceLocation;

/** HBM-compatible liquid product exported by the common turbine circuit. */
public final class StfrFluids {

	public static FluidType YARONITE;
	public static FluidType LIQUID_NITROGEN;

	private StfrFluids() {}

	/** Space is optional; resolve its registered fluid without loading any Space classes. */
	public static FluidType spaceNitrogen() {
		FluidType type = Fluids.fromName("NITROGEN");
		return type == Fluids.NONE ? null : type;
	}

	public static boolean isLiquidNitrogen(FluidType type) {
		return type != null && (type == LIQUID_NITROGEN || type == spaceNitrogen());
	}

	public static void init() {
		if (YARONITE != null) return;
		YARONITE =
				new FluidType("STFR_YARONITE", 0x8B1028, 4, 0, 2, EnumSymbol.RADIATION) {
					@Override
					public ResourceLocation getTexture() {
						return new ResourceLocation(
								"hbmstfr", "textures/gui/fluids/stfr_yaronite.png");
					}

					@Override
					public int getTint() {
						return 0xFFFFFF;
					}
				}.addTraits(Fluids.LIQUID, new FT_Corrosive(60), new FT_VentRadiation(0.1F));
		Fluids.metaOrder.add(YARONITE);
		Fluids.setupForgeFluidCompat(
				YARONITE,
				new ResourceLocation("hbmstfr", "blocks/forgefluid/yaronite_still"),
				new ResourceLocation("hbmstfr", "blocks/forgefluid/yaronite_flowing"),
				0xFFFFFFFF);
		LIQUID_NITROGEN = new FluidType("STFR_LIQUID_NITROGEN", 0x74D9EF, 2, 0, 0,
				EnumSymbol.CROYGENIC) {
			@Override
			public ResourceLocation getTexture() {
				return new ResourceLocation("hbm", "textures/gui/fluids/cryogel.png");
			}
		}.setTemp(-196).addTraits(Fluids.LIQUID);
		Fluids.metaOrder.add(LIQUID_NITROGEN);
		Fluids.setupForgeFluidCompat(
				LIQUID_NITROGEN,
				new ResourceLocation("hbm", "blocks/forgefluid/fluid_default_still"),
				new ResourceLocation("hbm", "blocks/forgefluid/fluid_default_flowing"),
				0xFF74D9EF);
	}
}

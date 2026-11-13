package fun.aloyenz.stfr.block;

import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import fun.aloyenz.stfr.Tags;

/** World surface and flowing faces use the registered Forge fluid's sprites. */
public final class BlockYaronite extends BlockFluidClassic {

	public BlockYaronite(Fluid fluid) {
		super(fluid, Material.WATER);
		setRegistryName(Tags.MODID, "yaronite");
		setTranslationKey(Tags.MODID + ".yaronite");
	}
}

package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockEndRod;
import net.minecraft.block.SoundType;
import fun.aloyenz.stfr.Tags;

/** Decorative and conductive mounting electrode, placeable on every block face. */
public class BlockTechnetiumElectrode extends BlockEndRod {

	public BlockTechnetiumElectrode() {
		this("technetium_electrode");
	}

	public BlockTechnetiumElectrode(String registryName) {
		setRegistryName(Tags.MODID, registryName);
		setTranslationKey(Tags.MODID + "." + registryName);
		setSoundType(SoundType.METAL);
		setHardness(2.0F);
		setResistance(10.0F);
		setLightLevel(0.15F);
		setCreativeTab(STFRBlocks.TAB);
	}
}

package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import fun.aloyenz.stfr.Tags;

/** Ordinary stairs made from welded BSCCO coils. */
public final class BlockBsccoStairs extends BlockStairs {

	public BlockBsccoStairs() {
		super(Blocks.IRON_BLOCK.getDefaultState());
		setRegistryName(Tags.MODID, "bscco_stairs");
		setTranslationKey(Tags.MODID + ".bscco_stairs");
		setSoundType(SoundType.METAL);
		setHardness(5.0F);
		setResistance(20.0F);
		setCreativeTab(STFRBlocks.TAB);
	}
}

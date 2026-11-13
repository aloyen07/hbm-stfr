package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import fun.aloyenz.stfr.Tags;

public final class BlockTechnetiumStairs extends BlockStairs {

	public BlockTechnetiumStairs() {
		super(Blocks.IRON_BLOCK.getDefaultState());
		setRegistryName(Tags.MODID, "technetium_steel_stairs");
		setTranslationKey(Tags.MODID + ".technetium_steel_stairs");
		setSoundType(SoundType.METAL);
		setHardness(5.0F);
		setResistance(20.0F);
		setCreativeTab(STFRBlocks.TAB);
	}
}

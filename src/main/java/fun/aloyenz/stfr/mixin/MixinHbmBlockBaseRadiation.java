package fun.aloyenz.stfr.mixin;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import fun.aloyenz.stfr.StfrConfig;

/** The three concrete blocks use HBM's generic BlockBase, so check their identities. */
@Mixin(value = BlockBase.class, remap = false)
public abstract class MixinHbmBlockBaseRadiation extends Block implements IRadResistantBlock {
	protected MixinHbmBlockBaseRadiation(Material material) {
		super(material);
	}

	@Override
	public boolean isRadResistant(World world, BlockPos pos) {
		return StfrConfig.radiationResistantConcrete && isConfiguredConcrete();
	}

	private boolean isConfiguredConcrete() {
		return (Object) this == ModBlocks.concrete
				|| (Object) this == ModBlocks.concrete_smooth
				|| (Object) this == ModBlocks.concrete_colored;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		if (StfrConfig.radiationResistantConcrete && isConfiguredConcrete())
			RadiationSystemNT.markSectionForRebuild(world, pos);
		super.onBlockAdded(world, pos, state);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (StfrConfig.radiationResistantConcrete && isConfiguredConcrete())
			RadiationSystemNT.markSectionForRebuild(world, pos);
		super.breakBlock(world, pos, state);
	}
}

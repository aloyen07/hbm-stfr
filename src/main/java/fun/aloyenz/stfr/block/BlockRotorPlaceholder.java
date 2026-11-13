package fun.aloyenz.stfr.block;

import java.util.Random;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;

/** Solid, non-harvestable world anchor for an independently rendered rotor block. */
public final class BlockRotorPlaceholder extends BlockContainer {

	public BlockRotorPlaceholder() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "rotor_placeholder");
		setTranslationKey(Tags.MODID + ".rotor_placeholder");
		setBlockUnbreakable();
		setResistance(6000000F);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityRotorPlaceholder();
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return true;
	}

	@Override
	public EnumPushReaction getPushReaction(IBlockState state) {
		return EnumPushReaction.BLOCK;
	}

	@Override
	public boolean canEntityDestroy(
			IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
		return false;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}
}

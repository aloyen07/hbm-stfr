package fun.aloyenz.stfr.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;

/**
 * Invisible, replaceable air that participates in vanilla block lighting. Never replaces solids.
 */
public final class BlockReactorLight extends Block {

	public static final PropertyInteger LEVEL = PropertyInteger.create("level", 1, 15);

	public BlockReactorLight() {
		super(Material.AIR);
		setRegistryName(Tags.MODID, "reactor_light");
		setTranslationKey(Tags.MODID + ".reactor_light");
		setDefaultState(blockState.getBaseState().withProperty(LEVEL, 1));
		setLightOpacity(0);
		setHardness(0);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, LEVEL);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(LEVEL, Math.max(1, Math.min(15, meta + 1)));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(LEVEL) - 1;
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getValue(LEVEL);
	}

	@Override
	public boolean isAir(IBlockState state, IBlockAccess world, BlockPos pos) {
		return true;
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
		return true;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(
			IBlockState state, IBlockAccess world, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	public boolean canCollideCheck(IBlockState state, boolean liquids) {
		return false;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return Items.AIR;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		world.checkLight(pos);
	}
}

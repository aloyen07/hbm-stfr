package fun.aloyenz.stfr.block;

import com.hbm.blocks.network.energy.BlockCable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.api.data.IDataMachine;
import fun.aloyenz.stfr.tile.TileEntityDataCable;

/** Data-only conductor whose nodes form an isolated logical network. */
public final class BlockDataCable extends BlockContainer {

	private static final AxisAlignedBB[] SHAPES = new AxisAlignedBB[64];
	private final IBlockState[] states = new IBlockState[64];

	static {
		double min = 5.5D / 16D;
		double max = 10.5D / 16D;
		for (int mask = 0; mask < SHAPES.length; mask++) {
			SHAPES[mask] =
					new AxisAlignedBB(
							bit(mask, EnumFacing.WEST) ? 0 : min,
									bit(mask, EnumFacing.DOWN) ? 0 : min,
							bit(mask, EnumFacing.NORTH) ? 0 : min,
									bit(mask, EnumFacing.EAST) ? 1 : max,
							bit(mask, EnumFacing.UP) ? 1 : max,
									bit(mask, EnumFacing.SOUTH) ? 1 : max);
		}
	}

	public BlockDataCable() {
		super(Material.CIRCUITS);
		setRegistryName(Tags.MODID, "data_cable");
		setTranslationKey(Tags.MODID + ".data_cable");
		setSoundType(SoundType.METAL);
		setHardness(1.0F);
		setCreativeTab(STFRBlocks.TAB);
		IBlockState base =
				blockState
						.getBaseState()
						.withProperty(BlockCable.POS_X, false)
						.withProperty(BlockCable.NEG_X, false)
						.withProperty(BlockCable.POS_Y, false)
						.withProperty(BlockCable.NEG_Y, false)
						.withProperty(BlockCable.POS_Z, false)
						.withProperty(BlockCable.NEG_Z, false);
		setDefaultState(base);
		for (int mask = 0; mask < states.length; mask++) {
			states[mask] =
					base.withProperty(BlockCable.POS_X, bit(mask, EnumFacing.EAST))
							.withProperty(BlockCable.NEG_X, bit(mask, EnumFacing.WEST))
							.withProperty(BlockCable.POS_Y, bit(mask, EnumFacing.UP))
							.withProperty(BlockCable.NEG_Y, bit(mask, EnumFacing.DOWN))
							.withProperty(BlockCable.POS_Z, bit(mask, EnumFacing.SOUTH))
							.withProperty(BlockCable.NEG_Z, bit(mask, EnumFacing.NORTH));
		}
	}

	private static boolean bit(int mask, EnumFacing side) {
		return (mask & 1 << side.getIndex()) != 0;
	}

	/** Both full-block casings carry exactly the same six-sided data bus. */
	public static boolean isDataCable(Block block) {
		return block instanceof BlockDataCable || block instanceof BlockCoveredDataCable;
	}

	public boolean connectsTo(IBlockAccess world, BlockPos pos, EnumFacing side) {
		BlockPos neighbour = pos.offset(side);
		return isDataCable(world.getBlockState(neighbour).getBlock())
				|| world.getTileEntity(neighbour) instanceof IDataMachine machine
						&& machine.acceptsDataCable();
	}

	private int connectionMask(IBlockAccess world, BlockPos pos) {
		int mask = 0;
		for (EnumFacing side : EnumFacing.VALUES) {
			if (connectsTo(world, pos, side)) mask |= 1 << side.getIndex();
		}
		return mask;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(
				this,
				BlockCable.POS_X,
				BlockCable.NEG_X,
				BlockCable.POS_Y,
				BlockCable.NEG_Y,
				BlockCable.POS_Z,
				BlockCable.NEG_Z);
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		return states[connectionMask(world, pos)];
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return SHAPES[connectionMask(world, pos)];
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityDataCable();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState();
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return 0;
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
	public boolean causesSuffocation(IBlockState state) {
		return false;
	}

	@Override
	public boolean isPassable(IBlockAccess world, BlockPos pos) {
		return true;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(
			IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}
}

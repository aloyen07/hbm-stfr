package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;

public final class BlockFuelSlot extends BlockContainer {

	public static final PropertyDirection FACING = PropertyDirection.create("facing");
	public static final PropertyDirection HANDLE_FACING =
			PropertyDirection.create("handle_facing", EnumFacing.Plane.HORIZONTAL);

	public BlockFuelSlot() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "fuel_slot");
		setTranslationKey(Tags.MODID + ".fuel_slot");
		setSoundType(SoundType.METAL);
		setHardness(5F);
		setResistance(20F);
		setCreativeTab(STFRBlocks.TAB);
		setDefaultState(
				blockState
						.getBaseState()
						.withProperty(FACING, EnumFacing.NORTH)
						.withProperty(HANDLE_FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, HANDLE_FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		if (meta == 4 || meta >= 6 && meta <= 8)
			return getDefaultState()
					.withProperty(FACING, EnumFacing.UP)
					.withProperty(HANDLE_FACING, handleFromMeta(meta, 6));
		if (meta == 5 || meta >= 9 && meta <= 11)
			return getDefaultState()
					.withProperty(FACING, EnumFacing.DOWN)
					.withProperty(HANDLE_FACING, handleFromMeta(meta, 9));
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		EnumFacing facing = state.getValue(FACING);
		if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
			EnumFacing handle = state.getValue(HANDLE_FACING);
			if (handle == EnumFacing.NORTH) return facing == EnumFacing.UP ? 4 : 5;
			int orientation = handle == EnumFacing.SOUTH ? 0 : handle == EnumFacing.WEST ? 1 : 2;
			return (facing == EnumFacing.UP ? 6 : 9) + orientation;
		}
		return facing.getHorizontalIndex();
	}

	private static EnumFacing handleFromMeta(int meta, int first) {
		return meta < first
				? EnumFacing.NORTH
				: meta == first
						? EnumFacing.SOUTH
						: meta == first + 1 ? EnumFacing.WEST : EnumFacing.EAST;
	}

	@Override
	public IBlockState getStateForPlacement(
			World world,
			BlockPos pos,
			EnumFacing side,
			float hitX,
			float hitY,
			float hitZ,
			int meta,
			EntityLivingBase placer) {
		IBlockState state = getDefaultState().withProperty(FACING, side.getOpposite());
		if (side.getAxis().isVertical()) {
			state = state.withProperty(HANDLE_FACING, placer.getHorizontalFacing().getOpposite());
		}
		return state;
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)))
				.withProperty(HANDLE_FACING, rotation.rotate(state.getValue(HANDLE_FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)))
				.withProperty(HANDLE_FACING, mirror.mirror(state.getValue(HANDLE_FACING)));
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityFuelSlot();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
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
	public boolean onBlockActivated(
			World world,
			BlockPos pos,
			IBlockState state,
			EntityPlayer player,
			EnumHand hand,
			EnumFacing side,
			float hitX,
			float hitY,
			float hitZ) {
		TileEntity tile = world.getTileEntity(pos);
		return tile instanceof TileEntityFuelSlot
				&& ((TileEntityFuelSlot) tile).activate(player, hand);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity tile = world.getTileEntity(pos);
		if (!world.isRemote && tile instanceof TileEntityFuelSlot) {
			ItemStack capsule = ((TileEntityFuelSlot) tile).removeForBreak();
			if (!capsule.isEmpty()) spawnAsEntity(world, pos, capsule);
		}
		super.breakBlock(world, pos, state);
	}
}

package fun.aloyenz.stfr.block;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrLogicSyncPacket;
import fun.aloyenz.stfr.meme.StfrCatEvents;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Non-editable sign mounted on a logic receiver. The board is rendered by TESR. */
public final class BlockLogicMonitor extends BlockLogicDevice {

	public static final PropertyDirection FACING =
			PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyBool WALL = PropertyBool.create("wall");
	private static final AxisAlignedBB SIGN_BOX =
			new AxisAlignedBB(0.0625, 0, 0.4375, 0.9375, 1, 0.5625);
	private static final AxisAlignedBB SIGN_BOX_SIDE =
			new AxisAlignedBB(0.4375, 0, 0.0625, 0.5625, 1, 0.9375);
	private static final AxisAlignedBB WALL_NORTH =
			new AxisAlignedBB(0, 0.28125, 0.875, 1, 0.78125, 1);
	private static final AxisAlignedBB WALL_SOUTH =
			new AxisAlignedBB(0, 0.28125, 0, 1, 0.78125, 0.125);
	private static final AxisAlignedBB WALL_WEST =
			new AxisAlignedBB(0.875, 0.28125, 0, 1, 0.78125, 1);
	private static final AxisAlignedBB WALL_EAST =
			new AxisAlignedBB(0, 0.28125, 0, 0.125, 0.78125, 1);

	public BlockLogicMonitor() {
		super("logic_monitor", Kind.MONITOR);
		setLightOpacity(0);
		setLightLevel(1F);
		setDefaultState(
				getDefaultState().withProperty(FACING, EnumFacing.SOUTH).withProperty(WALL, false));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, CLOSED, FACING, WALL);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
				.withProperty(WALL, (meta & 4) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex() | (state.getValue(WALL) ? 4 : 0);
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
			EntityLivingBase placer,
			EnumHand hand) {
		return getDefaultState()
				.withProperty(
						FACING,
						side == EnumFacing.UP ? placer.getHorizontalFacing().getOpposite() : side)
				.withProperty(WALL, side != EnumFacing.UP);
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
		if (side == EnumFacing.UP)
			return world.getBlockState(pos.down()).getBlock() == STFRBlocks.LOGIC_RECEIVER;
		return side.getAxis().isHorizontal()
				&& world.isSideSolid(pos.offset(side.getOpposite()), side, false);
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		if (!super.canPlaceBlockAt(world, pos)) return false;
		if (world.getBlockState(pos.down()).getBlock() == STFRBlocks.LOGIC_RECEIVER) return true;
		for (EnumFacing side : EnumFacing.HORIZONTALS)
			if (world.isSideSolid(pos.offset(side.getOpposite()), side, false)) return true;
		return false;
	}

	@Override
	public void neighborChanged(
			IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		boolean supported =
				state.getValue(WALL)
						? world.isSideSolid(
								pos.offset(state.getValue(FACING).getOpposite()),
								state.getValue(FACING),
								false)
						: world.getBlockState(pos.down()).getBlock() == STFRBlocks.LOGIC_RECEIVER;
		if (!supported) world.destroyBlock(pos, true);
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
		TileEntity catTile = world.getTileEntity(pos);
		if (catTile instanceof TileEntityLogicDevice) {
			TileEntityLogicDevice monitor = (TileEntityLogicDevice) catTile;
			if (hand != EnumHand.MAIN_HAND && monitor.catStage() > 0) return true;
			if (StfrCatEvents.click(world, monitor)) return true;
		}
		if (player.getHeldItem(hand).getItem() == STFRItems.MONITOR_PRESET_WAND) return false;
		if (!world.isRemote && player instanceof EntityPlayerMP) {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof TileEntityLogicDevice)
				StfrLogicPacket.CHANNEL.sendTo(
						new StfrLogicSyncPacket(pos, side, (TileEntityLogicDevice) tile),
						(EntityPlayerMP) player);
		}
		return true;
	}

	@Override
	public boolean canCreatureSpawn(
			IBlockState state,
			IBlockAccess world,
			BlockPos pos,
			EntityLiving.SpawnPlacementType type) {
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (!state.getValue(WALL))
			return state.getValue(FACING).getAxis() == EnumFacing.Axis.X ? SIGN_BOX_SIDE : SIGN_BOX;
		switch (state.getValue(FACING)) {
			case NORTH:
				return WALL_NORTH;
			case SOUTH:
				return WALL_SOUTH;
			case WEST:
				return WALL_WEST;
			case EAST:
				return WALL_EAST;
			default:
				return WALL_NORTH;
		}
	}
}

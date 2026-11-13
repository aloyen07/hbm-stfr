package fun.aloyenz.stfr.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;

/** A straight vanilla stair shape rotated by 90 degrees onto a vertical plane. */
public class BlockVerticalStairs extends Block {

	public static final PropertyDirection FACING =
			PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	private static final AxisAlignedBB EAST_BACK = new AxisAlignedBB(0, 0, 0, 1, 1, 0.5D);
	private static final AxisAlignedBB EAST_STEP = new AxisAlignedBB(0.5D, 0, 0.5D, 1, 1, 1);
	private static final AxisAlignedBB SOUTH_BACK = new AxisAlignedBB(0.5D, 0, 0, 1, 1, 1);
	private static final AxisAlignedBB SOUTH_STEP = new AxisAlignedBB(0, 0, 0.5D, 0.5D, 1, 1);
	private static final AxisAlignedBB WEST_BACK = new AxisAlignedBB(0, 0, 0.5D, 1, 1, 1);
	private static final AxisAlignedBB WEST_STEP = new AxisAlignedBB(0, 0, 0, 0.5D, 1, 0.5D);
	private static final AxisAlignedBB NORTH_BACK = new AxisAlignedBB(0, 0, 0, 0.5D, 1, 1);
	private static final AxisAlignedBB NORTH_STEP = new AxisAlignedBB(0.5D, 0, 0, 1, 1, 0.5D);

	public BlockVerticalStairs() {
		this("vertical_technetium_steel_stairs");
	}

	public BlockVerticalStairs(String registryName) {
		super(Material.IRON);
		setRegistryName(Tags.MODID, registryName);
		setTranslationKey(Tags.MODID + "." + registryName);
		setSoundType(SoundType.METAL);
		setHardness(5.0F);
		setResistance(20.0F);
		setCreativeTab(STFRBlocks.TAB);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex();
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
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return withRotation(state, mirror.toRotation(state.getValue(FACING)));
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return FULL_BLOCK_AABB;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(
			IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public void addCollisionBoxToList(
			IBlockState state,
			World world,
			BlockPos pos,
			AxisAlignedBB entityBox,
			List<AxisAlignedBB> boxes,
			@Nullable Entity entity,
			boolean actualState) {
		switch (state.getValue(FACING)) {
			case SOUTH -> {
				addCollisionBoxToList(pos, entityBox, boxes, SOUTH_BACK);
				addCollisionBoxToList(pos, entityBox, boxes, SOUTH_STEP);
			}
			case WEST -> {
				addCollisionBoxToList(pos, entityBox, boxes, WEST_BACK);
				addCollisionBoxToList(pos, entityBox, boxes, WEST_STEP);
			}
			case EAST -> {
				addCollisionBoxToList(pos, entityBox, boxes, EAST_BACK);
				addCollisionBoxToList(pos, entityBox, boxes, EAST_STEP);
			}
			default -> {
				addCollisionBoxToList(pos, entityBox, boxes, NORTH_BACK);
				addCollisionBoxToList(pos, entityBox, boxes, NORTH_STEP);
			}
		}
	}
}

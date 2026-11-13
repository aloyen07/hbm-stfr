package fun.aloyenz.stfr.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.STFR;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;

/** An annotated structure-mounted feed throat with a configurable polonium buffer. */
public final class BlockSolidInjector extends Block implements ITileEntityProvider {

	public static final PropertyDirection FACING =
			PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

	public BlockSolidInjector() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "solid_injector");
		setTranslationKey(Tags.MODID + ".solid_injector");
		setHardness(5F);
		setResistance(30F);
		setCreativeTab(STFRBlocks.TAB);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex((meta & 3) + 2));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return (state.getValue(FACING).getHorizontalIndex() + 2) & 3;
	}

	@Override
	public IBlockState getStateForPlacement(
			World world,
			BlockPos pos,
			EnumFacing face,
			float hitX,
			float hitY,
			float hitZ,
			int meta,
			EntityLivingBase placer) {
		// The feed throat points into the attached structure, the loading
		// cassette towards the player. Vertical placement keeps a horizontal throat.
		EnumFacing inward =
				face.getAxis().isHorizontal() ? face.getOpposite() : placer.getHorizontalFacing();
		return getDefaultState().withProperty(FACING, inward);
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
	public boolean onBlockActivated(
			World world,
			BlockPos pos,
			IBlockState state,
			EntityPlayer player,
			EnumHand hand,
			EnumFacing face,
			float x,
			float y,
			float z) {
		if (player.getHeldItem(hand).getItem() == STFRItems.REACTOR_DEBUG_WAND
				|| player.getHeldItem(hand).getItem() == STFRItems.REACTOR_ROTOR_WAND
				|| player.getHeldItem(hand).getItem() == STFRItems.REACTOR_DESTRUCTION_WAND)
			return false;
		if (!world.isRemote)
			player.openGui(STFR.instance, 2, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntitySolidInjector();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity tile = world.getTileEntity(pos);
		if (!world.isRemote && tile instanceof TileEntitySolidInjector) {
			ItemStack stack = ((TileEntitySolidInjector) tile).eject();
			if (!stack.isEmpty())
				InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
		}
		super.breakBlock(world, pos, state);
	}
}

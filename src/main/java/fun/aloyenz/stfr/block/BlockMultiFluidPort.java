package fun.aloyenz.stfr.block;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;

public final class BlockMultiFluidPort extends BlockContainer implements IRadResistantBlock {

	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	public BlockMultiFluidPort() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "multi_fluid_port");
		setTranslationKey(Tags.MODID + ".multi_fluid_port");
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
		if (meta == 4) return getDefaultState().withProperty(FACING, EnumFacing.UP);
		if (meta == 5) return getDefaultState().withProperty(FACING, EnumFacing.DOWN);
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		EnumFacing facing = state.getValue(FACING);
		if (facing == EnumFacing.UP) return 4;
		if (facing == EnumFacing.DOWN) return 5;
		return facing.getHorizontalIndex();
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
		return getDefaultState().withProperty(FACING, side.getOpposite());
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
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityMultiFluidPort();
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		RadiationSystemNT.markSectionForRebuild(world, pos);
		super.onBlockAdded(world, pos, state);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		RadiationSystemNT.markSectionForRebuild(world, pos);
		super.breakBlock(world, pos, state);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
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
		if (world.isRemote) return true;
		if (FluidUtil.interactWithFluidHandler(player, hand, world, pos, side)) return true;
		if (world.getTileEntity(pos) instanceof TileEntityMultiFluidPort) {
			TileEntityMultiFluidPort port = (TileEntityMultiFluidPort) world.getTileEntity(pos);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.port.status",
							port.waterAmount(),
							port.lubricantAmount(),
							port.steamAmount(),
							port.productsAmount(),
							port.getPower()));
		}
		return true;
	}
}

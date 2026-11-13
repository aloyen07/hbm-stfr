package fun.aloyenz.stfr.block;

import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityCapsuleFiller;

/** Six-way mounted refiller. The recessed front faces the player and the fuel slot. */
public final class BlockCapsuleFiller extends BlockContainer {
	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	public BlockCapsuleFiller() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "capsule_filler");
		setTranslationKey(Tags.MODID + ".capsule_filler");
		setSoundType(SoundType.METAL);
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
		return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta % 6));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
			float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, side);
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
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityCapsuleFiller();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
			EntityPlayer player, EnumHand hand, EnumFacing face, float x, float y, float z) {
		if (!world.isRemote) {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof TileEntityCapsuleFiller) {
				TileEntityCapsuleFiller filler = (TileEntityCapsuleFiller) tile;
				if (player.getHeldItem(hand).getItem() instanceof IItemFluidIdentifier)
					filler.selectMode(player, ((IItemFluidIdentifier) player.getHeldItem(hand).getItem())
							.getType(world, pos.getX(), pos.getY(), pos.getZ(), player.getHeldItem(hand)));
				else if (player.getHeldItem(hand).isEmpty()) filler.sendStatus(player);
			}
		}
		return true;
	}
}

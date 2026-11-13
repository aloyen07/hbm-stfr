package fun.aloyenz.stfr.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;

/** Directional electrode controller/mechanism whose marked top points away from the placer. */
public final class BlockElectrodeControl extends Block {

	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	public BlockElectrodeControl(String registryName) {
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
}

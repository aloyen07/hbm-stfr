package fun.aloyenz.stfr.block;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The animated front faces the operator; the rear and service ports stay distinct. */
public final class BlockLogicMainframe extends BlockLogicDevice {

	public static final PropertyDirection FACING =
			PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

	private final boolean creative;

	public BlockLogicMainframe() {
		this("logic_mainframe", true);
	}

	public BlockLogicMainframe(String name, boolean creative) {
		super(name, Kind.MAINFRAME);
		this.creative = creative;
		setDefaultState(getDefaultState().withProperty(FACING, EnumFacing.NORTH));
	}

	public boolean creative() {
		return creative;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, CLOSED, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		// Legacy metadata 0/1 remains north-facing; retain the old closed bit.
		return getDefaultState()
				.withProperty(CLOSED, (meta & 1) != 0)
				.withProperty(FACING, EnumFacing.byHorizontalIndex(((meta >> 1) & 3) + 2));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return (((state.getValue(FACING).getHorizontalIndex() + 2) & 3) << 1)
				| (state.getValue(CLOSED) ? 1 : 0);
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
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
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

package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;

/** Breakable in-world anchor for an electrode whose original model is aimed at the core. */
public final class BlockElectrodeProxy extends BlockContainer {

	public BlockElectrodeProxy() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "electrode_proxy");
		setTranslationKey(Tags.MODID + ".electrode_proxy");
		setHardness(4F);
		setResistance(20F);
		setSoundType(SoundType.METAL);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityElectrodeProxy();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileEntityElectrodeProxy)) return FULL_BLOCK_AABB;
		TileEntityElectrodeProxy proxy = (TileEntityElectrodeProxy) tile;
		if (proxy.original() == null) return FULL_BLOCK_AABB;
		Vec3d base = StfrElectrodes.mount(pos, proxy.original()),
				tip = StfrElectrodes.tip(pos, proxy.original(), proxy.center());
		Vec3d axis = tip.subtract(base);
		double rx = .125 * Math.sqrt(Math.max(0, 1 - axis.x * axis.x));
		double ry = .125 * Math.sqrt(Math.max(0, 1 - axis.y * axis.y));
		double rz = .125 * Math.sqrt(Math.max(0, 1 - axis.z * axis.z));
		return new AxisAlignedBB(
				Math.min(base.x, tip.x) - rx - pos.getX(),
				Math.min(base.y, tip.y) - ry - pos.getY(),
				Math.min(base.z, tip.z) - rz - pos.getZ(),
				Math.max(base.x, tip.x) + rx - pos.getX(),
				Math.max(base.y, tip.y) + ry - pos.getY(),
				Math.max(base.z, tip.z) + rz - pos.getZ());
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(
			IBlockState state, IBlockAccess world, BlockPos pos) {
		return getBoundingBox(state, world, pos);
	}

	@Override
	public void getDrops(
			NonNullList<ItemStack> drops,
			IBlockAccess world,
			BlockPos pos,
			IBlockState state,
			int fortune) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityElectrodeProxy) {
			IBlockState original = ((TileEntityElectrodeProxy) tile).original();
			if (original != null)
				original.getBlock().getDrops(drops, world, pos, original, fortune);
		}
	}
}

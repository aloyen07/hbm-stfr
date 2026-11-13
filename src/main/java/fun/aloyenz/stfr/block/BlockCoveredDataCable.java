package fun.aloyenz.stfr.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityDataCable;

/** Full-block casing around the same logical network node as the exposed cable. */
public final class BlockCoveredDataCable extends BlockContainer {

	public BlockCoveredDataCable(String name, boolean radiationShielded) {
		super(Material.IRON);
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setSoundType(SoundType.METAL);
		setHardness(radiationShielded ? 4F : 2F);
		setResistance(radiationShielded ? 90F : 8F);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityDataCable();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}
}

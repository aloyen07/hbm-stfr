package fun.aloyenz.stfr.block;

import com.hbm.api.block.IToolable.ToolType;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

public final class BlockStructureAnalyzer extends BlockContainer {

	public BlockStructureAnalyzer() {
		super(Material.IRON);
		setRegistryName(Tags.MODID, "structure_analyzer");
		setTranslationKey(Tags.MODID + ".structure_analyzer");
		setHardness(5F);
		setResistance(20F);
		setSoundType(SoundType.METAL);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityStructureAnalyzer();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		super.breakBlock(world, pos, state);
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
		if (hand == EnumHand.MAIN_HAND && player.getHeldItem(hand).isEmpty()) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TileEntityStructureAnalyzer)) return false;
			TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
			analyzer.toggleProjection();
			if (!world.isRemote) {
				player.sendMessage(new TextComponentTranslation(
						analyzer.isProjectionEnabled()
								? "message.hbmstfr.analyzer.projection_on"
								: "message.hbmstfr.analyzer.projection_off"));
				world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK,
						SoundCategory.BLOCKS, 0.6F, analyzer.isProjectionEnabled() ? 1.2F : 0.8F);
			}
			return true;
		}
		if (ToolType.getType(player.getHeldItem(hand)) != ToolType.SCREWDRIVER) return false;
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityStructureAnalyzer) {
			TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
			// Advance on both logical sides. This gives the client-side wand overlay
			// the new origin immediately; the server update remains authoritative.
			analyzer.advanceHeight();
			if (!world.isRemote) {
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.analyzer.height", analyzer.getHeightOffset()));
				world.playSound(
						null, pos, SoundEvents.UI_BUTTON_CLICK, SoundCategory.BLOCKS, 0.6F, 1.2F);
			}
		}
		return true;
	}
}

package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Reactor diagnosis and orientation repair. */
public final class ItemReactorAnalysisWand extends Item {

	public ItemReactorAnalysisWand() {
		setRegistryName(Tags.MODID, "reactor_analysis_wand");
		setTranslationKey(Tags.MODID + ".reactor_analysis_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	public static TileEntityStructureAnalyzer containingAnalyzer(World world, BlockPos pos) {
		for (TileEntity tile : world.loadedTileEntityList)
			if (tile instanceof TileEntityStructureAnalyzer) {
				TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
				if (analyzer.containsWorldPos(pos)) return analyzer;
			}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(
				new TextComponentTranslation("tooltip.hbmstfr.analysis.use").getFormattedText());
	}

	@Override
	public EnumActionResult onItemUseFirst(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumFacing side,
			float hitX,
			float hitY,
			float hitZ,
			EnumHand hand) {
		TileEntityStructureAnalyzer analyzer = containingAnalyzer(world, pos);
		if (analyzer == null || !player.canPlayerEdit(pos, side, player.getHeldItem(hand)))
			return EnumActionResult.SUCCESS;
		BlockPos local = analyzer.toLocal(pos);
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		if (cell == null || (StfrConfig.legacyWandFix && !analyzer.getMiscRole(local).isRotable()))
			return EnumActionResult.SUCCESS;
		IBlockState actual = world.getBlockState(pos);
		if (actual.getBlock() == STFRBlocks.ROTOR_PLACEHOLDER) return EnumActionResult.SUCCESS;
		if (StfrStructureValidator.isHatchRole(analyzer.getRole(local))
				&& actual.getBlock() == cell.expected.getBlock()) {
			IBlockState corrected =
					player.isSneaking()
							? StfrStructureValidator.toggleHatch(actual)
							: StfrStructureValidator.correctedHatch(actual, cell.expected);
			if (!world.isRemote && !corrected.equals(actual))
				world.setBlockState(pos, corrected, 3);
			return EnumActionResult.SUCCESS;
		}
		if (StfrConfig.legacyWandFix && StfrStructureValidator.matchesCell(
				cell, analyzer.getRole(local), actual, world, pos, analyzer))
			return EnumActionResult.SUCCESS;
		IBlockState reference = StfrStructureValidator.referenceState(cell, analyzer, actual);
		IBlockState corrected = StfrStructureValidator.correctedRotation(actual, reference);
		if (!actual.equals(corrected)
				&& (!StfrConfig.legacyWandFix || StfrStructureValidator.matchesCell(
						cell, analyzer.getRole(local), corrected, world, pos, analyzer))
				&& !world.isRemote) world.setBlockState(pos, corrected, 3);
		return EnumActionResult.SUCCESS;
	}
}

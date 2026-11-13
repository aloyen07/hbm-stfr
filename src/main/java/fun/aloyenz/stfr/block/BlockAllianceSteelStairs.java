package fun.aloyenz.stfr.block;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import java.util.List;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.crafting.StfrManualRecipes;

public final class BlockAllianceSteelStairs extends BlockStairs implements IToolable, ILookOverlay {

	private final boolean welded;

	public BlockAllianceSteelStairs(String name, boolean welded) {
		super(STFRBlocks.COMBINE_STEEL_RIVETED.getDefaultState());
		this.welded = welded;
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setCreativeTab(STFRBlocks.TAB);
		setHarvestLevel("pickaxe", 0);
	}

	@Override
	public List<ItemStack> getDrops(
			IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		return BlockAllianceSteel.drops(4, 2, welded ? 2 : 0);
	}

	@Override
	public boolean onScrew(
			World world,
			EntityPlayer player,
			int x,
			int y,
			int z,
			EnumFacing side,
			float hitX,
			float hitY,
			float hitZ,
			EnumHand hand,
			ToolType tool) {
		if (welded || tool != ToolType.TORCH || world.isRemote) return false;
		return StfrManualRecipes.apply(world, player, new BlockPos(x, y, z), tool);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
		if (!welded) ManualProcessingOverlay.print(event, world, pos);
	}
}

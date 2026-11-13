package fun.aloyenz.stfr.block;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
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

/** Riveted Alliance steel, with a welded variant produced by the HBM torch. */
public final class BlockAllianceSteel extends Block implements IToolable, ILookOverlay {

	private final boolean welded;

	public BlockAllianceSteel(String name, boolean welded) {
		super(Material.IRON);
		this.welded = welded;
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setSoundType(SoundType.METAL);
		setHardness(5.0F);
		setResistance(300.0F);
		setHarvestLevel("pickaxe", 0);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public List<ItemStack> getDrops(
			IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		return drops(8, 2, welded ? 3 : 0);
	}

	static List<ItemStack> drops(int alloyIngots, int duraBolts, int ferroSlag) {
		List<ItemStack> result = new ArrayList<>();
		result.add(new ItemStack(ModItems.ingot_combine_steel, alloyIngots));
		result.add(Mats.MAT_DURA.make(ModItems.bolt, duraBolts));
		if (ferroSlag > 0) result.add(Mats.MAT_FERRO.make(ModItems.scraps, ferroSlag));
		return result;
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

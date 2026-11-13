package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Creative authoring wand for physical failure effects; it never changes structure roles. */
public final class ItemReactorDestructionWand extends Item {

	private static final String MODE = "destructionMode";

	public ItemReactorDestructionWand() {
		setRegistryName(Tags.MODID, "reactor_destruction_wand");
		setTranslationKey(Tags.MODID + ".reactor_destruction_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	private static TileEntityStructureAnalyzer.DestructionMark mode(ItemStack stack) {
		try {
			return TileEntityStructureAnalyzer.DestructionMark.valueOf(
					stack.hasTagCompound() ? stack.getTagCompound().getString(MODE) : "");
		} catch (IllegalArgumentException ignored) {
			return TileEntityStructureAnalyzer.DestructionMark.DESTRUCTIBLE;
		}
	}

	private static void cycle(EntityPlayer player, ItemStack stack) {
		TileEntityStructureAnalyzer.DestructionMark[] values =
				TileEntityStructureAnalyzer.DestructionMark.values();
		TileEntityStructureAnalyzer.DestructionMark next =
				values[(mode(stack).ordinal() + 1) % values.length];
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString(MODE, next.name());
		player.sendMessage(
				new TextComponentTranslation("message.hbmstfr.destruction_wand.mode", next.name()));
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking() && !world.isRemote) cycle(player, stack);
		return new ActionResult<>(
				player.isSneaking() ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing face,
			float hitX,
			float hitY,
			float hitZ) {
		if (player.isSneaking()) {
			if (!world.isRemote) cycle(player, player.getHeldItem(hand));
			return EnumActionResult.SUCCESS;
		}
		TileEntityStructureAnalyzer analyzer = ItemReactorDebugWand.findNearestAnalyzer(world, pos);
		if (analyzer == null || !analyzer.containsWorldPos(pos)) return EnumActionResult.FAIL;
		if (!world.isRemote) {
			BlockPos local = analyzer.toLocal(pos);
			TileEntityStructureAnalyzer.DestructionMark selected = mode(player.getHeldItem(hand));
			analyzer.markDestruction(local, selected);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.destruction_wand.marked",
							selected.name(),
							local.getX(),
							local.getY(),
							local.getZ()));
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return super.getItemStackDisplayName(stack) + " [" + mode(stack).name() + "]";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(
				new TextComponentTranslation("tooltip.hbmstfr.destruction_wand.use")
						.getFormattedText());
	}
}

package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Authoring tool for the rotating block set and its face-based markers. */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class ItemReactorRotorWand extends Item {

	private static final String MODE = "rotorMode";

	public ItemReactorRotorWand() {
		setRegistryName(Tags.MODID, "reactor_rotor_wand");
		setTranslationKey(Tags.MODID + ".reactor_rotor_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void letWandMarkBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getItemStack().getItem() != STFRItems.REACTOR_ROTOR_WAND) return;
		// A turbine block may have its own right-click behavior. Let the item
		// author its annotation first, including on HBM blocks with GUIs.
		event.setUseBlock(Result.DENY);
		event.setUseItem(Result.ALLOW);
	}

	public static TileEntityStructureAnalyzer.RotorMark mode(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) return TileEntityStructureAnalyzer.RotorMark.ROTOR;
		try {
			return TileEntityStructureAnalyzer.RotorMark.valueOf(tag.getString(MODE));
		} catch (IllegalArgumentException ignored) {
			return TileEntityStructureAnalyzer.RotorMark.ROTOR;
		}
	}

	private static void cycle(EntityPlayer player, ItemStack stack) {
		TileEntityStructureAnalyzer.RotorMark[] values =
				TileEntityStructureAnalyzer.RotorMark.values();
		TileEntityStructureAnalyzer.RotorMark next =
				values[(mode(stack).ordinal() + 1) % values.length];
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString(MODE, next.name());
		player.sendMessage(
				new TextComponentTranslation("message.hbmstfr.rotor_wand.mode", next.name()));
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return super.getItemStackDisplayName(stack) + " [" + mode(stack).name() + "]";
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (!player.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, stack);
		if (!world.isRemote) cycle(player, stack);
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
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
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityStructureAnalyzer) {
			if (player.isSneaking() && !world.isRemote) {
				TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
				int changed = analyzer.saveRotorMap();
				analyzer.audit(player, true);
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.rotor_wand.saved", changed));
			}
			return EnumActionResult.SUCCESS;
		}
		if (player.isSneaking()) {
			if (!world.isRemote) cycle(player, player.getHeldItem(hand));
			return EnumActionResult.SUCCESS;
		}
		TileEntityStructureAnalyzer analyzer = ItemReactorDebugWand.findNearestAnalyzer(world, pos);
		if (analyzer == null
				|| !analyzer.containsWorldPos(pos)
				|| !analyzer.isTemplateCell(analyzer.toLocal(pos))) return EnumActionResult.FAIL;
		if (!world.isRemote) {
			analyzer.markRotor(analyzer.toLocal(pos), face, mode(player.getHeldItem(hand)));
			BlockPos local = analyzer.toLocal(pos);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.rotor_wand.marked",
							mode(player.getHeldItem(hand)).name(),
							local.getX(),
							local.getY(),
							local.getZ(),
							face.getName()));
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(
				new TextComponentTranslation("tooltip.hbmstfr.rotor_wand.use").getFormattedText());
	}
}

package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

public final class ItemReactorMiscWand extends Item {

	public ItemReactorMiscWand() {
		setRegistryName(Tags.MODID, "reactor_misc_wand");
		setTranslationKey(Tags.MODID + ".reactor_misc_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	public static StfrMiscRole selected(ItemStack stack) {
		try {
			return stack.hasTagCompound()
					? StfrMiscRole.valueOf(stack.getTagCompound().getString("miscRole"))
					: StfrMiscRole.UNDEFINED;
		} catch (IllegalArgumentException ignored) {
			return StfrMiscRole.UNDEFINED;
		}
	}

	private void cycle(EntityPlayer player, World world, ItemStack stack) {
		if (world.isRemote) return;
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		StfrMiscRole role = selected(stack).next();
		stack.getTagCompound().setString("miscRole", role.name());
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.wand.role", role.name()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(new TextComponentTranslation("tooltip.hbmstfr.misc.use").getFormattedText());
		tooltip.add(new TextComponentTranslation("tooltip.hbmstfr.misc.export").getFormattedText());
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return super.getItemStackDisplayName(stack) + " [" + selected(stack).name() + "]";
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		if (player.isSneaking()) cycle(player, world, player.getHeldItem(hand));
		return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
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
		if (player.isSneaking()) {
			if (world.getTileEntity(pos) instanceof TileEntityStructureAnalyzer) {
				if (!world.isRemote)
					((TileEntityStructureAnalyzer) world.getTileEntity(pos))
							.exportStructure(player);
			} else cycle(player, world, player.getHeldItem(hand));
			return EnumActionResult.SUCCESS;
		}
		if (!world.isRemote && player.canPlayerEdit(pos, side, player.getHeldItem(hand))) {
			TileEntityStructureAnalyzer analyzer =
					ItemReactorAnalysisWand.containingAnalyzer(world, pos);
			if (analyzer == null)
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.wand.no_analyzer"));
			else {
				StfrMiscRole role = selected(player.getHeldItem(hand));
				if (!analyzer.setMiscRole(analyzer.toLocal(pos), role))
					player.sendMessage(
							new TextComponentTranslation("message.hbmstfr.misc.invalid"));
				else
					player.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.misc.assigned", role.name()));
			}
		}
		return EnumActionResult.SUCCESS;
	}
}

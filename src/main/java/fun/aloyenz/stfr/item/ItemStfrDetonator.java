package fun.aloyenz.stfr.item;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.STFR;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.security.*;

public final class ItemStfrDetonator extends Item {

	public ItemStfrDetonator() {
		setRegistryName(Tags.MODID, "stfr_detonator");
		setTranslationKey(Tags.MODID + ".stfr_detonator");
		setCreativeTab(STFRBlocks.TAB);
		setMaxStackSize(1);
	}

	public EnumActionResult onItemUse(
			EntityPlayer p,
			World w,
			BlockPos pos,
			EnumHand hand,
			EnumFacing facing,
			float x,
			float y,
			float z) {
		if (w.getBlockState(pos).getBlock() != STFRBlocks.DETONATOR_CONNECTOR)
			return EnumActionResult.PASS;
		if (!w.isRemote) StfrDetonatorProgramming.insert(p, w, pos, hand);
		return EnumActionResult.SUCCESS;
	}

	public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
		ItemStack held = p.getHeldItem(hand);
		NBTTagCompound tag = held.getOrCreateSubCompound("hbmstfr");
		if (!w.isRemote) {
			if (!tag.hasUniqueId("network") || !tag.hasUniqueId("reactor"))
				StfrSecurity.message(p, "unprogrammed");
			else {
				if (!tag.hasUniqueId("device")) tag.setUniqueId("device", UUID.randomUUID());
				p.openGui(
						STFR.instance,
						1,
						w,
						hand == EnumHand.MAIN_HAND ? p.inventory.currentItem : 40,
						0,
						0);
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, held);
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World w, List<String> tooltip, ITooltipFlag flag) {
		NBTTagCompound tag = stack.getSubCompound("hbmstfr");
		if (tag != null && tag.hasUniqueId("network"))
			tooltip.add("Network: " + tag.getUniqueId("network"));
	}
}

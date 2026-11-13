package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public final class ItemStfrKeyCard extends Item {

	public ItemStfrKeyCard() {
		setRegistryName(Tags.MODID, "stfr_key_card");
		setTranslationKey(Tags.MODID + ".stfr_key_card");
		setCreativeTab(STFRBlocks.TAB);
		setMaxStackSize(1);
	}

	public void onCreated(ItemStack stack, World w, EntityPlayer p) {
		if (!w.isRemote) StfrSecurity.cardId(stack);
	}

	public void onUpdate(ItemStack stack, World w, Entity e, int slot, boolean held) {
		if (!w.isRemote) StfrSecurity.cardId(stack);
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
		if (w.isRemote)
			return w.getTileEntity(pos) instanceof TileEntityLogicDevice
					? EnumActionResult.SUCCESS
					: EnumActionResult.PASS;
		return StfrSecurity.useCard(p, w, pos, p.getHeldItem(hand))
				? EnumActionResult.SUCCESS
				: EnumActionResult.PASS;
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World w, List<String> tooltip, ITooltipFlag flag) {
		NBTTagCompound tag = stack.getSubCompound("hbmstfr");
		if (tag != null && tag.hasUniqueId("card")) tooltip.add("UUID: " + tag.getUniqueId("card"));
	}
}

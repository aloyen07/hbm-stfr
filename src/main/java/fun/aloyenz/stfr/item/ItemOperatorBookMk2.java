package fun.aloyenz.stfr.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import fun.aloyenz.stfr.STFR;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;

/** Read-only HBM-style field manual. Its pages live in the packaged book resource. */
public final class ItemOperatorBookMk2 extends Item {

	public ItemOperatorBookMk2(String name) {
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		if (world.isRemote)
			player.openGui(STFR.instance, 3, world, hand == EnumHand.MAIN_HAND ? 0 : 1, 0, 0);
		return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
	}
}

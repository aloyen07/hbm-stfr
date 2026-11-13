package fun.aloyenz.stfr.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Creative-only probe for the two digamma scream sound paths. */
public final class ItemStfrScreamWand extends Item {

	public ItemStfrScreamWand() {
		setRegistryName(Tags.MODID, "stfr_scream_wand");
		setTranslationKey(Tags.MODID + ".stfr_scream_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing facing,
			float x,
			float y,
			float z) {
		if (world.getBlockState(pos).getBlock() != STFRBlocks.MULTI_FLUID_PORT)
			return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		if (!player.capabilities.isCreativeMode) {
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.simulation.creative"));
			return EnumActionResult.FAIL;
		}
		StfrReactor reactor = StfrManager.get(world).forPort(pos);
		if (reactor == null) {
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.control_wand.no_reactor"));
			return EnumActionResult.FAIL;
		}
		reactor.playDigammaScream(world, player.isSneaking());
		return EnumActionResult.SUCCESS;
	}

	@Override
	public boolean canDestroyBlockInCreative(
			World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
		return false;
	}
}

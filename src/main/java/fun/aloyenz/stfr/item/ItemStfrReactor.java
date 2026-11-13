package fun.aloyenz.stfr.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Creative/command construction item; its only blueprint is the live STFR template. */
public final class ItemStfrReactor extends Item {

	public ItemStfrReactor() {
		setRegistryName(Tags.MODID, "stfr_reactor");
		setTranslationKey(Tags.MODID + ".stfr_reactor");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(I18n.format("item.hbmstfr.stfr_reactor.help"));
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos clicked,
			EnumHand hand,
			EnumFacing facing,
			float hitX,
			float hitY,
			float hitZ) {
		BlockPos base = clicked.offset(facing);
		BlockPos origin = base.add(-StfrStructureTemplate.CENTER.getX(), 0,
				-StfrStructureTemplate.CENTER.getZ());
		BlockPos end = origin.add(StfrStructureTemplate.SIZE - 1,
				StfrStructureTemplate.SIZE - 1, StfrStructureTemplate.SIZE - 1);
		if (!world.isAreaLoaded(origin, end)
				|| origin.getY() < 0 || end.getY() >= world.getActualHeight()) {
			if (!world.isRemote)
				player.sendMessage(new TextComponentTranslation("message.hbmstfr.reactor_item.area"));
			return EnumActionResult.FAIL;
		}
		for (BlockPos pos : BlockPos.getAllInBox(origin, end)) {
			if (!world.isAirBlock(pos)) {
				if (!world.isRemote)
					player.sendMessage(new TextComponentTranslation("message.hbmstfr.reactor_item.occupied"));
				return EnumActionResult.FAIL;
			}
		}
		ItemStack held = player.getHeldItem(hand);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!player.canPlayerEdit(origin.add(cell.localPos), facing, held)) {
				if (!world.isRemote)
					player.sendMessage(new TextComponentTranslation("message.hbmstfr.reactor_item.denied"));
				return EnumActionResult.FAIL;
			}
		}
		if (world.isRemote) return EnumActionResult.SUCCESS;
		List<BlockSnapshot> previous = new ArrayList<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			BlockPos target = origin.add(cell.localPos);
			IBlockState state = cell.expected;
			previous.add(BlockSnapshot.getBlockSnapshot(world, target));
			if (!world.setBlockState(target, state, 2)) {
				rollback(world, previous);
				return EnumActionResult.FAIL;
			}
		}
		if (ForgeEventFactory.onPlayerMultiBlockPlace(player, previous, facing, hand).isCanceled()) {
			rollback(world, previous);
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.reactor_item.denied"));
			return EnumActionResult.FAIL;
		}
		StfrManager.get(world).getOrCreate(origin.add(StfrStructureTemplate.CENTER));
		if (!player.capabilities.isCreativeMode) held.shrink(1);
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.reactor_item.built"));
		return EnumActionResult.SUCCESS;
	}

	private static void rollback(World world, List<BlockSnapshot> previous) {
		for (BlockSnapshot snapshot : previous) snapshot.restore(true, false);
	}
}

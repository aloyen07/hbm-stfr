package fun.aloyenz.stfr.item;

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
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Temporary control interface until the data-cable control panel is available. */
public final class ItemReactorControlWand extends Item {

	private enum Command {
		KEY,
		SSTB,
		BREAKERS,
		PRIME,
		CONFIRM,
		SHUTDOWN
	}

	public ItemReactorControlWand() {
		setRegistryName(Tags.MODID, "reactor_control_wand");
		setTranslationKey(Tags.MODID + ".reactor_control_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	private static Command command(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return Command.values()[
				Math.floorMod(
						tag == null ? 0 : tag.getInteger("command"), Command.values().length)];
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking() && !world.isRemote) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag == null) {
				tag = new NBTTagCompound();
				stack.setTagCompound(tag);
			}
			tag.setInteger("command", (command(stack).ordinal() + 1) % Command.values().length);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.control_wand.selected", command(stack).name()));
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing facing,
			float hitX,
			float hitY,
			float hitZ) {
		if (world.getBlockState(pos).getBlock() != STFRBlocks.MULTI_FLUID_PORT)
			return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		StfrManager manager = StfrManager.get(world);
		StfrReactor reactor = null;
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			BlockPos center = pos.subtract(cell.localPos.subtract(StfrStructureTemplate.CENTER));
			reactor = manager.get(center);
			if (reactor == null) reactor = manager.get(center.offset(direction.getOpposite()));
			if (reactor != null) break;
		}
		if (reactor == null) {
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.control_wand.no_reactor"));
			return EnumActionResult.SUCCESS;
		}
		reactor.refresh(world);
		Command selected = command(player.getHeldItem(hand));
		boolean accepted = true;
		switch (selected) {
			case KEY:
				reactor.setKeyOn(!reactor.keyOn());
				break;
			case SSTB:
				accepted = reactor.pulseSstb(world, pos).accepted;
				break;
			case BREAKERS:
				reactor.setBreakersOn(!reactor.breakersOn());
				break;
			case PRIME:
				accepted = reactor.prime();
				break;
			case CONFIRM:
				accepted = reactor.confirm(world);
				break;
			case SHUTDOWN:
				accepted = reactor.shutdown(world);
				break;
			default:
				accepted = false;
		}
		manager.markDirty();
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.control_wand.status",
						selected.name(),
						accepted ? "OK" : "DENIED",
						reactor.runState().name(),
						reactor.stateTicks(),
						reactor.keyOn(),
						reactor.breakersOn(),
						reactor.workingTurbines(),
						reactor.sealFailures()));
		return EnumActionResult.SUCCESS;
	}
}

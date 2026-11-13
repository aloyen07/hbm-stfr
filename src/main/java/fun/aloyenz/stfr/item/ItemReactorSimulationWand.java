package fun.aloyenz.stfr.item;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** All selection and execution happens on a registered STFR port. */
public final class ItemReactorSimulationWand extends Item {

	public ItemReactorSimulationWand() {
		setRegistryName(Tags.MODID, "reactor_simulation_wand");
		setTranslationKey(Tags.MODID + ".reactor_simulation_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	private static StfrReactor.RunState selected(ItemStack stack) {
		int index =
				stack.hasTagCompound() ? stack.getTagCompound().getInteger("simulationState") : 0;
		return StfrReactor.RunState.values()[
				Math.floorMod(index, StfrReactor.RunState.values().length)];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(I18n.format("message.hbmstfr.simulation.selected", selected(stack).name()));
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
		return click(player, world, pos, hand, false);
	}

	public EnumActionResult click(
			EntityPlayer player, World world, BlockPos pos, EnumHand hand, boolean left) {
		if (world.getBlockState(pos).getBlock() != STFRBlocks.MULTI_FLUID_PORT)
			return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		if (!player.capabilities.isCreativeMode) {
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.simulation.creative"));
			return EnumActionResult.FAIL;
		}
		StfrManager manager = StfrManager.get(world);
		StfrReactor reactor = manager.forPort(pos);
		if (reactor == null) {
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.control_wand.no_reactor"));
			return EnumActionResult.FAIL;
		}
		ItemStack stack = player.getHeldItem(hand);
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound tag = stack.getTagCompound();
		if (player.isSneaking()) {
			StfrReactor.RunState[] states = StfrReactor.RunState.values();
			int index = Math.floorMod(selected(stack).ordinal() + (left ? -1 : 1), states.length);
			tag.setInteger("simulationState", index);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.simulation.selected", states[index].name()));
		} else if (left) {
			EnumFacing direction = reactor.portDirection(pos);
			boolean created =
					direction != null
							&& reactor.maintenance()
									.simulate(world, StfrReactor.maintenanceComponent(direction));
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.simulation.maintenance",
							direction,
							created ? "OK" : "DENIED"));
			manager.markDirty();
		} else {
			StfrReactor.RunState state = selected(stack);
			reactor.simulate(world, state);
			manager.markDirty();
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.simulation.applied", state.name()));
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	public boolean canDestroyBlockInCreative(
			World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
		return false;
	}
}

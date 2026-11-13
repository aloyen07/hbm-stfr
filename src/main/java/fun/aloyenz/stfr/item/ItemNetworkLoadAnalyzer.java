package fun.aloyenz.stfr.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.BlockLogicMainframe;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;

public final class ItemNetworkLoadAnalyzer extends Item {

	public ItemNetworkLoadAnalyzer() {
		setRegistryName(Tags.MODID, "network_load_analyzer");
		setTranslationKey(Tags.MODID + ".network_load_analyzer");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public EnumActionResult onItemUseFirst(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumFacing face,
			float x,
			float y,
			float z,
			EnumHand hand) {
		if (!(world.getBlockState(pos).getBlock() instanceof BlockLogicMainframe))
			return EnumActionResult.PASS;
		if (!world.isRemote) {
			StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.network_load.slots",
							network.slotsUsed,
							network.slotCapacity,
							network.slotDemand,
							network.installedSlotCapacity));
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.network_load.frames",
							network.activeMainframes,
							network.mainframes,
							network.powerRequired));
			String issue =
					network.issue.toLowerCase(Locale.ROOT).replace(' ', '_').replace(':', '_');
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.network.issue." + issue));
			List<Map.Entry<BlockPos, String>> faults =
					new ArrayList<>(network.unavailableDevices.entrySet());
			faults.sort(Comparator.comparingLong(entry -> entry.getKey().toLong()));
			for (Map.Entry<BlockPos, String> fault :
					faults.subList(0, Math.min(10, faults.size()))) {
				BlockPos at = fault.getKey();
				ITextComponent name =
						new TextComponentTranslation(
								world.getBlockState(at).getBlock().getTranslationKey() + ".name");
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.network_load.fault",
								name,
								at.getX(),
								at.getY(),
								at.getZ(),
								new TextComponentTranslation(
										"message.hbmstfr.network_load.reason."
												+ fault.getValue())));
			}
			if (faults.size() > 10)
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.network_load.more", faults.size() - 10));
			if (faults.isEmpty() && !network.ready())
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.network_load.check_cable"));
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(
				new TextComponentTranslation("tooltip.hbmstfr.network_load.use")
						.getFormattedText());
	}
}

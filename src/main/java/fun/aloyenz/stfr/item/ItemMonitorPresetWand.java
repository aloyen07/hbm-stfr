package fun.aloyenz.stfr.item;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Applies useful four-line layouts without requiring the monitor text editor. */
public final class ItemMonitorPresetWand extends Item {

	private static final String[][] PRESETS = {
		{"&bFuel Injection", "$FUEL", "Fuel $FTYPE", "Left $FLEFT"},
		{"&bStab. Offset", "$OFFSET", "Spin $RPM RPM", "Turb $TURB"},
		{"&bRF Suppress", "$RF", "Rate $RR", "Gain $RXHE"},
		{"&bPower Lasers", "$LASER", "CoreT $CTEMP", "Cap $CAP"},
		{"&bPressure Vent", "$VENT", "CaseP $PRESS", "OutHe4 $PROD"},
		{"&bShield Perm", "$PERM", "CoreHe4 $CHE4", "CaseHe4 $CASEHE4"},
		{"&bReactor Status", "$STATE", "Core $CORE", "Case $SEAL"},
		{"&bShield Status", "$SSTATUS", "Intens $SHIELD", "MSSR $MSSR"},
		{"&bShield Stress", "Total $SSTRESS", "P $PSTRESS S $SPSTRESS", "RGDT $RSTRESS"},
		{"&bCore Thermals", "RR $RR", "HTR $HTR MW", "GAIN $RXHE MW"},
		{"&bGenerating", "Rad $RAD MCi", "Cool $COOL MW", "Out $GEN MWe"},
		{"&bCapacitor", "Charge $CAP", "Energy $POWER", "Time $UPTIME"},
		{"&bCore", "Spin $RPM RPM", "T $CTEMP kC", "P $CPRESS kPa"},
		{"&bCase", "Pres $PRESS kPa", "Temp $TEMP C", "Stress $SSTRESS"},
		{"D $DEUT mB", "He $HE4MB mB", "Po $PO210 mB", "Ya $YARONITE mB"},
		{"&bReactivity", "$REACT", "Temp $CTEMP", "RR $RR"},
		{"&bLubricant", "$LUBE/6000", "Coolant", "$WATER mB"},
		{"&bCooling", "Case $TEMP C", "Water $WATER", "Steam $STEAM"},
		{"&bStructure", "Turbines $TURB", "Seal err $SEAL", "Block err $ERR"},
		{"&bStartup", "Key $KEY", "SSTB $SSTB", "Breaker $BRK"}
	};

	public ItemMonitorPresetWand() {
		setRegistryName(Tags.MODID, "monitor_preset_wand");
		setTranslationKey(Tags.MODID + ".monitor_preset_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	private static TextComponentTranslation presetName(int index) {
		return new TextComponentTranslation("item.hbmstfr.monitor_preset_wand.preset." + (index + 1));
	}

	private static int selected(ItemStack stack) {
		return stack.hasTagCompound()
				? Math.floorMod(stack.getTagCompound().getInteger("preset"), PRESETS.length)
				: 0;
	}

	private static void cycle(ItemStack stack, EntityPlayer player) {
		NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
		int next = (selected(stack) + 1) % PRESETS.length;
		tag.setInteger("preset", next);
		stack.setTagCompound(tag);
		player.sendStatusMessage(
				new TextComponentTranslation(
						"message.hbmstfr.preset.selected",
						next + 1,
						PRESETS.length,
						presetName(next)),
				true);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking() && !world.isRemote) cycle(stack, player);
		return new ActionResult<>(
				player.isSneaking() ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
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
		ItemStack stack = player.getHeldItem(hand);
		if (player.isSneaking()) {
			if (!world.isRemote) cycle(stack, player);
			return EnumActionResult.SUCCESS;
		}
		if (!(world.getTileEntity(pos) instanceof TileEntityLogicDevice))
			return EnumActionResult.PASS;
		TileEntityLogicDevice monitor = (TileEntityLogicDevice) world.getTileEntity(pos);
		if (monitor.kind() != BlockLogicDevice.Kind.MONITOR) return EnumActionResult.PASS;
		if (!world.isRemote) {
			int preset = selected(stack);
			monitor.setMonitorText(PRESETS[preset]);
			player.sendStatusMessage(
					new TextComponentTranslation(
							"message.hbmstfr.preset.applied",
							preset + 1,
							presetName(preset)),
					true);
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		int preset = selected(stack);
		tooltip.add(
				(preset + 1)
						+ "/"
						+ PRESETS.length
						+ " · "
						+ presetName(preset).getFormattedText());
		tooltip.add(
				new TextComponentTranslation("item.hbmstfr.monitor_preset_wand.help")
						.getFormattedText());
	}
}

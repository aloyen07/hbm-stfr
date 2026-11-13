package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

public final class ItemReactorDebugWand extends Item {

	private static final String TAG_ROLE = "selectedRole";
	private static final String TAG_SYMMETRY = "radialSymmetry";

	public ItemReactorDebugWand() {
		setRegistryName(Tags.MODID, "reactor_debug_wand");
		setTranslationKey(Tags.MODID + ".reactor_debug_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	public static StfrBlockRole getSelectedRole(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null) return StfrBlockRole.EXACT;
		try {
			return StfrBlockRole.valueOf(tag.getString(TAG_ROLE));
		} catch (IllegalArgumentException exception) {
			return StfrBlockRole.EXACT;
		}
	}

	private static void setSelectedRole(ItemStack stack, StfrBlockRole role) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString(TAG_ROLE, role.name());
	}

	public static boolean isRadialSymmetryEnabled(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG_SYMMETRY);
	}

	private static boolean toggleRadialSymmetry(ItemStack stack) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		boolean enabled = !stack.getTagCompound().getBoolean(TAG_SYMMETRY);
		stack.getTagCompound().setBoolean(TAG_SYMMETRY, enabled);
		return enabled;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return super.getItemStackDisplayName(stack)
				+ " ["
				+ getSelectedRole(stack).name()
				+ (isRadialSymmetryEnabled(stack) ? " ×6" : "")
				+ "]";
	}

	private static void selectNextRole(EntityPlayer player, World world, ItemStack wand) {
		StfrBlockRole role = getSelectedRole(wand).nextEditable();
		setSelectedRole(wand, role);
		if (!world.isRemote)
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.wand.role", role.name()));
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(
			World world, EntityPlayer player, EnumHand hand) {
		ItemStack wand = player.getHeldItem(hand);
		if (player.isSneaking()) {
			selectNextRole(player, world, wand);
			return new ActionResult<>(EnumActionResult.SUCCESS, wand);
		}
		return new ActionResult<>(EnumActionResult.PASS, wand);
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
		ItemStack wand = player.getHeldItem(hand);
		TileEntity clicked = world.getTileEntity(pos);
		if (clicked instanceof TileEntityStructureAnalyzer) {
			if (player.isSneaking()) {
				if (!world.isRemote) ((TileEntityStructureAnalyzer) clicked).audit(player, true);
			} else {
				boolean enabled = toggleRadialSymmetry(wand);
				if (!world.isRemote)
					player.sendMessage(
							new TextComponentTranslation(
									enabled
											? "message.hbmstfr.wand.symmetry_on"
											: "message.hbmstfr.wand.symmetry_off"));
			}
			return EnumActionResult.SUCCESS;
		}

		if (player.isSneaking()) {
			selectNextRole(player, world, wand);
			return EnumActionResult.SUCCESS;
		}

		TileEntityStructureAnalyzer analyzer = findNearestAnalyzer(world, pos);
		StfrBlockRole role = getSelectedRole(wand);
		BlockPos target = pos;
		if (analyzer != null
				&& (role.requiresAir() || role == StfrBlockRole.UNASSIGNED)
				&& analyzer.containsWorldPos(pos)
				&& analyzer.isTemplateCell(analyzer.toLocal(pos))) {
			BlockPos adjacent = pos.offset(facing);
			if (analyzer.containsWorldPos(adjacent)
					&& !analyzer.isTemplateCell(analyzer.toLocal(adjacent))
					&& (role.requiresAir()
							|| analyzer.getRole(analyzer.toLocal(adjacent)).requiresAir())) {
				target = adjacent;
			}
		}
		if (analyzer == null || !analyzer.containsWorldPos(target)) {
			if (!world.isRemote)
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.wand.no_analyzer"));
			return EnumActionResult.FAIL;
		}
		if (role.requiresAir() && analyzer.isTemplateCell(analyzer.toLocal(target))) {
			if (!world.isRemote)
				player.sendMessage(new TextComponentTranslation("message.hbmstfr.wand.air_target"));
			return EnumActionResult.FAIL;
		}
		if (!world.isRemote) {
			BlockPos local = analyzer.toLocal(target);
			StfrBlockRole old = analyzer.getRole(local);
			int changed =
					isRadialSymmetryEnabled(wand)
							? analyzer.setRadialRole(
									local, target.equals(pos) ? null : analyzer.toLocal(pos), role)
							: 1;
			if (!isRadialSymmetryEnabled(wand)) analyzer.setRole(local, role);
			if (isRadialSymmetryEnabled(wand)) {
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.wand.symmetry_count", changed));
			} else {
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.wand.assigned",
								local.getX(),
								local.getY(),
								local.getZ(),
								old.name(),
								role.name()));
			}
		}
		return EnumActionResult.SUCCESS;
	}

	@Nullable
	public static TileEntityStructureAnalyzer findNearestAnalyzer(World world, BlockPos pos) {
		TileEntityStructureAnalyzer best = null;
		double bestDistance = 96D * 96D;
		for (TileEntity tile : world.loadedTileEntityList) {
			if (!(tile instanceof TileEntityStructureAnalyzer)) continue;
			double distance = tile.getPos().distanceSq(pos);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = (TileEntityStructureAnalyzer) tile;
			}
		}
		return best;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(
				TextFormatting.AQUA
						+ new TextComponentTranslation(
										"tooltip.hbmstfr.wand.role", getSelectedRole(stack).name())
								.getFormattedText());
		tooltip.add(
				TextFormatting.GRAY
						+ new TextComponentTranslation("tooltip.hbmstfr.wand.use")
								.getFormattedText());
	}
}

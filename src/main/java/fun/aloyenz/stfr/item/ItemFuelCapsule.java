package fun.aloyenz.stfr.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;

/** Portable 100-bucket fuel capsule. Fluid identity and fill grade live on the stack. */
public final class ItemFuelCapsule extends Item {

	public static final int CAPACITY = 100_000;
	private static final String TAG_TYPE = "stfrFillType";
	private static final String TAG_GRADE = "stfrFillGrade";

	public ItemFuelCapsule() {
		setRegistryName(Tags.MODID, "fuel_capsule");
		setTranslationKey(Tags.MODID + ".fuel_capsule");
		setMaxStackSize(1);
		setHasSubtypes(true);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
		return new CapsuleFluidHandler(stack);
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (isInCreativeTab(tab)) {
			items.add(new ItemStack(this));
			ItemStack water = new ItemStack(this);
			fillInternal(water, new FluidStack(FluidRegistry.WATER, CAPACITY));
			items.add(water);
		}
	}

	/** Machine-only filling path. Public fluid capabilities remain drain-only. */
	public static int fillInternal(ItemStack stack, FluidStack fluid) {
		IFluidHandlerItem handler =
				stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
		return handler instanceof CapsuleFluidHandler
				? ((CapsuleFluidHandler) handler).fillFromMachine(fluid) : 0;
	}

	@Override
	public int getMetadata(int damage) {
		return damage & 1;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		FluidStack fluid = getFluid(stack);
		return fluid != null && fluid.amount > 0 && fluid.amount < CAPACITY;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		FluidStack fluid = getFluid(stack);
		return fluid == null ? 1D : 1D - Math.min(1D, (double) fluid.amount / CAPACITY);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(
			ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		FluidStack fluid = getFluid(stack);
		if (fluid == null || fluid.amount <= 0) {
			tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.hbmstfr.fuel_capsule.empty"));
		} else {
			tooltip.add(TextFormatting.BLUE + fluid.getLocalizedName());
			tooltip.add(
					TextFormatting.GRAY + String.format("%,d / %,d mB", fluid.amount, CAPACITY));
		}
		tooltip.add(
				TextFormatting.DARK_GRAY
						+ I18n.format(
								"tooltip.hbmstfr.fuel_capsule.grade", getFillGrade(stack), 5));
	}

	@Nullable
	public static FluidStack getFluid(ItemStack stack) {
		if (!stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null))
			return null;
		IFluidHandlerItem handler =
				stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
		if (handler == null || handler.getTankProperties().length == 0) return null;
		return handler.getTankProperties()[0].getContents();
	}

	public static int getFillGrade(ItemStack stack) {
		NBTTagCompound tag = stack.getSubCompound("hbmstfr");
		return tag == null ? 0 : tag.getByte(TAG_GRADE) & 255;
	}

	public static String getFillType(ItemStack stack) {
		NBTTagCompound tag = stack.getSubCompound("hbmstfr");
		return tag == null ? "empty" : tag.getString(TAG_TYPE);
	}

	private static final class CapsuleFluidHandler extends FluidHandlerItemStack {

		private CapsuleFluidHandler(ItemStack container) {
			super(container, CAPACITY);
		}

		@Override
		public int fill(@Nullable FluidStack resource, boolean doFill) {
			return 0;
		}

		private int fillFromMachine(@Nullable FluidStack resource) {
			return super.fill(resource, true);
		}

		@Override
		protected void setFluid(@Nullable FluidStack fluid) {
			super.setFluid(fluid);
			NBTTagCompound tag = container.getOrCreateSubCompound("hbmstfr");
			if (fluid == null || fluid.amount <= 0) {
				tag.setString(TAG_TYPE, "empty");
				tag.setByte(TAG_GRADE, (byte) 0);
			} else {
				tag.setString(TAG_TYPE, fluid.getFluid().getName());
				tag.setByte(
						TAG_GRADE,
						(byte) Math.min(5, (fluid.amount * 5 + CAPACITY - 1) / CAPACITY));
			}
		}
	}
}

package fun.aloyenz.stfr.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.junit.jupiter.api.Test;

public final class StfrCapsuleFillingTest {
	@Test
	void selectedWaterModeIgnoresLeftoverDeuterium() {
		assertEquals(1000, TileEntityCapsuleFiller.Mode.WATER.amount(0, 100000, 16000, 1000, 100));
		assertEquals(0, TileEntityCapsuleFiller.Mode.WATER.amount(0, 100000, 16000, 1000, 0));
		assertEquals(0, TileEntityCapsuleFiller.Mode.UNSELECTED.amount(0, 100000, 16000, 16000, 16000));
		assertEquals(1000, TileEntityCapsuleFiller.Mode.DEUTERIUM.amount(0, 100000, 1000, 0, 0));
	}

	@Test
	void waterIsStoredAndSurvivesReloadWhilePublicFillingStaysDisabled() throws Exception {
		Bootstrap.register();
		Constructor<?> constructor = Class.forName(
				"fun.aloyenz.stfr.item.ItemFuelCapsule$CapsuleFluidHandler")
				.getDeclaredConstructor(ItemStack.class);
		constructor.setAccessible(true);
		Method machineFill = constructor.getDeclaringClass()
				.getDeclaredMethod("fillFromMachine", FluidStack.class);
		machineFill.setAccessible(true);
		ItemStack stack = new ItemStack(Items.STICK, 1, 1);
		IFluidHandlerItem handler =
				(IFluidHandlerItem) constructor.newInstance(stack);
		int water = 100000, nitrogen = 10000;
		for (int tick = 0; tick < 100; tick++) {
			int amount = TileEntityCapsuleFiller.Mode.WATER.amount(tick * 1000, 100000, 16000, water, nitrogen);
			int accepted = (Integer) machineFill.invoke(handler,
					new FluidStack(FluidRegistry.WATER, amount));
			assertEquals(1000, accepted);
			water -= accepted;
			nitrogen -= StfrCapsuleFilling.nitrogenCost(accepted);
		}
		assertEquals(0, water);
		assertEquals(0, nitrogen);
		IFluidHandlerItem reloaded =
				(IFluidHandlerItem) constructor.newInstance(stack.copy());
		assertEquals(100000, reloaded.getTankProperties()[0].getContents().amount);
		assertEquals(FluidRegistry.WATER, reloaded.getTankProperties()[0].getContents().getFluid());
		assertEquals(0, reloaded.fill(new FluidStack(FluidRegistry.WATER, 1000), true));
	}
	@Test
	void capsEveryTickAtOneBucket() {
		assertEquals(1000, StfrCapsuleFilling.amount(0, 100_000, 50_000, 50_000));
		assertEquals(300, StfrCapsuleFilling.amount(99_700, 100_000, 50_000, 50_000));
		assertEquals(0, StfrCapsuleFilling.amount(100_000, 100_000, 50_000, 50_000));
	}

	@Test
	void waterRequiresBothPressurizedInputs() {
		assertEquals(0, StfrCapsuleFilling.waterAmount(0, 100_000, 20_000, 0));
		assertEquals(400, StfrCapsuleFilling.waterAmount(0, 100_000, 20_000, 40));
		assertEquals(1000, StfrCapsuleFilling.waterAmount(0, 100_000, 1000, 100));
		assertEquals(100, StfrCapsuleFilling.nitrogenCost(1000));
		assertEquals(1, StfrCapsuleFilling.nitrogenCost(1));
	}
}

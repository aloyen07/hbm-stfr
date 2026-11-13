package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.misc.EnumSymbol;
import java.util.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.logic.StfrMainframePower;
import fun.aloyenz.stfr.logic.StfrNetworkCapacity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Physical product accounting, whole-device shedding and per-frame energy billing. */
public final class StfrNetworkMatterAcceptanceTest {

	private static void check(boolean value, String message) {
		Assertions.assertTrue(value, message);
	}

	private static void near(double actual, double expected, String message) {
		check(Math.abs(actual - expected) < 1e-7, message + ": " + actual + " vs " + expected);
	}

	private static final class Storage implements StfrThermalResources {

		int helium, room;

		public int waterAmount() {
			return 0;
		}

		public int steamRoom() {
			return 0;
		}

		public int productRoom() {
			return room;
		}

		public void consumeWater(int amount) {
			check(amount == 0, "No phantom coolant");
		}

		public int produceSteam(int amount) {
			return 0;
		}

		public int produceHelium(int amount) {
			int n = Math.min(amount, room);
			helium += n;
			room -= n;
			return n;
		}
	}

	@Test
	void acceptanceScenarios() {
		Bootstrap.register();
		List<StfrNetworkCapacity.Device> devices =
				Arrays.asList(
						new StfrNetworkCapacity.Device(10, 1, 0), // detonator
						new StfrNetworkCapacity.Device(20, 1, 1), // receiver
						new StfrNetworkCapacity.Device(30, 1, 2), // control
						new StfrNetworkCapacity.Device(40, 4, 3), // autopilot
						new StfrNetworkCapacity.Device(50, 2, 4)); // port
		for (int capacity = 9; capacity >= 0; capacity--) {
			StfrNetworkCapacity.Allocation a = StfrNetworkCapacity.allocate(capacity, devices);
			check(
					a.demand == 9 && a.used <= capacity,
					"Demand includes disconnected devices; capacity never exceeded");
			if (capacity == 8)
				check(
						!a.enabled.contains(10L) && a.enabled.contains(20L),
						"Detonator is shed first");
			if (capacity == 7)
				check(
						!a.enabled.contains(20L) && a.enabled.contains(30L),
						"Receiver is shed before controls");
			if (capacity == 6)
				check(
						a.enabled.equals(new HashSet<>(Arrays.asList(40L, 50L))),
						"Ports and autopilot retained before controls");
			if (capacity == 2)
				check(a.enabled.equals(Collections.singleton(50L)), "Port survives until last");
			if (capacity < 2)
				check(!a.enabled.contains(50L), "Ports require their complete two-slot allocation");
			List<StfrNetworkCapacity.Device> reversed = new ArrayList<>(devices);
			Collections.reverse(reversed);
			check(
					StfrNetworkCapacity.allocate(capacity, reversed).enabled.equals(a.enabled),
					"Allocation independent of graph traversal order");
		}
		check(
				StfrNetworkCapacity.allocate(
								4,
								Arrays.asList(
										new StfrNetworkCapacity.Device(2, 4, 3),
										new StfrNetworkCapacity.Device(1, 4, 3)))
						.enabled
						.equals(Collections.singleton(1L)),
				"Equal-priority devices have stable ordering");
		TileEntity.register("hbmstfr:logic_device", TileEntityLogicDevice.class);
		TileEntityLogicDevice first = new TileEntityLogicDevice(),
				second = new TileEntityLogicDevice(),
				third = new TileEntityLogicDevice();
		List<TileEntityLogicDevice> rack = Arrays.asList(first, second, third);
		StfrConfig.mainframeSlots = 2;
		StfrConfig.mainframePowerPerTick = 5000;
		check(
				StfrMainframePower.slots(true) == 9999 && StfrMainframePower.energyCost(true) == 0,
				"Creative frames supply 9999 slots without consuming HE");
		check(
				StfrMainframePower.slots(false) == 2
						&& StfrMainframePower.energyCost(false) == 5000,
				"Survival frames retain configured costs");
		StfrConfig.mainframeSlots = 7;
		StfrConfig.mainframePowerPerTick = 1234;
		check(
				StfrMainframePower.slots(true) == 9999 && StfrMainframePower.energyCost(true) == 0,
				"Config cannot charge creative frames or change their capacity");
		check(
				StfrMainframePower.slots(false) == 7
						&& StfrMainframePower.energyCost(false) == 1234,
				"Config changes apply only to survival frames");
		StfrConfig.mainframeSlots = 2;
		StfrConfig.mainframePowerPerTick = 5000;
		first.setMainframeStoredEnergy(20000);
		StfrMainframePower.billTick(rack, 1);
		near(total(rack), 5000, "Three frames each pay 5000 HE");
		for (TileEntityLogicDevice frame : rack)
			check(powered(frame), "Every funded frame powered");
		StfrMainframePower.billTick(rack, 1);
		near(total(rack), 5000, "Inspection cannot double-charge a tick");
		StfrMainframePower.billTick(rack, 2);
		near(total(rack), 0, "Only available energy is billed");
		check(
				powered(first) && !powered(second) && !powered(third),
				"Unfunded frames provide no slots");
		TileEntityLogicDevice fourth = new TileEntityLogicDevice();
		fourth.setMainframeStoredEnergy(5000);
		StfrMainframePower.billTick(Arrays.asList(first, second, third, fourth), 2);
		check(
				powered(fourth) && total(Arrays.asList(first, second, third, fourth)) == 0,
				"New frame in same tick pays without rebilling existing members");
		first.setMainframeStoredEnergy(12345);
		TileEntityLogicDevice reloaded = new TileEntityLogicDevice();
		reloaded.readFromNBT(first.writeToNBT(new NBTTagCompound()));
		check(
				reloaded.mainframeStoredEnergy() == 12345
						&& reloaded.mainframePowerTick() == Long.MIN_VALUE,
				"Stored HE persists and billing resumes after reload");
		Fluids.WATER = new FluidType("STFR_TEST_WATER", 0, 0, 0, 0, EnumSymbol.NONE);
		for (double yield : new double[] {.9, .4, 0, 1}) {
			StfrConfig.deuteriumHeliumYield = yield;
			StfrCoreSimulation core = new StfrCoreSimulation();
			core.ignite();
			core.injectPhysical(StfrCoreSimulation.Fuel.DEUTERIUM, 100);
			near(
					core.coreFuel() / StfrCoreSimulation.PLASMA_UNITS_PER_MB,
					100,
					"Real injection preserves mB");
			Storage output = new Storage();
			for (int i = 0; i < 400; i++) {
				core.eventTemperature(StfrCoreSimulation.CORE_TEMP_NOMINAL);
				core.tick(output, 4, 0, 90, 0, 0, 0, 0, false);
			}
			double reacted = 100 - core.coreFuel() / StfrCoreSimulation.PLASMA_UNITS_PER_MB;
			check(reacted > 0, "Physical fuel actually burns");
			double helium = core.coreHelium4() / StfrCoreSimulation.PLASMA_UNITS_PER_MB;
			near(helium, reacted * yield, "Configured yield measured in physical mB");
			NBTTagCompound state = core.write();
			state.setDouble("caseHelium4", core.coreHelium4());
			state.setDouble("coreHelium4", 0);
			state.setDouble("coreTemperature", 0);
			core.read(state);
			core.tick(output, 0, 0, 100, 0, 0, 0, 100, false);
			near(
					core.caseHelium4() / StfrCoreSimulation.PLASMA_UNITS_PER_MB
							+ core.write().getDouble("productRemainder"),
					helium,
					"Blocked output retains all helium");
			StfrCoreSimulation restored = new StfrCoreSimulation();
			restored.read(core.write());
			output.room = 1000;
			for (int i = 0; i < 200; i++) restored.tick(output, 0, 0, 100, 0, 0, 0, 100, false);
			near(
					output.helium
							+ restored.caseHelium4() / StfrCoreSimulation.PLASMA_UNITS_PER_MB
							+ restored.write().getDouble("productRemainder"),
					helium,
					"Output plus saved fractional remainder conserves matter");
			check(output.helium <= 100 * yield, "No inflated helium output");
		}
		StfrConfig.deuteriumHeliumYield = .9;
		NBTTagCompound legacy = new NBTTagCompound();
		legacy.setDouble("productRemainder", StfrCoreSimulation.PLASMA_UNITS_PER_MB * 2.5);
		StfrCoreSimulation migrated = new StfrCoreSimulation();
		migrated.read(legacy);
		near(
				migrated.write().getDouble("productRemainder"),
				2.5,
				"Legacy output units migrate once");
		migrated.read(migrated.write());
		near(
				migrated.write().getDouble("productRemainder"),
				2.5,
				"Repeated reload does not reconvert mB");
		System.out.println("STFR network, Mainframe power and physical matter checks passed");
	}

	private static long total(List<TileEntityLogicDevice> frames) {
		long n = 0;
		for (TileEntityLogicDevice frame : frames) n += frame.mainframeStoredEnergy();
		return n;
	}

	private static boolean powered(TileEntityLogicDevice frame) {
		return frame.writeToNBT(new NBTTagCompound()).getBoolean("mainframePowered");
	}
}

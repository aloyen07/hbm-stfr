package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.render.misc.EnumSymbol;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.logic.StfrSignalRepeater;
import fun.aloyenz.stfr.reactor.physics.StfrRotorPhysics;
import fun.aloyenz.stfr.reactor.states.StfrSectionState;
import fun.aloyenz.stfr.structure.StfrBlockRole;

/** Reference boundaries and saved-state regression checks without a Minecraft world. */
public final class StfrPhysicsAcceptanceTest {

	static void check(boolean condition, String message) {
		Assertions.assertTrue(condition, message);
	}

	static void near(double actual, double expected, String message) {
		check(Math.abs(actual - expected) < 1e-7, message + ": " + actual);
	}

	static final class Storage implements StfrThermalResources {

		int water, steam, helium;

		@Override
		public int waterAmount() {
			return water;
		}

		@Override
		public int steamRoom() {
			return 84_000_000 - steam;
		}

		@Override
		public int productRoom() {
			return 600_000 - helium;
		}

		@Override
		public void consumeWater(int n) {
			check(n <= water, "No water overdraft");
			water -= n;
		}

		@Override
		public int produceSteam(int n) {
			check(n <= steamRoom(), "No steam overflow");
			steam += n;
			return n;
		}

		@Override
		public int produceHelium(int n) {
			check(n <= productRoom(), "No product overflow");
			helium += n;
			return n;
		}
	}

	static final class PilotControls implements StfrAutopilot.Controls {

		final StfrCoreSimulation core = new StfrCoreSimulation();
		int fuel = 46, suppression = 95, offset = 60, lasers = 10, permeability = 15, vent = 15;
		boolean digamma = true;
		long tick;

		public StfrCoreSimulation core() {
			return core;
		}

		public boolean digammaActive() {
			return digamma;
		}

		public double autopilotTelemetry(double value, int salt) {
			return value * (1D + Math.sin((tick + salt * 37L) * .173D) * .0001D);
		}

		public int fuelInputLevel() {
			return fuel;
		}

		public int suppression() {
			return suppression;
		}

		public int offset() {
			return offset;
		}

		public int lasers() {
			return lasers;
		}

		public int vent() {
			return vent;
		}

		public int permeability() {
			return permeability;
		}

		public BlockPos center() {
			return BlockPos.ORIGIN;
		}

		public void autopilotAdjust(
				int fuel, int suppression, int offset, int lasers, int vent, int permeability) {
			this.fuel = bound(this.fuel + fuel, 0, 100);
			this.suppression = bound(this.suppression + suppression, 0, 100);
			this.offset = bound(this.offset + offset, -100, 100);
			this.lasers = bound(this.lasers + lasers, 0, 100);
			this.vent = bound(this.vent + vent, 0, 100);
			this.permeability = bound(this.permeability + permeability, 0, 100);
		}

		private static int bound(int value, int minimum, int maximum) {
			return Math.max(minimum, Math.min(maximum, value));
		}
	}

	@Test
	void acceptanceScenarios() {
		for (EnumFacing axis : EnumFacing.VALUES) {
			Vec3d a = new Vec3d(axis.getDirectionVec());
			Vec3d radial = Math.abs(a.y) > .5 ? new Vec3d(2, 0, 0) : new Vec3d(0, 2, 0);
			Vec3d push = StfrRotorPhysics.impulse(radial, axis, 100);
			near(push.dotProduct(a), 0, "Rotor kick is perpendicular to its axis");
			near(push.dotProduct(radial), 0, "Rotor kick follows the surface tangent");
			near(push.length(), .34, "Normal rotor gives a stronger player impulse");
			near(
					push.add(StfrRotorPhysics.impulse(radial.scale(-1), axis, 100)).length(),
					0,
					"Opposite rotor sides kick in opposite directions");
			near(
					StfrRotorPhysics.impulse(radial, axis, 200).length(),
					push.length() * 2,
					"Kick strength scales with rotor speed");
			near(
					StfrRotorPhysics.impulse(radial, axis, 0).length(),
					0,
					"Stopped rotors cannot kick");
			near(
					StfrRotorPhysics.impulse(a.scale(3), axis, 100).length(),
					0,
					"Rotation axis has no tangential velocity");
			check(
					StfrRotorPhysics.impulse(radial, axis, Integer.MAX_VALUE).length() <= 1.5000001,
					"Extreme overdrive kick stays bounded");
		}
		StfrComponentStatus.Builder partial = new StfrComponentStatus.Builder(EnumFacing.UP);
		partial.optionalTotal = 3;
		partial.defect(
				new BlockPos(14, 24, 14),
				StfrBlockRole.OPTIONAL_EFFICIENCY,
				"WRONG_BLOCK",
				"expected",
				"air");
		partial.defect(
				new BlockPos(15, 24, 14), StfrBlockRole.SEAL, "WRONG_BLOCK", "expected", "air");
		check(
				new StfrComponentStatus(partial).turbineStructureValid,
				"Partial efficiency loss can leave a turbine running");
		check(
				new StfrComponentStatus(partial).restorationLevel() == 0,
				"A running turbine with a missing optional block is not fully restored");
		StfrComponentStatus.Builder functional = new StfrComponentStatus.Builder(EnumFacing.UP);
		functional.defect(
				new BlockPos(15, 24, 14), StfrBlockRole.SEAL, "WRONG_BLOCK", "expected", "air");
		check(
				new StfrComponentStatus(functional).restorationLevel() == 1,
				"Seal damage permits the functional recovery level");
		StfrComponentStatus.Builder complete = new StfrComponentStatus.Builder(EnumFacing.UP);
		check(
				new StfrComponentStatus(complete).restorationLevel() == 2,
				"Restoring seals completes the second recovery level");
		complete.secondaryElectrodeMask = 7;
		check(
				new StfrComponentStatus(complete).restorationLevel() == 0,
				"All electrode chains must work before a recovery notice");
		StfrSignalRepeater repeats = new StfrSignalRepeater();
		for (int tick = 1; tick <= 12; tick++)
			check(
					repeats.tick(0, true, 3) == (tick % 3 == 0),
					"Held input repeats at exact N-tick intervals");
		check(!repeats.tick(1, true, 3), "Each face has its own timer");
		repeats.tick(0, true, 3);
		repeats.tick(0, false, 3);
		check(!repeats.tick(0, true, 3), "Releasing power resets the hold timer");
		for (int tick = 0; tick < 10; tick++)
			check(!repeats.tick(2, true, 0), "Repeat is disabled by default");
		check(repeats.tick(3, true, 1), "One-tick repeat is supported");
		for (int tick = 0; tick < 10; tick++)
			check(!repeats.tick(4, false, 1), "Button pulses are excluded from repeat eligibility");
		double kevPerTemperature = .00008617718028266115D;
		check(
				StfrSectionState.state(63, 1, 0).equals("REFUELING"),
				"Removing fuel from intact section requests refueling");
		check(
				StfrSectionState.damaged(63) == 0,
				"Refueling section must not announce structural defunction");
		check(
				StfrSectionState.state(62, 1, 0).equals("FAILED"),
				"A physically broken section still fails during refueling");
		check(
				StfrSectionState.state(63, 0, 0).equals("READY"),
				"Locked fuel restores ready section");
		check(
				StfrSectionState.enteredRefueling(63, 0, 8) == 8,
				"Ready section announces refueling entry");
		check(
				StfrSectionState.enteredRefueling(63, 8, 8) == 0,
				"Repeated inspection does not repeat refueling voice");
		check(
				StfrSectionState.enteredRefueling(0, 0, 63) == 0,
				"Discovering an empty structure does not announce six refueling events");
		near(
				StfrCoreSimulation.reactivity(2715D / kevPerTemperature),
				.2D,
				"Original sine is in degrees, peak is 90 degrees");
		near(StfrCoreSimulation.reactivity(15D / kevPerTemperature), 0D, "Reaction threshold");
		near(StfrCoreSimulation.reactivity(0), 0D, "Cold core cannot react");
		near(
				StfrCoreSimulation.reactivity(5415D / kevPerTemperature),
				0D,
				"Descending sine reaches zero");
		check(
				StfrCoreSimulation.reactivity(1000D / kevPerTemperature) > 0,
				"Positive lower-temperature branch");
		StfrCoreSimulation core = new StfrCoreSimulation();
		core.inject(StfrCoreSimulation.Fuel.DEUTERIUM, 100);
		core.inject(StfrCoreSimulation.Fuel.TRITIUM, 25);
		core.startupLaserClick();
		StfrCoreSimulation restored = new StfrCoreSimulation();
		restored.read(core.write());
		near(
				restored.amount(StfrCoreSimulation.Fuel.DEUTERIUM, false),
				100,
				"D mixture survives save");
		near(
				restored.amount(StfrCoreSimulation.Fuel.TRITIUM, false),
				25,
				"T mixture survives save");
		near(restored.coreFuel(), 125, "Mixture totals agree");
		near(
				restored.coreTemperature(),
				core.coreTemperature(),
				"Reload during laser heating must not erase temperature");
		NBTTagCompound legacy = new NBTTagCompound();
		legacy.setString("fuel", "DEUTERIUM");
		legacy.setDouble("coreFuel", 12);
		legacy.setDouble("caseFuel", 40);
		legacy.setDouble("caseHelium4", 20);
		legacy.setDouble("caseTemperature", 1200);
		restored.read(legacy);
		near(
				restored.amount(StfrCoreSimulation.Fuel.DEUTERIUM, false),
				12,
				"Old single-fuel save migrates");
		restored.leakCase();
		near(restored.caseFuel(), 38, "Unsealed case loses five percent");
		near(restored.caseHelium4(), 19, "Helium leaks too");
		restored.coolOffline();
		near(restored.caseTemperature(), 1199.0686, "Original offline cooling");
		legacy.setDouble("caseTemperature", -50);
		restored.read(legacy);
		near(restored.caseTemperature(), -50, "Cold case survives reload");
		legacy.setDouble("coreFuel", Double.NaN);
		restored.read(legacy);
		near(restored.coreFuel(), 0, "Invalid saved amounts do not poison physics");
		StfrMaintenance.Defect defect = new StfrMaintenance.Defect(BlockPos.ORIGIN, 0);
		Map<BlockPos, StfrBlockRole> serviceRoles = new HashMap<>();
		serviceRoles.put(BlockPos.ORIGIN, StfrBlockRole.EXACT);
		for (EnumFacing face : EnumFacing.VALUES)
			serviceRoles.put(BlockPos.ORIGIN.offset(face), StfrBlockRole.EXACT);
		Function<BlockPos, StfrBlockRole> lookup =
				pos -> serviceRoles.getOrDefault(pos, StfrBlockRole.UNASSIGNED);
		check(
				!StfrMaintenance.serviceable(BlockPos.ORIGIN, lookup),
				"Fully enclosed reactor block cannot request maintenance");
		for (EnumFacing face : EnumFacing.VALUES) {
			serviceRoles.put(BlockPos.ORIGIN.offset(face), StfrBlockRole.UNASSIGNED);
			check(
					StfrMaintenance.serviceable(BlockPos.ORIGIN, lookup),
					"Each of six unsigned neighbors exposes a serviceable face");
			serviceRoles.put(BlockPos.ORIGIN.offset(face), StfrBlockRole.EXACT);
		}
		serviceRoles.put(BlockPos.ORIGIN, StfrBlockRole.UNASSIGNED);
		check(
				!StfrMaintenance.serviceable(BlockPos.ORIGIN, lookup),
				"Unassigned world blocks do not become reactor maintenance targets");
		for (int tick = 1; tick <= 6000; tick++) {
			defect.age++;
			check(defect.overdue() == (tick == 6000), "Deadline is exactly 6000 loaded ticks");
		}
		for (int click = 1; click <= 5; click++)
			check(defect.repair() == (click == 5), "Exactly five wrench uses repair a defect");
		near(StfrMaintenance.probability(false), .0001, "Original normal maintenance probability");
		near(StfrMaintenance.probability(true), .002, "Original hardcore probability");
		check(
				StfrMaintenance.candidates(BlockPos.ORIGIN, 0).size() == 120,
				"15 case positions in eight octants");
		for (int c = 1; c <= 6; c++)
			check(
					StfrMaintenance.candidates(BlockPos.ORIGIN, c).size() == 49,
					"49 candidates per stabilizer");
		StfrMaintenance maintenance = new StfrMaintenance(null);
		NBTTagCompound saved = new NBTTagCompound(), item = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		item.setLong("pos", new BlockPos(10, 20, 30).toLong());
		item.setInteger("component", 3);
		item.setInteger("age", 5999);
		item.setInteger("repairs", 4);
		item.setInteger("particle", 7);
		list.appendTag(item);
		saved.setTag("defects", list);
		maintenance.read(saved);
		check(
				maintenance.count() == 1 && maintenance.count(3) == 1,
				"Pending maintenance survives reload");
		NBTTagCompound roundTrip =
				maintenance.write().getTagList("defects", 10).getCompoundTagAt(0);
		check(
				roundTrip.getInteger("age") == 5999
						&& roundTrip.getInteger("repairs") == 4
						&& roundTrip.getInteger("particle") == 7,
				"Repair progress, particle and deadline preserved");
		// Minimal real HBM fluid traits; a world and mod initialization are not needed.
		Fluids.WATER = new FluidType("STFR_TEST_WATER", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.STEAM = new FluidType("STFR_TEST_STEAM", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.WATER.addTraits(new FT_Heatable().addStep(200, 1, Fluids.STEAM, 100));
		NBTTagCompound initial = new NBTTagCompound();
		initial.setBoolean("exists", true);
		initial.setDouble("caseTemperature", 1000);
		initial.setDouble("capacitor", 1_000_000);
		initial.setDouble("shield", 100);
		Storage resources = new Storage();
		resources.water = 10_000;
		core.read(initial);
		core.tick(resources, 6, 0, 100, 0, 0, 0, 0);
		int waterUsed = 10_000 - resources.water;
		check(waterUsed > 0, "Hot case boils water");
		check(resources.steam == waterUsed * 100, "Use HBM recipe, not hard-coded TERF expansion");
		check(
				core.caseTemperature() < 1000 && core.coolingRate() > 0,
				"Delivered coolant removes heat");
		resources = new Storage();
		resources.water = 100;
		core.read(initial);
		core.tick(resources, 6, 0, 100, 0, 0, 0, 0);
		near(core.caseTemperature(), 1000, "Insufficient coolant cannot provide full cooling");
		check(
				resources.water == 100 && resources.steam == 0,
				"No partial operation consumes coolant without cooling");
		resources = new Storage();
		resources.water = 10_000;
		resources.steam = 84_000_000;
		core.read(initial);
		core.tick(resources, 6, 0, 100, 0, 0, 0, 0);
		near(core.caseTemperature(), 1000, "Blocked output stops boiling and heat removal");
		check(
				core.steamBlocked() && resources.water == 10_000,
				"Full steam tank cannot eat coolant");
		initial.setDouble("caseTemperature", 20);
		resources = new Storage();
		resources.water = 10_000;
		core.read(initial);
		core.tick(resources, 6, 0, 100, 0, 0, 0, 0);
		check(
				resources.water == 10_000 && resources.steam == 0,
				"Cold case does not consume cooling water");
		initial.setDouble("caseTemperature", 1000);
		initial.setBoolean("exists", false);
		core.read(initial);
		core.tick(resources, 6, 0, 100, 0, 0, 0, 0);
		check(
				resources.water == 10_000 && resources.steam == 0,
				"Inactive core does not consume cooling water");
		initial.setBoolean("exists", true);
		check(
				StfrElectrodeGeneration.power(5000, 1, 1000, 10_000_000) == 5_000_000,
				"Electrodes collect reaction power without consuming steam");
		check(
				StfrElectrodeGeneration.power(5000, .5, 1000, 10_000_000) == 2_500_000,
				"Electrical output follows independent electrode coverage");
		check(
				StfrElectrodeGeneration.power(5000, 0, 1000, 10_000_000) == 0,
				"Broken electrodes cannot collect HE");
		check(
				StfrElectrodeGeneration.power(15000, 1, 1000, 10_000_000) == 10_000_000,
				"Electrical generation obeys reactor limit");
		core.simulateOnline();
		check(
				core.exists() && core.coreFuel() > 0 && core.shield() == 100,
				"Online simulation creates a working shielded plasma");
		near(
				core.coreTemperature(),
				StfrCoreSimulation.CORE_TEMP_NOMINAL,
				"Online simulation uses nominal temperature");
		initial.setString("fuel", "DEUTERIUM");
		initial.setDouble("coreFuel", 1_000_000);
		initial.setDouble("coreTemperature", StfrCoreSimulation.CORE_TEMP_NOMINAL);
		initial.setDouble("caseTemperature", 20);
		resources = new Storage();
		core.read(initial);
		core.tick(resources, 4, 0, 90, 0, 0, 0, 0);
		double consumed = 1_000_000 - core.coreFuel();
		check(consumed > 0, "Peak-temperature core reacts");
		near(
				core.coreHelium4(),
				consumed * StfrConfig.deuteriumHeliumYield,
				"Fusion product follows configured material yield");
		near(core.reactionRate(), consumed, "Reported reaction agrees with consumed composition");
		check(core.reactionPower() > 0, "Fusion produces heat");
		initial.setDouble("caseTemperature", 1000);
		resources = new Storage();
		resources.water = 10_000;
		core.read(initial);
		core.tick(resources, 6, 0, 90, 0, 0, 0, 0);
		int retainedSteam = resources.steam, remainingWater = resources.water;
		check(
				retainedSteam > 0 && retainedSteam == (10_000 - remainingWater) * 100,
				"Active fusion produces real steam for external turbine cooling loop");
		check(
				StfrElectrodeGeneration.power(core.reactionPower(), 1, 1000, 10_000_000) > 0,
				"Direct HE and cooling steam are produced together");
		check(
				resources.steam == retainedSteam && resources.water == remainingWater,
				"Electrode collection leaves water and steam untouched");

	}

}

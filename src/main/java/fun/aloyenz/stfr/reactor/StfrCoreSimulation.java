package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.StfrConfig;

/** TERF core/case calculation. Physical tanks use mB; plasma uses scaled composition amounts. */
public final class StfrCoreSimulation {

	public enum Fuel {
		NONE,
		DEUTERIUM,
		TRITIUM,
		HELIUM3
	}

	/** Converts physical tank mB into the existing TERF plasma density scale. */
	public static final double PLASMA_UNITS_PER_MB = 100_000D / 24D;

	static final double TERF_UNITS_PER_MB = 10D;
	static final double SHC_PER_MOLE = 27.15129792D;
	static final double MAX_CAPACITOR = 1_000_000D;

	/** TERF's deuterium peak: 2715 keV, core scoreboard temperature in kilokelvins. */
	public static final double CORE_TEMP_NOMINAL = 2715D / .00008617718028266115D;

	public static final double CORE_TEMP_SAFE_MIN = CORE_TEMP_NOMINAL;
	public static final double CORE_TEMP_SAFE_MAX = CORE_TEMP_NOMINAL * 1.5D;

	Fuel fuel = Fuel.NONE;
	final double[] coreAmounts = new double[Fuel.values().length],
			caseAmounts = new double[Fuel.values().length];
	double coreFuel, coreHelium4, caseFuel, caseHelium4;
	double coreTemperature, caseTemperature = 20D;
	double reactionPower, reactionRate, heatTransferRate, coolingRate, radiation;
	double steamRemainder, productRemainder, capacitor = MAX_CAPACITOR;
	double shield = 100D, rotation, pressure, casePressure;
	double mssr, pressureStress, spinStress, rigidityStress, shieldStress;
	int steamProduced;
	boolean steamBlocked, coreExists;
	double reactionFactor = 1D, pressureBonus;
	double effectiveReactivity;
	boolean shieldOverdrive;
	boolean flowInitialized;
	double effectivePermeability, effectiveVent;

	public Fuel fuel() { return fuel; }

	public double coreFuel() { return coreFuel; }

	public double coreHelium4() { return coreHelium4; }

	public double caseHelium4() { return caseHelium4; }

	public double caseFuel() { return caseFuel; }

	public double amount(Fuel substance, boolean inCase) {
		return (inCase ? caseAmounts : coreAmounts)[substance.ordinal()];
	}

	public String fuelLabel() {
		String label = "NONE";
		int count = 0;
		for (Fuel substance : Fuel.values())
			if (substance != Fuel.NONE && coreAmounts[substance.ordinal()] > 0) {
				label = substance.name();
				count++;
			}
		return count > 1 ? "MIXED" : label;
	}

	public static double reactivity(double temperature) {
		double kev = temperature * .00008617718028266115D;
		return Math.max(
				0D, Math.sin(Math.toRadians(Math.max(0D, Math.min(10000D, kev) / 30D - .5D))) / 5D);
	}

	public void leakCase() {
		for (int i = 1; i < caseAmounts.length; i++)
			caseAmounts[i] -= Math.floor(caseAmounts[i] * 10D / 20D) / 10D;
		caseHelium4 -= Math.floor(caseHelium4 * 10D / 20D) / 10D;
		totals();
	}

	public void coolOffline() {
		if (caseTemperature >= 22D) caseTemperature = Math.max(20D, caseTemperature - .9314D);
	}

	public void coolCase(double degrees) {
		caseTemperature = Math.max(-273.15D, caseTemperature - Math.max(0, degrees));
	}

	public boolean spendCapacitor(double amount) {
		if (amount < 0 || capacitor < amount) return false;
		capacitor -= amount;
		return true;
	}

	void totals() {
		coreFuel = caseFuel = 0;
		for (int i = 1; i < coreAmounts.length; i++) {
			coreFuel += coreAmounts[i];
			caseFuel += caseAmounts[i];
		}
	}

	public double coreTemperature() { return coreTemperature; }

	public double caseTemperature() { return caseTemperature; }

	public double reactionPower() { return reactionPower; }

	public double reactionRate() { return reactionRate; }

	public double heatTransferRate() { return heatTransferRate; }

	public double coolingRate() { return coolingRate; }

	public double radiation() { return radiation; }

	public double capacitor() {
		return capacitor / MAX_CAPACITOR * 100D;
	}

	public double shield() { return shield; }

	public double rotation() { return rotation; }

	public double pressure() { return pressure; }

	/** Plasma density without the destabilizer's externally supplied compression field. */
	public double intrinsicPressure() { return coreDensity(); }

	public double casePressure() { return casePressure; }

	public double mssr() { return mssr; }

	public double pressureStress() { return pressureStress; }

	public double spinStress() { return spinStress; }

	public double rigidityStress() { return rigidityStress; }

	public double shieldStress() { return shieldStress; }

	public double effectiveReactivity() { return effectiveReactivity; }

	public double reactionFactor() { return reactionFactor; }

	public boolean steamBlocked() { return steamBlocked; }

	public int steamProduced() { return steamProduced; }

	public boolean exists() { return coreExists; }

	public void eventShield(double intensity) {
		shield = Math.max(0, Math.min(100, intensity));
	}

	public void loseShieldFraction(double fraction) { eventShield(shield * (1D - Math.max(0D, Math.min(1D, fraction)))); }

	public void setShieldOverdrive(boolean active) {
		shieldOverdrive = active;
	}

	public void setDigammaPhysics(boolean active, double poisonFactor) {
		reactionFactor = active ? 2D : 1D;
		reactionFactor *= Math.max(0D, Math.min(1D, poisonFactor));
	}

	public void setCompressionPressure(double target, double amount) {
		double base = coreDensity();
		double nextBonus = Math.max(0D, (target - base) * Math.max(0D, Math.min(1D, amount)));
		// The driven shield does real compression work on the plasma.  Heating
		// is tied to the increase in confinement pressure rather than being a
		// scripted temperature correction toward the resonance value.
		if (coreExists && nextBonus > pressureBonus)
			coreTemperature = Math.min(1E15, coreTemperature + (nextBonus - pressureBonus) * 24D);
		pressureBonus = nextBonus;
	}

	/** Converts sustained shield-overdrive work into plasma heat while seeking resonance. */
	public void applyCompressionHeating(double targetTemperature, double maximumPerTick) {
		if (!coreExists || !shieldOverdrive || coreTemperature >= targetTemperature) return;
		double error = targetTemperature - coreTemperature;
		double taper = Math.max(.08D, Math.min(1D, error / 2_000_000D));
		coreTemperature =
				Math.min(targetTemperature, coreTemperature + Math.max(0D, maximumPerTick) * taper);
	}

	public void eventHeat(double coreDegrees, double caseDegrees) {
		coreTemperature = Math.max(0, Math.min(1E15, coreTemperature + coreDegrees));
		caseTemperature = Math.max(-273.15, Math.min(1E12, caseTemperature + caseDegrees));
	}

	public void overloadSpin() {
		if (rotation != 0) rotation += rotation < 0 ? -151756756 : 151756756;
	}

	public void eventBrake(double amount) {
		rotation = Math.copySign(Math.max(0, Math.abs(rotation) - amount), rotation);
	}

	public void purge(double fraction) {
		fraction = Math.max(0, Math.min(1, fraction));
		for (int i = 1; i < coreAmounts.length; i++) coreAmounts[i] *= 1 - fraction;
		coreHelium4 *= 1 - fraction;
		totals();
	}

	public void injectPhysical(Fuel substance, int millibuckets) {
		if (millibuckets > 0) inject(substance, millibuckets * PLASMA_UNITS_PER_MB);
	}

	public static double heliumYield(Fuel substance) {
		switch (substance) {
			case DEUTERIUM:
				return StfrConfig.deuteriumHeliumYield;
			case TRITIUM:
				return StfrConfig.tritiumHeliumYield;
			case HELIUM3:
				return StfrConfig.helium3HeliumYield;
			default:
				return 0;
		}
	}

	public void eventTemperature(double temperature) {
		coreTemperature = Math.max(0, temperature);
	}

	public void ignite() {
		coreExists = true;
	}

	public void simulateOnline() {
		deactivate();
		inject(Fuel.DEUTERIUM, 150_000D);
		coreTemperature = CORE_TEMP_NOMINAL;
		caseTemperature = 300D;
		rotation = 120_000_000D;
		mssr = 48D;
		restabilize();
	}

	public void simulationPreset() {
		simulateOnline();
		rotation = StfrConfig.simulatedCoreRotation;
		mssr = StfrConfig.simulatedMssr;
		coreTemperature = StfrConfig.simulatedCoreTemperature;
	}

	public void restabilize() {
		coreExists = true;
		shield = 100;
		capacitor = MAX_CAPACITOR;
		shieldStress = pressureStress = spinStress = rigidityStress = 0;
	}

	public void failureHeat() {
		caseTemperature = 1015D;
	}

	public void beginStartup() {
		coreExists = false;
		coreTemperature = 0;
		pressure = 0;
		shield = 0;
	}

	public void startupShieldTick() {
		shield = Math.min(100D, shield + 1D);
	}

	public void deactivate() {
		fuel = Fuel.NONE;
		coreFuel = coreHelium4 = caseFuel = caseHelium4 = 0;
		Arrays.fill(coreAmounts, 0);
		Arrays.fill(caseAmounts, 0);
		forceOffline();
	}

	public void startupLaserClick() {
		coreTemperature += 1_111_232D;
	}

	public void forceOffline() {
		coreExists = false;
		coreTemperature = pressure = rotation = 0;
		reactionPower = reactionRate = heatTransferRate = coolingRate = radiation = 0;
		steamProduced = 0;
		mssr = pressureStress = spinStress = rigidityStress = shieldStress = 0;
		steamBlocked = false;
	}

	public void discardUnignited() {
		if (coreExists) return;
		fuel = Fuel.NONE;
		coreFuel = coreHelium4 = caseFuel = caseHelium4 = 0;
		Arrays.fill(coreAmounts, 0);
		Arrays.fill(caseAmounts, 0);
		forceOffline();
	}

	public boolean accepts(Fuel incoming) {
		return incoming != Fuel.NONE;
	}

	public void inject(Fuel incoming, double amount) {
		if (!Double.isFinite(amount) || amount <= 0 || !accepts(incoming)) return;
		if (fuel == Fuel.NONE) fuel = incoming;
		coreAmounts[incoming.ordinal()] += amount;
		totals();
	}

	public void tick(
			StfrThermalResources resources,
			int workingStabilizers,
			int injection,
			int suppression,
			int offset,
			int permeability,
			int lasers,
			int vent) {
		tick(
				resources,
				workingStabilizers,
				injection,
				suppression,
				offset,
				permeability,
				lasers,
				vent,
				true);
	}

	public void tick(
			StfrThermalResources resources,
			int workingStabilizers,
			int injection,
			int suppression,
			int offset,
			int permeability,
			int lasers,
			int vent,
			boolean produceSteam) {
		tick(
				resources,
				workingStabilizers,
				injection,
				suppression,
				offset,
				permeability,
				lasers,
				vent,
				produceSteam,
				workingStabilizers > 0 ? 1D : 0D);
	}

	public void tick(
			StfrThermalResources resources,
			int workingStabilizers,
			int injection,
			int suppression,
			int offset,
			int permeability,
			int lasers,
			int vent,
			boolean produceSteam,
			double electrodeCoverage) {
		tick(
				resources,
				workingStabilizers,
				injection,
				suppression,
				offset,
				permeability,
				lasers,
				vent,
				produceSteam,
				electrodeCoverage,
				false);
	}

	public void tick(
			StfrThermalResources resources,
			int workingStabilizers,
			int injection,
			int suppression,
			int offset,
			int permeability,
			int lasers,
			int vent,
			boolean produceSteam,
			double electrodeCoverage,
			boolean leakingCase) {
		StfrCorePhysics.tick(this, resources, workingStabilizers, injection, suppression, offset, permeability, lasers, vent, produceSteam, electrodeCoverage, leakingCase);
	}

	// Physical amounts are conserved in the fuel mB scale. TERF's original
	// thermal/density balance used 0.25 ash units per burned fuel unit; changing
	// the physical yield to 0.9 must not multiply the plasma load by 3.6.
	// Apply the balance factor only to density/heat capacity, never transfers
	// or output. Retained helium still accumulates and can overload the core.
	static double heliumDensityFactor() { return StfrConfig.heliumPlasmaDensityFactor; }

	double coreDensity() {
		return (coreFuel + coreHelium4 * heliumDensityFactor()) * TERF_UNITS_PER_MB;
	}

	double caseDensity() {
		return (caseFuel + caseHelium4 * heliumDensityFactor()) * TERF_UNITS_PER_MB;
	}

	static double flow(double amount, double setting, double divider) {
		return amount * Math.max(0D, Math.min(100D, setting)) / divider;
	}

	/** Uses HBM's own WATER heating recipe, so addon updates cannot desync the coolant loop. */
	static FT_Heatable.HeatingStep waterToSteamStep() {
		if (!Fluids.WATER.hasTrait(FT_Heatable.class)) return null;
		FT_Heatable.HeatingStep step = Fluids.WATER.getTrait(FT_Heatable.class).getFirstStep();
		if (step == null
				|| step.typeProduced != Fluids.STEAM
				|| step.amountReq <= 0
				|| step.amountProduced <= 0) return null;
		return step;
	}

	public NBTTagCompound write() { return StfrCorePersistence.write(this); }

	public void read(NBTTagCompound tag) { StfrCorePersistence.read(this, tag); }

	static double finite(double value, double fallback) {
		return Double.isFinite(value) ? value : fallback;
	}

	static double positive(NBTTagCompound tag, String key) { return Math.max(0D, finite(tag.getDouble(key), 0D)); }
}

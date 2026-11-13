package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.StfrConfig;

/** Persistent reactor-wide storage. Ports are access points, never independent tanks. */
public final class StfrResourceNetwork implements StfrThermalResources {

	public static final long STARTUP_ENERGY_CAPACITY = 1_000_000L;
	public static final long ONLINE_ENERGY_CAPACITY = 1_000_000_000L;
	private final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 3_600_000);
	private final FluidTankNTM lubricant = new FluidTankNTM(Fluids.LUBRICANT, 6_000);
	private final FluidTankNTM steam = new FluidTankNTM(Fluids.STEAM, 84_000_000);
	private final FluidTankNTM helium4 = new FluidTankNTM(Fluids.HELIUM4, 600_000);
	private FluidTankNTM yaronite;
	private long energy;
	private long energyCapacity = STARTUP_ENERGY_CAPACITY;
	private transient long outputBudget;

	public FluidTankNTM water() {
		return water;
	}

	public FluidTankNTM lubricant() {
		return lubricant;
	}

	public FluidTankNTM steam() {
		return steam;
	}

	public FluidTankNTM helium4() {
		return helium4;
	}

	public FluidTankNTM yaronite() {
		if (yaronite == null) yaronite = new FluidTankNTM(StfrFluids.YARONITE, 3072);
		return yaronite;
	}

	@Override
	public int waterAmount() {
		return water.getFill();
	}

	@Override
	public int steamRoom() {
		return steam.getMaxFill() - steam.getFill();
	}

	@Override
	public int productRoom() {
		return helium4.getMaxFill() - helium4.getFill();
	}

	@Override
	public void consumeWater(int amount) {
		take(water, amount);
	}

	@Override
	public int produceSteam(int amount) {
		return add(steam, amount);
	}

	@Override
	public int produceHelium(int amount) {
		return add(helium4, amount);
	}

	public long energy() {
		return energy;
	}

	public long energyCapacity() {
		return energyCapacity;
	}

	public long outputBudget() {
		return outputBudget;
	}

	public void setFailure(boolean failure) {
		if (!failure) return;
		energyCapacity = Math.max(ONLINE_ENERGY_CAPACITY, StfrConfig.recoveryCharge);
		energy = Math.min(energy, energyCapacity);
		outputBudget = 0;
	}

	public void setOnline(boolean online) {
		energyCapacity = online ? ONLINE_ENERGY_CAPACITY : STARTUP_ENERGY_CAPACITY;
		if (!online) outputBudget = 0;
		if (energy > energyCapacity) energy = energyCapacity;
	}

	public void setEnergy(long amount) {
		energy = Math.max(0, Math.min(energyCapacity, amount));
	}

	public long addEnergy(long amount) {
		long accepted = Math.min(Math.max(0, amount), energyCapacity - energy);
		energy += accepted;
		return accepted;
	}

	public void beginOutputTick(long maximum) {
		outputBudget = Math.min(energy, Math.max(0, maximum));
	}

	public void useOutput(long amount) {
		long used = Math.min(Math.max(0, amount), Math.min(energy, outputBudget));
		energy -= used;
		outputBudget -= used;
	}

	public long takeEnergy(long requested) {
		long taken = Math.min(Math.max(0, requested), energy);
		energy -= taken;
		return taken;
	}

	public int add(FluidTankNTM tank, int amount) {
		int accepted = Math.min(Math.max(0, amount), tank.getMaxFill() - tank.getFill());
		if (accepted > 0) tank.setFill(tank.getFill() + accepted);
		return accepted;
	}

	public int take(FluidTankNTM tank, int amount) {
		int taken = Math.min(Math.max(0, amount), tank.getFill());
		if (taken > 0) tank.setFill(tank.getFill() - taken);
		return taken;
	}

	public NBTTagCompound write(NBTTagCompound tag) {
		water.writeToNBT(tag, "water");
		lubricant.writeToNBT(tag, "lubricant");
		steam.writeToNBT(tag, "steam");
		helium4.writeToNBT(tag, "helium4");
		yaronite().writeToNBT(tag, "yaronite");
		tag.setLong("energy", energy);
		return tag;
	}

	public void read(NBTTagCompound tag) {
		water.readFromNBT(tag, "water");
		lubricant.readFromNBT(tag, "lubricant");
		steam.readFromNBT(tag, "steam");
		helium4.readFromNBT(tag, "helium4");
		yaronite().readFromNBT(tag, "yaronite");
		water.setTankType(Fluids.WATER);
		lubricant.setTankType(Fluids.LUBRICANT);
		steam.setTankType(Fluids.STEAM);
		helium4.setTankType(Fluids.HELIUM4);
		yaronite().setTankType(StfrFluids.YARONITE);
		// Run state, which selects the actual capacity, is restored by StfrReactor
		// immediately after this method. Preserve an ONLINE-sized saved value here.
		energy = Math.max(0, tag.getLong("energy"));
	}
}

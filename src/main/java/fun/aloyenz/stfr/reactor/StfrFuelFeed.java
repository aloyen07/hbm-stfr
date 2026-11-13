package fun.aloyenz.stfr.reactor;

import java.util.function.IntSupplier;
import net.minecraft.nbt.NBTTagCompound;
import fun.aloyenz.stfr.reactor.StfrCoreSimulation.Fuel;

/** One physical mB reserved from a capsule, metered into the core each tick. */
public final class StfrFuelFeed {

	private Fuel fuel = Fuel.NONE;
	private double stored;

	public double storedMb() {
		return stored;
	}

	public Fuel fuel() {
		return fuel;
	}

	public void tick(StfrCoreSimulation core, double demand, Fuel incoming, IntSupplier drainOne) {
		if (!Double.isFinite(demand) || demand <= 0) return;
		double delivered = release(core, demand);
		double remaining = demand - delivered;
		if (remaining > 1E-12 && incoming != Fuel.NONE && drainOne.getAsInt() == 1) {
			fuel = incoming;
			stored = 1D;
			release(core, remaining);
		}
	}

	private double release(StfrCoreSimulation core, double demand) {
		double amount = Math.min(demand, stored);
		if (amount <= 0) return 0;
		core.inject(fuel, amount * StfrCoreSimulation.PLASMA_UNITS_PER_MB);
		stored -= amount;
		if (stored == 0) fuel = Fuel.NONE;
		return amount;
	}

	public NBTTagCompound write() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("fuel", fuel.name());
		tag.setDouble("storedMb", stored);
		return tag;
	}

	public void read(NBTTagCompound tag) {
		try {
			fuel = Fuel.valueOf(tag.getString("fuel"));
		} catch (IllegalArgumentException e) {
			fuel = Fuel.NONE;
		}
		double amount = tag.getDouble("storedMb");
		stored =
				fuel != Fuel.NONE && Double.isFinite(amount) ? Math.max(0, Math.min(1, amount)) : 0;
		if (stored == 0) fuel = Fuel.NONE;
	}
}

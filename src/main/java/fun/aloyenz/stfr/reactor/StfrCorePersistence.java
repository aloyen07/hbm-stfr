package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrCoreSimulation.*;

import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;

/** Core mixture, thermal state and legacy-save conversion. */
final class StfrCorePersistence {
	private StfrCorePersistence() {}

	static NBTTagCompound write(StfrCoreSimulation c) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("fuel", c.fuel.name());
		tag.setDouble("coreFuel", c.coreFuel);
		tag.setDouble("coreHelium4", c.coreHelium4);
		tag.setDouble("caseFuel", c.caseFuel);
		tag.setDouble("caseHelium4", c.caseHelium4);
		tag.setDouble("coreTemperature", c.coreTemperature);
		tag.setDouble("caseTemperature", c.caseTemperature);
		tag.setDouble("reactionPower", c.reactionPower);
		tag.setDouble("reactionRate", c.reactionRate);
		tag.setDouble("heatTransferRate", c.heatTransferRate);
		tag.setDouble("coolingRate", c.coolingRate);
		tag.setDouble("radiation", c.radiation);
		tag.setDouble("steamRemainder", c.steamRemainder);
		tag.setDouble("productRemainder", c.productRemainder);
		tag.setInteger("matterFormat", 2);
		tag.setDouble("capacitor", c.capacitor);
		tag.setDouble("shield", c.shield);
		tag.setDouble("rotation", c.rotation);
		tag.setDouble("pressure", c.pressure);
		tag.setDouble("casePressure", c.casePressure);
		tag.setDouble("mssr", c.mssr);
		tag.setDouble("pressureStress", c.pressureStress);
		tag.setDouble("spinStress", c.spinStress);
		tag.setDouble("rigidityStress", c.rigidityStress);
		tag.setDouble("shieldStress", c.shieldStress);
		tag.setBoolean("flowInitialized", c.flowInitialized);
		tag.setDouble("effectivePermeability", c.effectivePermeability);
		tag.setDouble("effectiveVent", c.effectiveVent);
		tag.setBoolean("steamBlocked", c.steamBlocked);
		tag.setBoolean("exists", c.coreExists);
		NBTTagCompound mixture = new NBTTagCompound();
		for (Fuel substance : Fuel.values())
			if (substance != Fuel.NONE) {
				mixture.setDouble("core_" + substance.name(), c.coreAmounts[substance.ordinal()]);
				mixture.setDouble("case_" + substance.name(), c.caseAmounts[substance.ordinal()]);
			}
		tag.setTag("mixture", mixture);
		return tag;
	}

	static void read(StfrCoreSimulation c, NBTTagCompound tag) {
		try {
			c.fuel = Fuel.valueOf(tag.getString("fuel"));
		} catch (IllegalArgumentException ex) {
			c.fuel = Fuel.NONE;
		}
		c.coreFuel = StfrCoreSimulation.positive(tag, "coreFuel");
		c.coreHelium4 = StfrCoreSimulation.positive(tag, "coreHelium4");
		c.caseFuel = StfrCoreSimulation.positive(tag, "caseFuel");
		c.caseHelium4 = StfrCoreSimulation.positive(tag, "caseHelium4");
		c.coreTemperature = StfrCoreSimulation.positive(tag, "coreTemperature");
		Arrays.fill(c.coreAmounts, 0);
		Arrays.fill(c.caseAmounts, 0);
		if (tag.hasKey("mixture", 10)) {
			NBTTagCompound mixture = tag.getCompoundTag("mixture");
			for (Fuel substance : Fuel.values())
				if (substance != Fuel.NONE) {
					c.coreAmounts[substance.ordinal()] =
							StfrCoreSimulation.positive(mixture, "core_" + substance.name());
					c.caseAmounts[substance.ordinal()] =
							StfrCoreSimulation.positive(mixture, "case_" + substance.name());
				}
		} else if (c.fuel != Fuel.NONE) {
			c.coreAmounts[c.fuel.ordinal()] = c.coreFuel;
			c.caseAmounts[c.fuel.ordinal()] = c.caseFuel;
		}
		c.totals();
		c.caseTemperature =
				tag.hasKey("caseTemperature")
						? Math.max(-273.15D, StfrCoreSimulation.finite(tag.getDouble("caseTemperature"), 20D))
						: 20D;
		c.reactionPower = StfrCoreSimulation.positive(tag, "reactionPower");
		c.reactionRate = StfrCoreSimulation.positive(tag, "reactionRate");
		c.heatTransferRate = StfrCoreSimulation.positive(tag, "heatTransferRate");
		c.coolingRate = StfrCoreSimulation.positive(tag, "coolingRate");
		c.radiation = StfrCoreSimulation.positive(tag, "radiation");
		c.steamRemainder = StfrCoreSimulation.positive(tag, "steamRemainder");
		c.productRemainder = StfrCoreSimulation.positive(tag, "productRemainder");
		if (tag.getInteger("matterFormat") < 2) c.productRemainder /= PLASMA_UNITS_PER_MB;
		c.capacitor =
				tag.hasKey("capacitor")
						? Math.max(
								0D, Math.min(MAX_CAPACITOR, StfrCoreSimulation.finite(tag.getDouble("capacitor"), 0D)))
						: MAX_CAPACITOR;
		c.shield = Math.max(0D, Math.min(100D, StfrCoreSimulation.finite(tag.getDouble("shield"), 0D)));
		c.rotation = StfrCoreSimulation.finite(tag.getDouble("rotation"), 0D);
		c.pressure = StfrCoreSimulation.positive(tag, "pressure");
		c.casePressure = StfrCoreSimulation.positive(tag, "casePressure");
		c.mssr = StfrCoreSimulation.positive(tag, "mssr");
		c.pressureStress = StfrCoreSimulation.positive(tag, "pressureStress");
		c.spinStress = StfrCoreSimulation.positive(tag, "spinStress");
		c.rigidityStress = StfrCoreSimulation.positive(tag, "rigidityStress");
		c.shieldStress = StfrCoreSimulation.positive(tag, "shieldStress");
		c.steamBlocked = tag.getBoolean("steamBlocked");
		c.coreExists = tag.getBoolean("exists");
		c.flowInitialized = tag.getBoolean("flowInitialized");
		c.effectivePermeability = Math.min(100, StfrCoreSimulation.positive(tag, "effectivePermeability"));
		c.effectiveVent = Math.min(100, StfrCoreSimulation.positive(tag, "effectiveVent"));
		// Preserve unignited startup contents and accumulated laser heating.
	}
}

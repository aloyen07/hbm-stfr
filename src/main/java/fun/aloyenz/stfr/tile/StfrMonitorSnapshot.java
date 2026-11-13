package fun.aloyenz.stfr.tile;

import static fun.aloyenz.stfr.tile.TileEntityLogicDevice.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.reactor.StfrCoreSimulation;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;

/** Server-side monitor graphics snapshot construction. */
final class StfrMonitorSnapshot {
	private StfrMonitorSnapshot() {}

	static NBTTagCompound graphicsSnapshot(StfrReactor reactor) {
		StfrCoreSimulation core = reactor.core();
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("reactorCenter", reactor.center.toLong());
		tag.setString("state", reactor.runState().name());
		tag.setInteger("ticks", reactor.stateTicks());
		tag.setBoolean("selfPending", reactor.emergency().selfPending());
		tag.setBoolean("selfAbortable", reactor.emergency().canAbortSelfDestruct());
		tag.setDouble("selfPower", reactor.emergency().selfProjectedMultiplier());
		tag.setInteger(
				"selfRadius",
				StfrEmergencyRules.blastRange(
						StfrConfig.detonationRadius, reactor.emergency().selfProjectedMultiplier()));
		tag.setBoolean("exists", core.exists());
		tag.setDouble("temperature", core.coreTemperature());
		tag.setDouble("caseTemperature", core.caseTemperature());
		tag.setDouble("reaction", core.reactionPower());
		tag.setDouble("transfer", core.heatTransferRate());
		tag.setDouble("cooling", core.coolingRate());
		tag.setDouble("pressure", core.pressure());
		tag.setDouble("rpm", Math.abs(core.rotation()) / 1000D);
		tag.setDouble("shield", core.shield());
		tag.setDouble("stress", core.shieldStress());
		tag.setDouble("capacitor", core.capacitor());
		tag.setInteger("maintenance", reactor.maintenance().count());
		tag.setInteger("errors", reactor.wrongBlocks());
		tag.setBoolean("breach", reactor.sealFailures() > 0);
		for (EnumFacing face : EnumFacing.VALUES)
			tag.setString("section" + face.ordinal(), reactor.turbineState(face));
		// All four displayed quantities use physical mB. Other fusion fuels
		// remain in the simulation but are outside this monitor's layout.
		double plasmaUnits = StfrCoreSimulation.PLASMA_UNITS_PER_MB;
		tag.setDouble(
				"DEUTERIUM", core.amount(StfrCoreSimulation.Fuel.DEUTERIUM, false) / plasmaUnits);
		tag.setDouble("HELIUM4", core.coreHelium4() / plasmaUnits);
		tag.setDouble("POLONIUM", reactor.poloniumMb());
		tag.setDouble("YARONITE", reactor.yaroniteMb());
		return tag;
	}
}

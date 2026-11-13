package fun.aloyenz.stfr.reactor;

import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.structure.StfrPositionTags;

final class StfrReactorPersistence {

	private StfrReactorPersistence() {}

	static NBTTagCompound write(StfrReactor r) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setUniqueId("id", r.id);
		tag.setLong("center", r.center.toLong());
		tag.setInteger("emitterMigrationVersion", r.emitterMigrationVersion);
		tag.setInteger("wrongBlocks", r.wrongBlocks);
		tag.setInteger("blockedAir", r.blockedAir);
		tag.setInteger("blockedChamberAir", r.blockedChamberAir);
		tag.setInteger("missingPorts", r.missingPorts);
		tag.setInteger("fixedHatchesWrong", r.fixedHatchesWrong);
		tag.setInteger("workingTurbines", r.workingTurbines);
		tag.setInteger("workingMask", r.workingMask);
		tag.setInteger("structuralMask", r.structuralMask);
		tag.setInteger("refuelingMask", r.refuelingMask);
		tag.setBoolean("structureValidated", r.structureValidated);
		tag.setString("lockState", r.lockState.name());
		tag.setString("runState", r.runState.name());
		tag.setInteger("stateTicks", r.stateTicks);
		tag.setBoolean("dirtyShutdown", r.dirtyShutdown);
		tag.setInteger("dirtyRadiationTicks", r.dirtyRadiationTicks);
		tag.setTag("caseBreach", StfrPositionTags.write(r.caseBreachPositions));
		tag.setTag("destructible", StfrPositionTags.write(r.destructiblePositions));
		if (r.liftBoxFirst != null) tag.setLong("liftBoxFirst", r.liftBoxFirst.toLong());
		if (r.liftBoxSecond != null) tag.setLong("liftBoxSecond", r.liftBoxSecond.toLong());
		tag.setInteger("liftReleaseLocalY", r.liftReleaseLocalY);
		tag.setLong("rotorAngleUnits", r.rotorAngleUnits);
		tag.setInteger("rotorVelocity", r.rotorVelocity);
		tag.setBoolean("rotorMapExplicit", r.rotorMapExplicit);
		NBTTagList rotorList = new NBTTagList();
		for (Long key : r.rotorPositions) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", key);
			rotorList.appendTag(item);
		}
		tag.setTag("rotorPositions", rotorList);
		NBTTagList axisList = new NBTTagList();
		for (Map.Entry<Long, EnumFacing> entry : r.rotorAxes.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setByte("face", (byte) entry.getValue().getIndex());
			axisList.appendTag(item);
		}
		tag.setTag("rotorAxes", axisList);
		NBTTagList steamList = new NBTTagList();
		for (Map.Entry<Long, Integer> entry : r.steamFaces.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setInteger("faces", entry.getValue());
			steamList.appendTag(item);
		}
		tag.setTag("steamFaces", steamList);
		NBTTagList hiddenList = new NBTTagList();
		for (Map.Entry<Long, IBlockState> entry : r.hiddenRotors.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), entry.getValue()));
			hiddenList.appendTag(item);
		}
		tag.setTag("hiddenRotors", hiddenList);
		NBTTagList electrodeList = new NBTTagList();
		for (Map.Entry<Long, IBlockState> entry : r.aimedElectrodes.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), entry.getValue()));
			electrodeList.appendTag(item);
		}
		tag.setTag("aimedElectrodes", electrodeList);
		tag.setBoolean("keyOn", r.keyOn);
		tag.setBoolean("sstbOn", r.sstbOn);
		tag.setBoolean("breakersOn", r.breakersOn);
		r.autopilotLease.write(tag);
		r.announcementOptions.write(tag);
		tag.setInteger("fuelInputLevel", r.fuelInputLevel);
		tag.setLong("uptimeTicks", r.uptimeTicks);
		tag.setDouble("temperature", r.temperature);
		tag.setDouble("pressure", r.pressure);
		tag.setLong("waterAmount", r.waterAmount);
		tag.setLong("steamAmount", r.steamAmount);
		tag.setLong("lubricantAmount", r.lubricantAmount);
		tag.setLong("productsAmount", r.productsAmount);
		tag.setLong("energyAmount", r.energyAmount);
		tag.setLong("fuelRemaining", r.fuelRemaining);
		tag.setTag("resources", r.resources.write(new NBTTagCompound()));
		tag.setTag("core", r.core.write());
		tag.setTag("maintenance", r.maintenance.write());
		tag.setInteger("previousCoolant", r.previousCoolant);
		tag.setBoolean("coolantIntakeLeaking", r.coolantIntakeLeaking);
		tag.setInteger("startupPowerShortage", r.startupPowerShortage);
		tag.setTag("failure", r.failure.write());
		tag.setTag("otherStates", r.emergency.write());
		tag.setInteger("suppression", r.suppression);
		tag.setInteger("offset", r.offset);
		tag.setInteger("lasers", r.lasers);
		tag.setInteger("vent", r.vent);
		tag.setInteger("permeability", r.permeability);
		tag.setBoolean("digammaActive", r.digammaActive);
		tag.setInteger("digammaSettlingTicks", r.digammaSettlingTicks);
		if (r.armedDestabilizerPos != null)
			tag.setLong("armedDestabilizerPos", r.armedDestabilizerPos.toLong());
		tag.setInteger("digammaHumCountdown", r.digammaHumCountdown);
		tag.setInteger("digammaAmbientTicks", r.digammaAmbientTicks);
		tag.setInteger("destabilizerStage", r.destabilizerStage);
		tag.setInteger("destabilizerTicks", r.destabilizerTicks);
		tag.setDouble("resonanceTemperature", r.resonanceTemperature);
		tag.setDouble("resonancePressure", r.resonancePressure);
		tag.setDouble("digammaCharge", r.digammaCharge);
		r.digammaMatter.write(tag);
		tag.setInteger("savedDigammaFuel", r.savedDigammaFuel);
		tag.setInteger("savedDigammaSuppression", r.savedDigammaSuppression);
		tag.setInteger("savedDigammaOffset", r.savedDigammaOffset);
		tag.setInteger("savedDigammaLasers", r.savedDigammaLasers);
		tag.setInteger("savedDigammaVent", r.savedDigammaVent);
		tag.setInteger("savedDigammaPermeability", r.savedDigammaPermeability);
		for (int i = 0; i < r.fuelFeeds.length; i++)
			tag.setTag("fuelFeed" + i, r.fuelFeeds[i].write());
		return tag;
	}

	static StfrReactor read(NBTTagCompound tag) {
		StfrReactor reactor =
				new StfrReactor(tag.getUniqueId("id"), BlockPos.fromLong(tag.getLong("center")));
		reactor.emitterMigrationVersion = tag.getInteger("emitterMigrationVersion");
		reactor.wrongBlocks = tag.getInteger("wrongBlocks");
		reactor.blockedAir = tag.getInteger("blockedAir");
		reactor.blockedChamberAir = tag.getInteger("blockedChamberAir");
		reactor.missingPorts = tag.getInteger("missingPorts");
		reactor.fixedHatchesWrong =
				tag.hasKey("fixedHatchesWrong")
						? tag.getInteger("fixedHatchesWrong")
						: tag.getInteger("fixedHatchesOpen");
		reactor.workingTurbines = tag.getInteger("workingTurbines");
		reactor.workingMask = tag.getInteger("workingMask");
		reactor.structuralMask = tag.getInteger("structuralMask");
		reactor.refuelingMask = tag.getInteger("refuelingMask");
		reactor.structureValidated = tag.getBoolean("structureValidated");
		if (tag.hasKey("lockState")) {
			try {
				reactor.lockState = StfrReactor.LockState.valueOf(tag.getString("lockState"));
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (tag.hasKey("runState")) {
			try {
				reactor.runState = StfrReactor.RunState.valueOf(tag.getString("runState"));
			} catch (IllegalArgumentException ignored) {
			}
		}
		reactor.stateTicks = Math.max(0, tag.getInteger("stateTicks"));
		reactor.dirtyShutdown = tag.getBoolean("dirtyShutdown");
		reactor.dirtyRadiationTicks = Math.max(0,
				Math.min(tag.getInteger("dirtyRadiationTicks"),
						StfrConfig.dirtyShutdownSourceDurationSeconds * 20 + 1));
		if (tag.hasKey("caseBreach", 9)) {
			reactor.caseBreachPositions.clear();
			reactor.caseBreachPositions.addAll(
					StfrPositionTags.read(tag.getTagList("caseBreach", 10)));
		}
		if (tag.hasKey("destructible", 9))
			reactor.destructiblePositions.addAll(
					StfrPositionTags.read(tag.getTagList("destructible", 10)));
		reactor.liftBoxFirst =
				tag.hasKey("liftBoxFirst") ? BlockPos.fromLong(tag.getLong("liftBoxFirst")) : null;
		reactor.liftBoxSecond =
				tag.hasKey("liftBoxSecond")
						? BlockPos.fromLong(tag.getLong("liftBoxSecond"))
						: null;
		reactor.liftReleaseLocalY =
				tag.hasKey("liftReleaseLocalY") ? tag.getInteger("liftReleaseLocalY") : 32;
		reactor.rotorAngleUnits = tag.getLong("rotorAngleUnits");
		reactor.rotorVelocity = tag.getInteger("rotorVelocity");
		if (tag.hasKey("rotorPositions")) {
			reactor.rotorPositions.clear();
			reactor.rotorAxes.clear();
			reactor.steamFaces.clear();
			NBTTagList list = tag.getTagList("rotorPositions", 10);
			for (int i = 0; i < list.tagCount(); i++)
				reactor.rotorPositions.add(list.getCompoundTagAt(i).getLong("pos"));
			list = tag.getTagList("rotorAxes", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound item = list.getCompoundTagAt(i);
				reactor.rotorAxes.put(
						item.getLong("pos"), EnumFacing.byIndex(item.getByte("face")));
			}
			list = tag.getTagList("steamFaces", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound item = list.getCompoundTagAt(i);
				reactor.steamFaces.put(item.getLong("pos"), item.getInteger("faces"));
			}
			list = tag.getTagList("hiddenRotors", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound item = list.getCompoundTagAt(i);
				reactor.hiddenRotors.put(
						item.getLong("pos"), NBTUtil.readBlockState(item.getCompoundTag("state")));
			}
		}
		reactor.rotorMapExplicit = tag.getBoolean("rotorMapExplicit");
		NBTTagList electrodeList = tag.getTagList("aimedElectrodes", 10);
		for (int i = 0; i < electrodeList.tagCount(); i++) {
			NBTTagCompound item = electrodeList.getCompoundTagAt(i);
			reactor.aimedElectrodes.put(
					item.getLong("pos"), NBTUtil.readBlockState(item.getCompoundTag("state")));
		}
		reactor.keyOn = tag.getBoolean("keyOn");
		// SSTB is a button, not a latched system. Never replay a held pulse after loading.
		reactor.sstbOn = false;
		reactor.breakersOn = tag.getBoolean("breakersOn");
		reactor.autopilotLease.read(tag, reactor.runState == StfrReactor.RunState.ONLINE);
		reactor.announcementOptions.read(tag);
		reactor.fuelInputLevel = Math.max(0, Math.min(100, tag.getInteger("fuelInputLevel")));
		reactor.uptimeTicks = Math.max(0L, tag.getLong("uptimeTicks"));
		reactor.temperature =
				tag.hasKey("temperature") ? Math.max(20, tag.getDouble("temperature")) : 20;
		reactor.pressure = Math.max(0, tag.getDouble("pressure"));
		reactor.waterAmount = Math.max(0, tag.getLong("waterAmount"));
		reactor.steamAmount = Math.max(0, tag.getLong("steamAmount"));
		reactor.lubricantAmount = Math.max(0, tag.getLong("lubricantAmount"));
		reactor.productsAmount = Math.max(0, tag.getLong("productsAmount"));
		reactor.energyAmount = Math.max(0, tag.getLong("energyAmount"));
		reactor.fuelRemaining = Math.max(0, tag.getLong("fuelRemaining"));
		if (tag.hasKey("resources", 10)) reactor.resources.read(tag.getCompoundTag("resources"));
		if (reactor.runState == StfrReactor.RunState.STARTUP_FAILURE)
			reactor.resources.setFailure(true);
		else
			reactor.resources.setOnline(
					reactor.runState == StfrReactor.RunState.ONLINE || reactor.emergencyState());
		reactor.failure.read(tag.getCompoundTag("failure"));
		reactor.emergency.read(tag.getCompoundTag("otherStates"));
		reactor.maintenance.read(tag.getCompoundTag("maintenance"));
		reactor.previousCoolant =
				tag.hasKey("previousCoolant") ? tag.getInteger("previousCoolant") : -1;
		reactor.coolantIntakeLeaking = tag.getBoolean("coolantIntakeLeaking");
		if (tag.hasKey("core", 10)) reactor.core.read(tag.getCompoundTag("core"));
		reactor.startupPowerShortage = Math.max(0, tag.getInteger("startupPowerShortage"));
		reactor.suppression =
				tag.hasKey("suppression")
						? StfrReactor.clamp(tag.getInteger("suppression"), 0, 100)
						: 90;
		reactor.offset = StfrReactor.clamp(tag.getInteger("offset"), -100, 100);
		reactor.lasers = StfrReactor.clamp(tag.getInteger("lasers"), 0, 100);
		reactor.vent = StfrReactor.clamp(tag.getInteger("vent"), 0, 100);
		reactor.permeability = StfrReactor.clamp(tag.getInteger("permeability"), 0, 100);
		reactor.digammaActive = tag.getBoolean("digammaActive");
		reactor.digammaSettlingTicks =
				Math.max(0, Math.min(1_000, tag.getInteger("digammaSettlingTicks")));
		reactor.digammaHumCountdown =
				Math.max(0, Math.min(80, tag.getInteger("digammaHumCountdown")));
		reactor.digammaAmbientTicks =
				Math.max(0, Math.min(599, tag.getInteger("digammaAmbientTicks")));
		reactor.destabilizerStage = Math.max(0, Math.min(6, tag.getInteger("destabilizerStage")));
		if (tag.hasKey("armedDestabilizerPos"))
			reactor.armedDestabilizerPos = BlockPos.fromLong(tag.getLong("armedDestabilizerPos"));
		reactor.destabilizerTicks = Math.max(0, tag.getInteger("destabilizerTicks"));
		reactor.resonanceTemperature = Math.max(0, tag.getDouble("resonanceTemperature"));
		reactor.resonancePressure = Math.max(0, tag.getDouble("resonancePressure"));
		reactor.digammaCharge = Math.max(0, Math.min(100_000_000D, tag.getDouble("digammaCharge")));
		reactor.digammaMatter.read(tag);
		reactor.savedDigammaFuel = StfrReactor.clamp(tag.getInteger("savedDigammaFuel"), 0, 100);
		reactor.savedDigammaSuppression =
				StfrReactor.clamp(tag.getInteger("savedDigammaSuppression"), 0, 100);
		reactor.savedDigammaOffset =
				StfrReactor.clamp(tag.getInteger("savedDigammaOffset"), -100, 100);
		reactor.savedDigammaLasers =
				StfrReactor.clamp(tag.getInteger("savedDigammaLasers"), 0, 100);
		reactor.savedDigammaVent = StfrReactor.clamp(tag.getInteger("savedDigammaVent"), 0, 100);
		reactor.savedDigammaPermeability =
				StfrReactor.clamp(tag.getInteger("savedDigammaPermeability"), 0, 100);
		// Old injectionRemainder was unspent demand, never physical inventory.
		for (int i = 0; i < reactor.fuelFeeds.length; i++)
			reactor.fuelFeeds[i].read(tag.getCompoundTag("fuelFeed" + i));
		return reactor;
	}
}

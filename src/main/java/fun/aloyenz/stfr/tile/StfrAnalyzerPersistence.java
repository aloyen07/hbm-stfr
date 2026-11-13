package fun.aloyenz.stfr.tile;

import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.structure.StfrPositionTags;
import fun.aloyenz.stfr.structure.StfrRotorTemplate;

/** Structure annotations and rotor-map NBT compatibility. */
final class StfrAnalyzerPersistence {
	private StfrAnalyzerPersistence() {}

	static NBTTagCompound writeToNBT(TileEntityStructureAnalyzer c, NBTTagCompound compound) {

		compound.setByte("heightOffset", (byte) c.heightOffset);
		compound.setBoolean("projectionEnabled", c.projectionEnabled);
		compound.setTag("caseBreach", StfrPositionTags.write(c.caseBreachBlocks()));
		compound.setTag("destructible", StfrPositionTags.write(c.destructible));
		if (c.liftBoxFirst != null) compound.setLong("liftBoxFirst", c.liftBoxFirst.toLong());
		if (c.liftBoxSecond != null) compound.setLong("liftBoxSecond", c.liftBoxSecond.toLong());
		if (c.liftReleaseY != Integer.MIN_VALUE) compound.setInteger("liftReleaseY", c.liftReleaseY);
		NBTTagList list = new NBTTagList();
		for (Map.Entry<Long, StfrBlockRole> entry : c.roles.entrySet()) {
			NBTTagCompound role = new NBTTagCompound();
			role.setLong("pos", entry.getKey());
			role.setString("role", entry.getValue().name());
			list.appendTag(role);
		}
		compound.setTag("roles", list);
		NBTTagList miscList = new NBTTagList();
		for (Map.Entry<Long, StfrMiscRole> entry : c.miscRoles.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setString("role", entry.getValue().name());
			IBlockState reference = c.injectorStates.get(entry.getKey());
			if (reference != null)
				item.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), reference));
			miscList.appendTag(item);
		}
		compound.setTag("miscRoles", miscList);
		NBTTagList rotorList = new NBTTagList();
		for (Long key : c.rotors) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", key);
			rotorList.appendTag(item);
		}
		compound.setTag("rotors", rotorList);
		NBTTagList axisList = new NBTTagList();
		for (Map.Entry<Long, EnumFacing> entry : c.axes.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setByte("face", (byte) entry.getValue().getIndex());
			axisList.appendTag(item);
		}
		compound.setTag("axes", axisList);
		NBTTagList particleList = new NBTTagList();
		for (Map.Entry<Long, Integer> entry : c.particleFaces.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setByte("faces", entry.getValue().byteValue());
			particleList.appendTag(item);
		}
		compound.setTag("rotorParticles", particleList);
		NBTTagList hiddenList = new NBTTagList();
		for (Map.Entry<Long, IBlockState> entry : c.hiddenRotorStates.entrySet()) {
			NBTTagCompound item = new NBTTagCompound();
			item.setLong("pos", entry.getKey());
			item.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), entry.getValue()));
			hiddenList.appendTag(item);
		}
		compound.setTag("hiddenRotors", hiddenList);
		compound.setDouble("rotorAngle", c.rotorAngle);
		compound.setInteger("rotorVelocity", c.rotorVelocity);
		compound.setInteger("visualRunState", c.visualRunState);
		compound.setInteger("visualStateTicks", c.visualStateTicks);
		return compound;
	}

	static void readFromNBT(TileEntityStructureAnalyzer c, NBTTagCompound compound) {

		c.heightOffset = Math.max(1, Math.min(16, compound.getByte("heightOffset") & 255));
		c.projectionEnabled = !compound.hasKey("projectionEnabled") || compound.getBoolean("projectionEnabled");
		c.roles.clear();
		c.miscRoles.clear();
		c.injectorStates.clear();
		NBTTagList miscList = compound.getTagList("miscRoles", 10);
		for (int i = 0; i < miscList.tagCount(); i++) {
			NBTTagCompound item = miscList.getCompoundTagAt(i);
			BlockPos local = BlockPos.fromLong(item.getLong("pos"));
			if (!c.containsLocalPos(local)) continue;
			try {
				StfrMiscRole role = StfrMiscRole.valueOf(item.getString("role"));
				IBlockState reference =
						item.hasKey("state", 10)
								? NBTUtil.readBlockState(item.getCompoundTag("state"))
								: null;
				if (role == StfrMiscRole.INJECTOR_PLACEABLE
						&& (reference == null || reference.getBlock() != STFRBlocks.SOLID_INJECTOR))
					continue;
				c.miscRoles.put(local.toLong(), role);
				if (reference != null) c.injectorStates.put(local.toLong(), reference);
			} catch (IllegalArgumentException ignored) {
			}
		}
		c.caseBreach.clear();
		c.caseBreachExplicit = compound.hasKey("caseBreach", 9);
		c.caseBreach.addAll(StfrPositionTags.read(compound.getTagList("caseBreach", 10)));
		c.destructible.clear();
		c.destructible.addAll(StfrPositionTags.read(compound.getTagList("destructible", 10)));
		c.liftBoxFirst =
				compound.hasKey("liftBoxFirst")
						? BlockPos.fromLong(compound.getLong("liftBoxFirst"))
						: null;
		c.liftBoxSecond =
				compound.hasKey("liftBoxSecond")
						? BlockPos.fromLong(compound.getLong("liftBoxSecond"))
						: null;
		c.liftReleaseY =
				compound.hasKey("liftReleaseY")
						? compound.getInteger("liftReleaseY")
						: Integer.MIN_VALUE;
		c.rotors.clear();
		c.axes.clear();
		c.particleFaces.clear();
		c.hiddenRotorStates.clear();
		c.rotorMapExplicit = compound.hasKey("rotors", 9);
		if (!c.rotorMapExplicit) StfrRotorTemplate.populate(c.rotors, c.axes, c.particleFaces);
		NBTTagList list = compound.getTagList("roles", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound role = list.getCompoundTagAt(i);
			try {
				c.roles.put(role.getLong("pos"), StfrBlockRole.valueOf(role.getString("role")));
			} catch (IllegalArgumentException ignored) {
			}
		}
		NBTTagList rotorList = compound.getTagList("rotors", 10);
		for (int i = 0; i < rotorList.tagCount(); i++)
			c.rotors.add(rotorList.getCompoundTagAt(i).getLong("pos"));
		NBTTagList axisList = compound.getTagList("axes", 10);
		for (int i = 0; i < axisList.tagCount(); i++) {
			NBTTagCompound item = axisList.getCompoundTagAt(i);
			c.axes.put(item.getLong("pos"), EnumFacing.byIndex(item.getByte("face")));
		}
		NBTTagList particleList = compound.getTagList("rotorParticles", 10);
		for (int i = 0; i < particleList.tagCount(); i++) {
			NBTTagCompound item = particleList.getCompoundTagAt(i);
			c.particleFaces.put(item.getLong("pos"), item.getByte("faces") & 63);
		}
		NBTTagList hiddenList = compound.getTagList("hiddenRotors", 10);
		for (int i = 0; i < hiddenList.tagCount(); i++) {
			NBTTagCompound item = hiddenList.getCompoundTagAt(i);
			c.hiddenRotorStates.put(
					item.getLong("pos"), NBTUtil.readBlockState(item.getCompoundTag("state")));
		}
		c.rotorAngle = compound.getDouble("rotorAngle");
		c.rotorVelocity = compound.getInteger("rotorVelocity");
		c.visualRunState = compound.getInteger("visualRunState");
		c.visualStateTicks = compound.getInteger("visualStateTicks");
		c.rotorSyncTick = c.getWorld() == null ? 0 : c.getWorld().getTotalWorldTime();
	}
}

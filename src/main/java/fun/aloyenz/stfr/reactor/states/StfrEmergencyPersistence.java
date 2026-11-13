package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** NBT compatibility for emergency scenes and saved recovery state. */
final class StfrEmergencyPersistence {
	private StfrEmergencyPersistence() {}

	static NBTTagCompound write(StfrEmergencyController c) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("stage", c.stage);
		if (c.manualNetwork != null) tag.setUniqueId("manualNetwork", c.manualNetwork);
		tag.setInteger("recovery", c.recovery);
		tag.setBoolean("recovering", c.recovering);
		tag.setBoolean("manualFailed", c.manualFailed);
		tag.setBoolean("selfWasOffline", c.selfWasOffline);
		tag.setBoolean("internalWarning", c.internalWarning);
		tag.setString("music", c.music);
		tag.setFloat("musicPitch", c.musicPitch);
		tag.setBoolean("selfPending", c.selfPending);
		tag.setBoolean("selfCommitted", c.selfCommitted);
		tag.setBoolean("selfFinale", c.selfFinale);
		tag.setBoolean("selfBlast", c.selfBlast);
		tag.setInteger("selfCharge", c.selfCharge);
		tag.setInteger("emitterMask", c.emitterMask);
		tag.setBoolean("emitterInstructionSent",c.emitterInstructionSent);
		tag.setString("selfPreviousState", c.selfPreviousState.name());
		tag.setInteger("selfPreviousTicks", c.selfPreviousTicks);
		tag.setTag("previousEmergencySnapshot", c.previousEmergencySnapshot.copy());
		NBTTagList entries = new NBTTagList();
		for (Map.Entry<Long, IBlockState> lens : c.lenses.entrySet()) {
			NBTTagCompound entry = new NBTTagCompound();
			entry.setLong("pos", lens.getKey());
			entry.setTag("state", NBTUtil.writeBlockState(new NBTTagCompound(), lens.getValue()));
			entries.appendTag(entry);
		}
		tag.setTag("lenses", entries);
		return tag;
	}

	static NBTTagCompound previousSnapshot(NBTTagCompound tag) {
		return tag.getCompoundTag(tag.hasKey("previousEmergencySnapshot",10)?
				"previousEmergencySnapshot":"selfPreviousOther");
	}

	static void read(StfrEmergencyController c, NBTTagCompound tag) {
		c.selfPending = tag.getBoolean("selfPending");
		c.selfCommitted = tag.getBoolean("selfCommitted");
		c.selfFinale = tag.getBoolean("selfFinale");
		c.selfBlast = tag.getBoolean("selfBlast");
		c.selfCharge = Math.max(0, tag.getInteger("selfCharge"));
		c.emitterMask = tag.getInteger("emitterMask");
		c.emitterInstructionSent=tag.getBoolean("emitterInstructionSent");
		try {
			c.selfPreviousState = StfrReactor.RunState.valueOf(tag.getString("selfPreviousState"));
		} catch (IllegalArgumentException ignored) {
			c.selfPreviousState = OFFLINE;
		}
		c.selfPreviousTicks = tag.getInteger("selfPreviousTicks");
		c.previousEmergencySnapshot = previousSnapshot(tag).copy();
		c.manualNetwork = tag.hasUniqueId("manualNetwork") ? tag.getUniqueId("manualNetwork") : null;
		c.stage = Math.max(0, Math.min(5, tag.getInteger("stage")));
		c.recovery = Math.max(0, tag.getInteger("recovery"));
		c.recovering = tag.getBoolean("recovering") || c.recovery > 0;
		c.manualFailed = tag.getBoolean("manualFailed");
		c.selfWasOffline = tag.getBoolean("selfWasOffline");
		c.internalWarning = tag.getBoolean("internalWarning");
		c.music = tag.getString("music");
		c.musicPitch = tag.hasKey("musicPitch") ? tag.getFloat("musicPitch") : 1;
		c.musicAudience.clear();
		c.lenses.clear();
		NBTTagList entries = tag.getTagList("lenses", 10);
		for (int i = 0; i < entries.tagCount(); i++) {
			NBTTagCompound e = entries.getCompoundTagAt(i);
			c.lenses.put(e.getLong("pos"), NBTUtil.readBlockState(e.getCompoundTag("state")));
		}
	}
}

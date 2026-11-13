package fun.aloyenz.stfr.reactor;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/** Saved owner of an already engaged autopilot; live controls still require a network check. */
final class StfrAutopilotLease {
	private boolean armed;
	private int level;
	private BlockPos owner;
	private long engagedTick;

	boolean armed() { return armed; }
	BlockPos owner() { return owner; }
	long engagedTick() { return engagedTick; }

	void remember(BlockPos owner, int level, long engagedTick) {
		this.armed = true;
		this.owner = owner.toImmutable();
		this.level = level;
		this.engagedTick = Math.max(0L, engagedTick);
	}

	void clear() {
		armed = false;
		owner = null;
		level = 0;
		engagedTick = 0L;
	}

	boolean canResume(BlockPos owner, int level, boolean online, boolean destabilizing) {
		return armed && online && !destabilizing && this.level == level
				&& owner.equals(this.owner);
	}

	void write(NBTTagCompound tag) {
		tag.setBoolean("autopilotArmed", armed);
		if (armed && owner != null) {
			tag.setLong("autopilotOwnerPos", owner.toLong());
			tag.setInteger("autopilotArmedLevel", level);
			tag.setLong("autopilotEngagedTick", engagedTick);
		}
	}

	void read(NBTTagCompound tag, boolean online) {
		clear();
		if (!online || !tag.getBoolean("autopilotArmed")
				|| !tag.hasKey("autopilotOwnerPos")) return;
		int savedLevel = tag.getInteger("autopilotArmedLevel");
		if (savedLevel < 1 || savedLevel > 3) return;
		remember(BlockPos.fromLong(tag.getLong("autopilotOwnerPos")), savedLevel,
				tag.getLong("autopilotEngagedTick"));
	}
}

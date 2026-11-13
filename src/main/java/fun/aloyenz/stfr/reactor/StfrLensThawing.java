package fun.aloyenz.stfr.reactor;

import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;

/** Per-lens state. Recipe lookups must never roll or advance this counter. */
public final class StfrLensThawing {
	public static final String KEY = "stfrThawRemaining";
	public static final int MIN = 3, MAX = 16;
	private StfrLensThawing() {}

	public static int remaining(NBTTagCompound tag, Random random) {
		if (!tag.hasKey(KEY, 3)) tag.setInteger(KEY, MIN + random.nextInt(MAX - MIN + 1));
		int value = Math.max(1, Math.min(MAX, tag.getInteger(KEY)));
		tag.setInteger(KEY, value);
		return value;
	}

	/** Called exactly once per completed furnace operation. Zero means thawed. */
	public static int process(NBTTagCompound tag, Random random) {
		int value = remaining(tag, random) - 1;
		tag.setInteger(KEY, value);
		return value;
	}
}

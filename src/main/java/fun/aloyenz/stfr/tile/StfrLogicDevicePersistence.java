package fun.aloyenz.stfr.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.StfrMainframePower;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.logic.StfrMonitorText;
import fun.aloyenz.stfr.logic.StfrSignal;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;

/** Saved signal, monitor and Mainframe device settings. */
final class StfrLogicDevicePersistence {
	private StfrLogicDevicePersistence() {}

	static NBTTagCompound writeToNBT(TileEntityLogicDevice c, NBTTagCompound tag) {

		for (int i = 0; i < 6; i++) tag.setString("signal" + i, c.signals[i].name());
		for (int i = 0; i < 6; i++) tag.setInteger("repeatInterval" + i, c.repeatIntervals[i]);
		tag.setLong("mainframeEnergy", c.mainframeEnergy);
		tag.setBoolean("mainframePowered", c.mainframePowered);
		tag.setBoolean("inlineMode", c.inlineMode);
		tag.setInteger("crashTicks", c.crashTicks);
		if (c.crashOrigin != null) tag.setLong("crashOrigin", c.crashOrigin.toLong());
		tag.setInteger("suppressionMask", c.suppressionMask);
		tag.setLong("autopilotFuelThreshold", c.autopilotFuelThreshold);
		tag.setBoolean("autopilotForceShutdown", c.autopilotForceShutdown);
		tag.setBoolean("monitorConnected", c.monitorConnected);
		tag.setInteger("catStage", c.catStage);
		tag.setBoolean("monitorUnstable", c.monitorUnstable);
		tag.setString("monitorDisplay", c.monitorDisplay.name());
		tag.setTag("monitorGraphics", c.monitorGraphics.copy());
		for (int i = 0; i < 4; i++) tag.setString("monitorLine" + i, c.monitorLines[i]);
		for (int i = 0; i < 4; i++) tag.setString("templateLine" + i, c.templateLines[i]);
		for (int i = 0; i < 4; i++) tag.setInteger("monitorColor" + i, c.monitorColors[i]);
		return tag;
	}

	static void readFromNBT(TileEntityLogicDevice c, NBTTagCompound tag) {

		c.mainframeEnergy =
				Math.max(
						0,
						Math.min(
								StfrMainframePower.BUFFER_PER_BLOCK,
								tag.getLong("mainframeEnergy")));
		c.mainframePowered = tag.getBoolean("mainframePowered");
		c.mainframePowerTick = Long.MIN_VALUE;
		c.inlineMode = tag.getBoolean("inlineMode");
		c.crashTicks = Math.max(0, tag.getInteger("crashTicks"));
		c.crashOrigin = tag.hasKey("crashOrigin") ? BlockPos.fromLong(tag.getLong("crashOrigin")) : null;
		c.suppressionMask =
				tag.hasKey("suppressionMask")
						? tag.getInteger("suppressionMask") & StfrFailureEffects.ALL
						: StfrFailureEffects.ALL;
		c.autopilotFuelThreshold =
				tag.hasKey("autopilotFuelThreshold")
						? Math.max(0L, tag.getLong("autopilotFuelThreshold"))
						: 6000L;
		c.autopilotForceShutdown = tag.getBoolean("autopilotForceShutdown");
		c.monitorConnected = tag.getBoolean("monitorConnected");
		c.catStage = Math.max(0, Math.min(3, tag.getInteger("catStage")));
		c.monitorUnstable = tag.getBoolean("monitorUnstable");
		c.monitorDisplay = StfrMonitorDisplay.read(tag.getString("monitorDisplay"));
		c.monitorGraphics = tag.getCompoundTag("monitorGraphics");
		for (int i = 0; i < 4; i++) c.monitorLines[i] = tag.getString("monitorLine" + i);
		for (int i = 0; i < 4; i++)
			c.templateLines[i] =
					tag.hasKey("templateLine" + i)
							? tag.getString("templateLine" + i)
							: StfrMonitorText.DEFAULT[i];
		for (int i = 0; i < 4; i++)
			c.monitorColors[i] =
					tag.hasKey("monitorColor" + i) ? tag.getInteger("monitorColor" + i) : 0xF4F4F4;
		for (int i = 0; i < 6; i++) {
			c.repeatIntervals[i] = Math.max(0, tag.getInteger("repeatInterval" + i));
			c.repeater.reset(i);
			try {
				c.signals[i] = StfrSignal.valueOf(tag.getString("signal" + i));
			} catch (IllegalArgumentException ignored) {
				c.signals[i] = StfrSignal.NONE;
			}
		}
	}
}

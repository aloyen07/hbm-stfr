package fun.aloyenz.stfr.client.render;

import static fun.aloyenz.stfr.client.render.StfrMonitorGraphics.*;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.reactor.StfrCoreSimulation;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Vector layouts for reactor telemetry and emergency monitor panels. */
final class StfrMonitorPanels {
	private StfrMonitorPanels() {}

	static void selfDestruct(FontRenderer f, NBTTagCompound d, TileEntityLogicDevice tile) {
		int color = (tile.getWorld().getTotalWorldTime() / 8 & 1) == 0 ? AMBER : RED;
		boolean pending = d.getBoolean("selfPending");
		int tick =
				d.getInteger("ticks")
						+ (pending
								? 0
								: (int)
										Math.min(
												10,
												Math.max(
														0,
														tile.getWorld().getTotalWorldTime()
																- d.getLong("snapshotTime"))));
		largeCentered(f, I18n.format("gui.hbmstfr.monitor.self_title"), 5, color, 1.5F);
		if (pending) {
			largeCentered(f, I18n.format("gui.hbmstfr.monitor.self_waiting"), 28, color, 1.7F);
			largeCentered(f, I18n.format("gui.hbmstfr.monitor.self_turbine"), 55, TEXT, 1.5F);
		} else
			switch (tile.monitorDisplay()) {
				case REACTOR:
					List<String> admin =
							f.listFormattedStringToWidth(
									I18n.format("gui.hbmstfr.monitor.self_admin"), 165);
					for (int i = 0; i < Math.min(2, admin.size()); i++)
						largeCentered(f, admin.get(i), 23 + i * 12, color, 1.2F);
					largeCentered(
							f,
							I18n.format(
									"gui.hbmstfr.monitor.self_progress",
									Math.min(100, tick * 100 / 3740)),
							51,
							TEXT,
							1.6F);
					bar(8, 73, 204, 4, tick / 3740D, color);
					break;
				case REACTION:
					largeCentered(
							f,
							I18n.format(
									d.getBoolean("selfAbortable")
											? "gui.hbmstfr.monitor.self_abort_deadline"
											: "gui.hbmstfr.monitor.self_irrevocable"),
							24,
							color,
							1.4F);
					largeCentered(
							f,
							StfrEmergencyRules.clock(
									Math.max(
											0,
											(d.getBoolean("selfAbortable") ? 2096 : 3740) - tick)),
							40,
							TEXT,
							2.5F);
					largeCentered(
							f,
							I18n.format(
									"gui.hbmstfr.monitor.self_explode",
									Math.max(0, 3740 - tick) / 20),
							69,
							color,
							1.1F);
					break;
				case CORE:
					largeCentered(
							f,
							I18n.format(
									"gui.hbmstfr.monitor.self_power",
									String.format(Locale.ROOT, "%.2f", d.getDouble("selfPower"))),
							25,
							TEXT,
							1.8F);
					largeCentered(
							f,
							I18n.format(
									"gui.hbmstfr.monitor.self_radius", d.getInteger("selfRadius")),
							45,
							TEXT,
							1.6F);
					largeCentered(
							f, I18n.format("gui.hbmstfr.monitor.self_evacuate"), 68, color, 1.2F);
					break;
				default:
					largeCentered(
							f,
							I18n.format(
									tick < 2305
											? "gui.hbmstfr.monitor.self_expires"
											: "gui.hbmstfr.monitor.self_countdown"),
							24,
							color,
							1.4F);
					largeCentered(
							f,
							StfrEmergencyRules.clock(StfrEmergencyRules.selfScreenRemaining(tick)),
							40,
							color,
							2.5F);
					largeCentered(
							f, I18n.format("gui.hbmstfr.monitor.self_evacuate"), 69, color, 1.1F);
					break;
			}
	}

	static void reaction(FontRenderer f, NBTTagCompound d) {
		title(f, StfrMonitorDisplay.REACTION, CYAN);
		double temperature = d.getDouble("temperature"),
				nominal = StfrCoreSimulation.CORE_TEMP_NOMINAL;
		double maximum = Math.max(nominal * 2D, Math.min(nominal * 8D, temperature * 1.12D));
		// 74 segments, as in the original dedicated reactivity graph.
		rect(
				8 + 132 * nominal / maximum,
				20,
				8 + 132 * StfrCoreSimulation.CORE_TEMP_SAFE_MAX / maximum,
				64,
				0x142D2B);
		for (int i = 0; i <= 4; i++) line(8, 20 + 11 * i, 140, 20 + 11 * i, DIM);
		for (int i = 0; i <= 4; i++) line(8 + 33 * i, 20, 8 + 33 * i, 64, DIM);
		for (int i = 1; i <= 74; i++) {
			double t0 = maximum * (i - 1) / 74D, t1 = maximum * i / 74D;
			line(
					8 + 132D * (i - 1) / 74D,
					64 - 44 * StfrCoreSimulation.reactivity(t0) / .2D,
					8 + 132D * i / 74D,
					64 - 44 * StfrCoreSimulation.reactivity(t1) / .2D,
					d.getBoolean("exists") ? CYAN : DIM);
		}
		double peakX = 8 + 132 * nominal / maximum;
		line(peakX, 18, peakX, 66, AMBER);
		if (d.getBoolean("exists")) {
			double x = 8 + 132 * Math.min(1D, temperature / maximum),
					y = 64 - 44 * StfrCoreSimulation.reactivity(temperature) / .2D;
			line(x, 20, x, 64, TEXT);
			rect(x - 2, y - 2, x + 2, y + 2, TEXT);
		}
		text(f, "0", 8, 68, DIM, 20);
		text(f, compact(maximum) + " °kC", 70, 68, TEXT, 74);
		text(f, I18n.format("gui.hbmstfr.monitor.temperature"), 150, 21, DIM, 65);
		text(f, compact(temperature), 150, 32, TEXT, 65);
		text(
				f,
				"R(T) "
						+ String.format(
								Locale.ROOT,
								"%.0f%%",
								StfrCoreSimulation.reactivity(temperature) / .2D * 100),
				150,
				45,
				CYAN,
				65);
		String status =
				!d.getBoolean("exists")
						? "off"
						: temperature < nominal
								? "below"
								: temperature > StfrCoreSimulation.CORE_TEMP_SAFE_MAX
										? "above"
										: "stable";
		text(
				f,
				I18n.format("gui.hbmstfr.monitor.reaction." + status),
				150,
				56,
				status.equals("stable") ? GREEN : status.equals("above") ? RED : AMBER,
				65);
		text(f, "RX " + compact(d.getDouble("reaction")) + " MW", 150, 68, GREEN, 65);
	}

	static void reactor(FontRenderer f, NBTTagCompound d, long time) {
		String state = d.getString("state");
		int color = stateColor(state);
		title(f, StfrMonitorDisplay.REACTOR, color);
		EnumFacing[] faces = {
			EnumFacing.UP,
			EnumFacing.EAST,
			EnumFacing.SOUTH,
			EnumFacing.DOWN,
			EnumFacing.WEST,
			EnumFacing.NORTH
		};
		String[] names = {"U", "E", "S", "D", "W", "N"};
		int[] dx = {0, 27, 27, 0, -27, -27}, dy = {-16, -10, 10, 16, 10, -10};
		int[] labelX = {48, 86, 86, 48, 12, 12}, labelY = {18, 32, 52, 67, 52, 32};
		for (int i = 0; i < 6; i++) {
			int c = sectionColor(d.getString("section" + faces[i].ordinal()));
			line(48, 46, 48 + dx[i], 46 + dy[i], c);
			ring(48 + dx[i], 46 + dy[i], 3, c);
			centeredText(f, names[i], labelX[i], labelY[i], c, 8);
		}
		ring(48, 46, 10, color);
		ring(48, 46, 7, DIM);
		if (d.getBoolean("exists")) ring(48, 46, 3 + (Math.sin(time * .12) + 1), color);
		text(
				f,
				I18n.format("gui.hbmstfr.monitor.state." + state.toLowerCase(Locale.ROOT)),
				99,
				21,
				color,
				114);
		text(
				f,
				I18n.format("gui.hbmstfr.monitor.errors", d.getInteger("errors")),
				99,
				34,
				TEXT,
				114);
		text(
				f,
				I18n.format("gui.hbmstfr.monitor.maintenance", d.getInteger("maintenance")),
				99,
				46,
				d.getInteger("maintenance") > 0 ? AMBER : TEXT,
				114);
		int ticks = d.getInteger("ticks"),
				duration =
						state.equals("STARTUP_CONFIRMED")
								? 1030
								: state.equals("SHUTDOWN")
										? 1140
										: state.equals("STARTUP_FAILURE")
												? 8040
												: state.equals("MELTDOWN") ? 1398 : 0;
		if (duration == 0) {
			try {
				duration = StfrEmergencyTimeline.deadline(StfrReactor.RunState.valueOf(state));
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (duration > 0) {
			bar(99, 60, 113, 4, (double) ticks / duration, color);
			boolean critical =
					!state.equals("STARTUP_CONFIRMED")
							&& !state.equals("SHUTDOWN")
							&& !state.equals("REIGNITION");
			int seconds = Math.max(0, duration - ticks) / 20;
			text(
					f,
					critical
							? String.format(Locale.ROOT, "T−%d:%02d", seconds / 60, seconds % 60)
							: String.format(
									Locale.ROOT, "%.0f%%", Math.min(100, 100D * ticks / duration)),
					99,
					67,
					color,
					114);
		} else
			text(
					f,
					"SHIELD " + String.format(Locale.ROOT, "%.0f%%", d.getDouble("shield")),
					99,
					64,
					CYAN,
					114);
	}

	static void core(FontRenderer f, NBTTagCompound d, long time) {
		int color =
				d.getString("state").equals("STARTUP_FAILURE")
						? PURPLE
						: d.getBoolean("exists") ? CYAN : DIM;
		title(f, StfrMonitorDisplay.CORE, color);
		ring(34, 43, 20, DIM);
		boolean shieldActive =
				!d.getString("state").equals("OFFLINE")
						&& !d.getString("state").equals("DETONATION");
		arc(
				34,
				43,
				20,
				-Math.PI / 2,
				-Math.PI / 2 + Math.PI * 2 * d.getDouble("shield") / 100D,
				!shieldActive ? DIM : d.getDouble("stress") >= 100 ? RED : CYAN);
		ring(34, 43, 11, color);
		if (d.getBoolean("exists")) {
			ring(34, 43, 6 + (Math.sin(time * .09) + 1), color);
			for (int i = 0; i < 3; i++) {
				double a = time * .035 + i * Math.PI * 2 / 3;
				double x = 34 + 14 * Math.cos(a), y = 43 + 14 * Math.sin(a);
				rect(x - 1, y - 1, x + 1, y + 1, TEXT);
			}
		}
		centeredText(f, d.getBoolean("exists") ? "ON" : "OFF", 34, 39, TEXT, 24);
		centeredText(
				f,
				"SH "
						+ (shieldActive
								? String.format(Locale.ROOT, "%.0f%%", d.getDouble("shield"))
								: "—"),
				34,
				67,
				shieldActive ? CYAN : DIM,
				54);
		text(f, "T " + compact(d.getDouble("temperature")) + " °kC", 64, 20, TEXT, 148);
		text(
				f,
				"P " + compact(d.getDouble("pressure")) + "  RPM " + compact(d.getDouble("rpm")),
				64,
				32,
				TEXT,
				148);
		String[] substances = {"DEUTERIUM", "HELIUM4", "POLONIUM", "YARONITE"};
		String[] names = {"deuterium", "helium", "polonium", "yaronite"};
		int[] colors = {CYAN, GREEN, PURPLE, AMBER};
		double total = 0;
		for (String substance : substances) total += Math.max(0, d.getDouble(substance));
		// Four compact rows stay entirely to the right of the animated core.
		for (int i = 0; i < substances.length; i++) {
			int y = 43 + i * 9;
			double percent = total > 0 ? 100 * Math.max(0, d.getDouble(substances[i])) / total : 0;
			String value =
					percent > 0 && percent < .05D
							? "<0.1%"
							: String.format(Locale.ROOT, "%.1f%%", percent);
			text(
					f,
					I18n.format("gui.hbmstfr.monitor.composition." + names[i]),
					64,
					y,
					colors[i],
					56);
			text(f, value, 123, y, TEXT, 43);
			bar(170, y + 3, 42, 3, percent / 100D, colors[i]);
		}
	}
}

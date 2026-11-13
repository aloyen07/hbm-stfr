package fun.aloyenz.stfr.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Vector panels fit the existing sign face; no dependency on TERF's custom font. */
final class StfrMonitorGraphics {

	static final int DIM = 0x284653,
			TEXT = 0xD8EEF3,
			CYAN = 0x67DCEC,
			GREEN = 0x82E4AC,
			AMBER = 0xFFBB65,
			RED = 0xFF626D,
			PURPLE = 0xC489FF;

	private StfrMonitorGraphics() {}

	static final List<Runnable> labels = new ArrayList<>();

	static void render(FontRenderer font, TileEntityLogicDevice tile) {
		NBTTagCompound d = tile.monitorGraphics();
		GlStateManager.pushMatrix();
		GlStateManager.translate(-.44, .164, .003);
		GlStateManager.scale(.004, -.004, .004);
		// Keep world occlusion, but composite the panel in drawing order. Coplanar
		// fills/grid/curves must not compete with each other in the depth buffer.
		boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		GlStateManager.depthMask(false);
		try {
			GlStateManager.disableCull();
			labels.clear();
			GlStateManager.disableTexture2D();
			Tessellator.getInstance()
					.getBuffer()
					.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			rect(0, 0, 220, 82, 0x07151D);
			rect(0, 0, 220, 1, DIM);
			rect(0, 81, 220, 82, DIM);
			if (!d.hasKey("state"))
				text(font, I18n.format("gui.hbmstfr.monitor.waiting"), 12, 35, TEXT, 195);
			else if (d.getBoolean("selfPending") || "SELF_DESTRUCT".equals(d.getString("state")))
				selfDestruct(font, d, tile);
			else
				switch (tile.monitorDisplay()) {
					case REACTION:
						reaction(font, d);
						break;
					case REACTOR:
						reactor(font, d, tile.getWorld().getTotalWorldTime());
						break;
					case CORE:
						core(font, d, tile.getWorld().getTotalWorldTime());
						break;
					default:
						break;
				}
			Tessellator.getInstance().draw();
			GlStateManager.enableTexture2D();
			for (Runnable label : labels) label.run();
			labels.clear();
		} finally {
			GlStateManager.depthMask(depthWrite);
			GlStateManager.enableCull();
			GlStateManager.color(1, 1, 1, 1);
			GlStateManager.popMatrix();
		}
	}

	static void selfDestruct(FontRenderer f, NBTTagCompound d, TileEntityLogicDevice tile) {
		StfrMonitorPanels.selfDestruct(f, d, tile);
	}

	static void largeCentered(
			FontRenderer font, String value, int y, int color, float desiredScale) {
		float scale = Math.min(desiredScale, 204F / Math.max(1, font.getStringWidth(value)));
		labels.add(
				() -> {
					GlStateManager.pushMatrix();
					GlStateManager.translate(110, y, .05);
					GlStateManager.scale(scale, scale, 1);
					font.drawString(value, -font.getStringWidth(value) / 2F, 0, color, false);
					GlStateManager.popMatrix();
				});
	}

	static void reaction(FontRenderer f, NBTTagCompound d) {
		StfrMonitorPanels.reaction(f, d);
	}

	static void reactor(FontRenderer f, NBTTagCompound d, long time) {
		StfrMonitorPanels.reactor(f, d, time);
	}

	static void core(FontRenderer f, NBTTagCompound d, long time) {
		StfrMonitorPanels.core(f, d, time);
	}

	static void title(FontRenderer f, StfrMonitorDisplay mode, int color) {
		text(f, I18n.format(mode.translationKey()), 6, 4, color, 208);
		rect(6, 15, 214, 16, DIM);
	}

	static int stateColor(String state) {
		switch (state) {
			case "ONLINE":
				return GREEN;
			case "OFFLINE":
				return 0x79929C;
			case "REACTION_LOSS":
			case "REIGNITION":
				return 0x72DDF5;
			case "OVERLOAD":
			case "UNDERLOAD":
			case "SELF_DESTRUCT":
			case "SHUTDOWN_FAILURE":
				return RED;
			case "STARTUP_FAILURE":
				return PURPLE;
			case "MELTDOWN":
			case "DETONATION":
				return RED;
			default:
				return AMBER;
		}
	}

	static int sectionColor(String state) {
		switch (state) {
			case "READY":
				return GREEN;
			case "REFUELING":
				return AMBER;
			case "MAINTENANCE":
				return 0xFF9060;
			default:
				return RED;
		}
	}

	static String compact(double value) {
		return String.format(
				Locale.ROOT,
				value >= 1e6 ? "%.2fM" : value >= 1e3 ? "%.1fk" : "%.0f",
				value >= 1e6 ? value / 1e6 : value >= 1e3 ? value / 1e3 : value);
	}

	static void text(FontRenderer font, String text, int x, int y, int color, int width) {
		labels.add(
				() -> {
					GlStateManager.pushMatrix();
					GlStateManager.translate(0, 0, .05);
					font.drawString(font.trimStringToWidth(text, width), x, y, color);
					GlStateManager.popMatrix();
				});
	}

	static void centeredText(
			FontRenderer font, String value, int center, int y, int color, int width) {
		String clipped = font.trimStringToWidth(value, width);
		text(font, clipped, center - font.getStringWidth(clipped) / 2, y, color, width);
	}

	static void bar(
			double x, double y, double width, double height, double value, int color) {
		rect(x, y, x + width, y + height, DIM);
		rect(x, y, x + width * Math.max(0, Math.min(1, value)), y + height, color);
	}

	static void ring(double x, double y, double r, int color) {
		arc(x, y, r, 0, Math.PI * 2, color);
	}

	static void arc(double x, double y, double r, double from, double to, int color) {
		for (int i = 1; i <= 48; i++) {
			double a = from + (to - from) * (i - 1) / 48, b = from + (to - from) * i / 48;
			line(
					x + r * Math.cos(a),
					y + r * Math.sin(a),
					x + r * Math.cos(b),
					y + r * Math.sin(b),
					color);
		}
	}

	static void line(double x1, double y1, double x2, double y2, int color) {
		double length = Math.hypot(x2 - x1, y2 - y1);
		if (length < 1e-8) return;
		double dx = -(y2 - y1) / length * .45, dy = (x2 - x1) / length * .45;
		quad(x1 + dx, y1 + dy, x2 + dx, y2 + dy, x2 - dx, y2 - dy, x1 - dx, y1 - dy, color);
	}

	static void rect(double x1, double y1, double x2, double y2, int color) {
		quad(x1, y1, x2, y1, x2, y2, x1, y2, color);
	}

	static void quad(
			double x1,
			double y1,
			double x2,
			double y2,
			double x3,
			double y3,
			double x4,
			double y4,
			int c) {
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		int r = c >> 16 & 255, g = c >> 8 & 255, blue = c & 255;
		b.pos(x1, y1, 0).color(r, g, blue, 255).endVertex();
		b.pos(x2, y2, 0).color(r, g, blue, 255).endVertex();
		b.pos(x3, y3, 0).color(r, g, blue, 255).endVertex();
		b.pos(x4, y4, 0).color(r, g, blue, 255).endVertex();
	}
}

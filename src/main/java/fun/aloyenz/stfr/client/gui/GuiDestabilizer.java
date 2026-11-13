package fun.aloyenz.stfr.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public final class GuiDestabilizer extends GuiScreen {

	private final int stage;
	private final float compression, charge;
	private final boolean powered, active;

	public GuiDestabilizer(
			int stage, float compression, float charge, boolean powered, boolean active) {
		this.stage = stage;
		this.compression = compression;
		this.charge = charge;
		this.powered = powered;
		this.active = active;
	}

	@Override
	public void drawScreen(int mx, int my, float partial) {
		drawDefaultBackground();
		int x = width / 2 - 90, y = height / 2 - 55;
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.destabilizer.title"),
				width / 2,
				y,
				0xFF4040);
		drawString(
				fontRenderer,
				I18n.format("gui.hbmstfr.destabilizer.power", powered ? "ON" : "OFF"),
				x,
				y + 22,
				powered ? 0x66FF66 : 0xFF5555);
		drawString(
				fontRenderer,
				I18n.format(
						"gui.hbmstfr.destabilizer.stage",
						active ? "DIGAMMA ONLINE" : Integer.toString(stage)),
				x,
				y + 36,
				0xFFFFFF);
		bar(
				x,
				y + 54,
				compression,
				0xC01828,
				I18n.format("gui.hbmstfr.destabilizer.compression", compression * 100F));
		bar(
				x,
				y + 76,
				charge,
				0xE04050,
				I18n.format("gui.hbmstfr.destabilizer.charge", charge * 100F));
		drawString(fontRenderer, "OOO \"Dvijuha\" productions", 6, height - fontRenderer.FONT_HEIGHT - 6, 0x888888);
		super.drawScreen(mx, my, partial);
	}

	private void bar(int x, int y, float value, int color, String label) {
		value = Math.max(0, Math.min(1, value));
		drawRect(x, y, x + 180, y + 14, 0xFF202020);
		drawRect(x + 1, y + 1, x + 1 + (int) (178 * value), y + 13, 0xFF000000 | color);
		drawCenteredString(fontRenderer, label, x + 90, y + 3, 0xFFFFFF);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}

package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.StfrAutopilotPacket;
import fun.aloyenz.stfr.logic.StfrLogicPacket;

public final class GuiAutopilot extends GuiScreen {

	private final BlockPos pos;
	private final long threshold;
	private final int level;
	private GuiTextField field;

	public GuiAutopilot(BlockPos pos, long threshold, int level) {
		this.pos = pos;
		this.threshold = threshold;
		this.level = level;
	}

	public void initGui() {
		field = new GuiTextField(0, fontRenderer, width / 2 - 70, height / 2 - 8, 140, 20);
		field.setMaxStringLength(10);
		field.setText(Long.toString(threshold));
		buttonList.clear();
		if (level == 3)
			buttonList.add(
					new GuiButton(
							1,
							width / 2 - 70,
							height / 2 + 22,
							140,
							20,
							I18n.format("gui.hbmstfr.autopilot.apply")));
		if (level == 3)
			buttonList.add(
					new GuiButton(
							2,
							width / 2 - 70,
							height / 2 + 48,
							140,
							20,
							I18n.format("gui.hbmstfr.autopilot.force_shutdown")));
	}

	protected void actionPerformed(GuiButton button) {
		if (button.id == 2) {
			StfrLogicPacket.CHANNEL.sendToServer(StfrAutopilotPacket.forceShutdown(pos));
			return;
		}
		try {
			StfrLogicPacket.CHANNEL.sendToServer(
					new StfrAutopilotPacket(pos, Long.parseLong(field.getText())));
		} catch (NumberFormatException ignored) {
		}
	}

	protected void keyTyped(char c, int key) throws IOException {
		if (field == null || !field.textboxKeyTyped(c, key)) super.keyTyped(c, key);
	}

	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if (field != null) field.mouseClicked(x, y, button);
	}

	public void updateScreen() {
		if (field != null) field.updateCursorCounter();
	}

	public void drawScreen(int x, int y, float partial) {
		drawDefaultBackground();
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.autopilot.title", level),
				width / 2,
				height / 2 - 48,
				0x66FFFF);
		if (level == 3) {
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.autopilot.fuel"),
					width / 2,
					height / 2 - 24,
					0xFFFFFF);
			field.drawTextBox();
		} else
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.autopilot.no_config"),
					width / 2,
					height / 2 - 4,
					0xBBBBBB);
		super.drawScreen(x, y, partial);
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}

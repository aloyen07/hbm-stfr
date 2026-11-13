package fun.aloyenz.stfr.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.*;

/** Presentation selection is separate from the saved four-line text editor. */
public final class GuiMonitorDisplay extends GuiScreen {

	private final BlockPos pos;
	private StfrMonitorDisplay selected;

	public GuiMonitorDisplay(BlockPos pos, StfrMonitorDisplay selected) {
		this.pos = pos;
		this.selected = selected;
	}

	public void initGui() {
		buttonList.clear();
		for (StfrMonitorDisplay mode : StfrMonitorDisplay.values())
			buttonList.add(
					new GuiButton(
							mode.ordinal(),
							width / 2 - 130,
							height / 2 - 55 + mode.ordinal() * 26,
							260,
							22,
							(mode == selected ? "[X] " : "[ ] ")
									+ I18n.format(mode.translationKey())));
		buttonList.add(
				new GuiButton(
						10,
						width / 2 - 65,
						height / 2 + 63,
						130,
						20,
						I18n.format("gui.hbmstfr.monitor.done")));
	}

	public void drawScreen(int x, int y, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.monitor.display.title"),
				width / 2,
				height / 2 - 85,
				0xFFFFFF);
		super.drawScreen(x, y, partialTicks);
	}

	protected void actionPerformed(GuiButton button) {
		if (button.id == 10) {
			mc.displayGuiScreen(null);
			return;
		}
		selected = StfrMonitorDisplay.values()[button.id];
		StfrLogicPacket.CHANNEL.sendToServer(new StfrMonitorDisplayPacket(pos, selected));
		initGui();
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}

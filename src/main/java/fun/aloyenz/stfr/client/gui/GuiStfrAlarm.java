package fun.aloyenz.stfr.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.StfrAlarmSettingsPacket;
import fun.aloyenz.stfr.logic.StfrLogicPacket;

public final class GuiStfrAlarm extends GuiScreen {
	private final BlockPos pos;
	private boolean messages, voice, music;

	public GuiStfrAlarm(BlockPos pos, boolean messages, boolean voice, boolean music) {
		this.pos = pos;
		this.messages = messages;
		this.voice = voice;
		this.music = music;
	}

	@Override
	public void initGui() {
		buttonList.clear();
		for (int i = 0; i < 3; i++) buttonList.add(new GuiButton(i, width / 2 - 75,
				height / 2 - 20 + i * 26, 150, 20, ""));
		refresh();
	}

	private void refresh() {
		boolean[] values = {messages, voice, music};
		String[] keys = {"messages", "voice", "music"};
		for (int i = 0; i < buttonList.size(); i++)
			buttonList.get(i).displayString = I18n.format("gui.hbmstfr.alarm." + keys[i])
					+ ": " + I18n.format("gui.hbmstfr.alarm." + (values[i] ? "on" : "off"));
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) messages = !messages;
		if (button.id == 1) voice = !voice;
		if (button.id == 2) music = !music;
		refresh();
		StfrLogicPacket.CHANNEL.sendToServer(
				new StfrAlarmSettingsPacket(pos, messages, voice, music));
	}

	@Override
	public void drawScreen(int x, int y, float partial) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, I18n.format("gui.hbmstfr.alarm.title"),
				width / 2, height / 2 - 48, 0x66FFFF);
		super.drawScreen(x, y, partial);
	}

	@Override
	public boolean doesGuiPauseGame() { return false; }
}

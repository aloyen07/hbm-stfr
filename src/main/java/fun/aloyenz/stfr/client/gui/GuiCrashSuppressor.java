package fun.aloyenz.stfr.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrSuppressorPacket;

public final class GuiCrashSuppressor extends GuiScreen {

	private final BlockPos pos;
	private int mask;
	private static final String[] TYPES = {
		"structure", "shockwave", "nuclear", "high_energy", "fire", "player_damage"
	};

	public GuiCrashSuppressor(BlockPos pos, int mask) {
		this.pos = pos;
		this.mask = mask;
	}

	public BlockPos pos() {
		return pos;
	}

	public void update(int mask) {
		this.mask = mask;
		initGui();
	}

	public void initGui() {
		buttonList.clear();
		for (int i = 0; i < TYPES.length; i++)
			buttonList.add(
					new GuiButton(
							i,
							width / 2 - 130,
							height / 2 - 64 + i * 24,
							260,
							20,
							I18n.format("gui.hbmstfr.suppressor." + TYPES[i])
									+ ": "
									+ I18n.format(
											(mask & (1 << i)) != 0
													? "gui.hbmstfr.suppressor.blocked"
													: "gui.hbmstfr.suppressor.allowed")));
		for (GuiButton button : buttonList)
			button.enabled = mc.player != null && mc.player.capabilities.isCreativeMode;
	}

	protected void actionPerformed(GuiButton button) {
		if (button.enabled)
			StfrLogicPacket.CHANNEL.sendToServer(
					new StfrSuppressorPacket(pos, mask ^ (1 << button.id)));
	}

	public void drawScreen(int x, int y, float partial) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, "Crash Suppressor", width / 2, height / 2 - 88, 0x66FFFF);
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.suppressor.coupled"),
				width / 2,
				height / 2 + 90,
				0xBBBBBB);
		super.drawScreen(x, y, partial);
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}

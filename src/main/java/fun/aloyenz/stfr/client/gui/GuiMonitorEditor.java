package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrMonitorText;
import fun.aloyenz.stfr.logic.StfrMonitorTextPacket;

/** Familiar four-line sign editor; placeholders and colours cost no visible characters. */
public final class GuiMonitorEditor extends GuiScreen {

	private final BlockPos pos;
	private final String[] saved;
	private final GuiTextField[] fields = new GuiTextField[4];
	private int active;
	private int helpPage;
	private static final String[][] HELP = {
		{"$STATE $TICKS $LOCK $CORE", "$KEY $SSTB $BRK"},
		{"$FUEL $FTYPE $FLEFT", "$DEUT $HE4MB $PO210 $YARONITE"},
		{"$RPM $TRPM $RVEL $CROT", "$CTEMP $CPRESS $SHIELD $RXHE"},
		{"$TEMP $PRESS $WATER $STEAM", "$LUBE $PROD $POWER"},
		{"$SSTRESS $PSTRESS $SPSTRESS", "$RSTRESS $MSSR $SSTATUS"},
		{"$RR $HTR $COOL $RAD $CAP", "$REACT $GEN $UPTIME"},
		{"$RF $PERM $LASER $VENT", "$TURB $REFUEL $SEAL $ERR $MISS $AIR"}
	};

	public GuiMonitorEditor(BlockPos pos, String[] saved) {
		this.pos = pos;
		this.saved = saved.clone();
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		buttonList.clear();
		buttonList.add(
				new GuiButton(
						0,
						width / 2 - 110,
						height / 2 + 75,
						100,
						20,
						I18n.format("gui.hbmstfr.monitor.done")));
		buttonList.add(
				new GuiButton(
						1,
						width / 2 + 10,
						height / 2 + 75,
						100,
						20,
						I18n.format("gui.hbmstfr.monitor.variables")));
		for (int i = 0; i < 4; i++) {
			fields[i] =
					new GuiTextField(
							i, fontRenderer, width / 2 - 135, height / 2 - 49 + i * 22, 270, 20);
			fields[i].setMaxStringLength(StfrMonitorText.RAW_LIMIT);
			fields[i].setText(saved[i]);
		}
		focus(0);
	}

	private void focus(int index) {
		active = (index + 4) % 4;
		for (int i = 0; i < 4; i++) fields[i].setFocused(i == active);
	}

	@Override
	public void updateScreen() {
		for (GuiTextField field : fields) field.updateCursorCounter();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.monitor.title"),
				width / 2,
				height / 2 - 86,
				0xFFFFFF);
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.monitor.help"),
				width / 2,
				height / 2 - 70,
				0xA0D4DF);
		drawCenteredString(fontRenderer, HELP[helpPage][0], width / 2, height / 2 + 42, 0xA0D4DF);
		drawCenteredString(fontRenderer, HELP[helpPage][1], width / 2, height / 2 + 53, 0xA0D4DF);
		boolean valid = true;
		for (GuiTextField field : fields) {
			field.drawTextBox();
			if (!StfrMonitorText.valid(field.getText())) valid = false;
		}
		buttonList.get(0).enabled = valid;
		if (!valid)
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.monitor.limit"),
					width / 2,
					height / 2 + 60,
					0xFF5555);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		for (int i = 0; i < 4; i++) {
			fields[i].mouseClicked(mouseX, mouseY, mouseButton);
			if (fields[i].isFocused()) focus(i);
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_TAB
				|| keyCode == Keyboard.KEY_DOWN
				|| keyCode == Keyboard.KEY_RETURN) {
			focus(active + 1);
		} else if (keyCode == Keyboard.KEY_UP) {
			focus(active - 1);
		} else if (keyCode == Keyboard.KEY_ESCAPE) {
			mc.displayGuiScreen(null);
		} else {
			fields[active].textboxKeyTyped(typedChar, keyCode);
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 1) {
			helpPage = (helpPage + 1) % HELP.length;
			return;
		}
		if (button.id != 0 || !button.enabled) return;
		String[] lines = new String[4];
		for (int i = 0; i < 4; i++) lines[i] = fields[i].getText();
		StfrLogicPacket.CHANNEL.sendToServer(new StfrMonitorTextPacket(pos, lines));
		mc.displayGuiScreen(null);
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}

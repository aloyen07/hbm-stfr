package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import java.util.Arrays;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import fun.aloyenz.stfr.inventory.ContainerSolidInjector;
import fun.aloyenz.stfr.logic.StfrInjectorConfigPacket;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;

public final class GuiSolidInjector extends GuiContainer {

	private final ContainerSolidInjector container;
	private GuiTextField dose, interval;
	private GuiButton redstone, apply;
	private int mode, lastDose = -1, lastInterval = -1, lastMode = -1;

	public GuiSolidInjector(ContainerSolidInjector container) {
		super(container);
		this.container = container;
		xSize = 176;
		ySize = 220;
	}

	private String modeLabel() {
		return I18n.format("gui.hbmstfr.injector.redstone." + mode);
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		dose = new GuiTextField(0, fontRenderer, guiLeft + 90, guiTop + 39, 77, 16);
		interval = new GuiTextField(1, fontRenderer, guiLeft + 90, guiTop + 69, 77, 16);
		dose.setMaxStringLength(5);
		interval.setMaxStringLength(5);
		dose.setText(Integer.toString(container.dose));
		interval.setText(Integer.toString(container.interval));
		mode = container.mode;
		redstone = new GuiStfrSecurityButton(0, guiLeft + 8, guiTop + 96, 160, 18, modeLabel());
		apply =
				new GuiStfrSecurityButton(
						1,
						guiLeft + 104,
						guiTop + 116,
						64,
						18,
						I18n.format("gui.hbmstfr.injector.apply"));
		buttonList.add(redstone);
		buttonList.add(apply);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		dose.updateCursorCounter();
		interval.updateCursorCounter();
		if (container.dose != lastDose) {
			dose.setText(Integer.toString(container.dose));
			lastDose = container.dose;
		}
		if (container.interval != lastInterval) {
			interval.setText(Integer.toString(container.interval));
			lastInterval = container.interval;
		}
		if (container.mode != lastMode) {
			mode = container.mode;
			lastMode = container.mode;
			redstone.displayString = modeLabel();
		}
		apply.enabled = valid();
	}

	private boolean valid() {
		try {
			int amount = Integer.parseInt(dose.getText()),
					ticks = Integer.parseInt(interval.getText());
			return amount >= 1
					&& amount <= TileEntitySolidInjector.CAPACITY
					&& ticks >= 1
					&& ticks <= TileEntitySolidInjector.MAX_INTERVAL;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			mode = (mode + 1) % 3;
			redstone.displayString = modeLabel();
		}
		if (button.id == 1 && valid())
			StfrLogicPacket.CHANNEL.sendToServer(
					new StfrInjectorConfigPacket(
							container.windowId,
							Integer.parseInt(dose.getText()),
							Integer.parseInt(interval.getText()),
							mode));
	}

	@Override
	protected void keyTyped(char c, int key) throws IOException {
		if (key == Keyboard.KEY_ESCAPE) {
			super.keyTyped(c, key);
			return;
		}
		if (key == Keyboard.KEY_TAB) {
			boolean focused = dose.isFocused();
			dose.setFocused(!focused);
			interval.setFocused(focused);
			return;
		}
		if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
			actionPerformed(apply);
			return;
		}
		if (dose.isFocused() || interval.isFocused()) {
			if (Character.isDigit(c) || c < 32) {
				dose.textboxKeyTyped(c, key);
				interval.textboxKeyTyped(c, key);
			}
			return;
		}
		super.keyTyped(c, key);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		dose.mouseClicked(x, y, button);
		interval.mouseClicked(x, y, button);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	private void panel(int x, int y, int w, int h, boolean inset) {
		drawRect(x, y, x + w, y + h, 0xFF171A1B);
		drawRect(x + 1, y + 1, x + w - 1, y + h - 1, inset ? 0xFF383D3F : 0xFF80888B);
		drawRect(x + 2, y + 2, x + w - 2, y + h - 2, inset ? 0xFF141B1E : 0xFF5D6568);
		if (!inset) {
			drawRect(x + 2, y + 2, x + w - 2, y + 3, 0xFF9AA1A3);
			drawRect(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFF42494D);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		panel(guiLeft, guiTop, xSize, ySize, false);
		panel(guiLeft + 5, guiTop + 19, 166, 74, true);
		panel(guiLeft + 25, guiTop + 41, 18, 18, true);
		panel(guiLeft + 55, guiTop + 30, 20, 57, true);
		int height =
				53
						* Math.min(TileEntitySolidInjector.CAPACITY, container.polonium)
						/ TileEntitySolidInjector.CAPACITY;
		if (height > 0) {
			drawRect(guiLeft + 57, guiTop + 85 - height, guiLeft + 73, guiTop + 85, 0xFF6E5040);
			drawRect(guiLeft + 58, guiTop + 85 - height, guiLeft + 60, guiTop + 85, 0xFFA48A6D);
			for (int y = guiTop + 85 - height + 3; y < guiTop + 85; y += 6)
				drawRect(guiLeft + 61, y, guiLeft + 71, y + 1, 0xFF594239);
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				panel(guiLeft + 7 + col * 18, guiTop + 137 + row * 18, 18, 18, true);
		for (int col = 0; col < 9; col++) panel(guiLeft + 7 + col * 18, guiTop + 195, 18, 18, true);
		for (int x : new int[] {3, 169})
			for (int y : new int[] {3, 213}) {
				drawRect(guiLeft + x, guiTop + y, guiLeft + x + 4, guiTop + y + 4, 0xFF242A2D);
				drawRect(
						guiLeft + x + 1,
						guiTop + y + 1,
						guiLeft + x + 3,
						guiTop + y + 2,
						0xFFAFB4B5);
			}
		dose.drawTextBox();
		interval.drawTextBox();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString(I18n.format("gui.hbmstfr.injector.title"), 8, 6, 0xE1E4E4);
		fontRenderer.drawString("Po-210", 13, 23, 0xC5B395);
		fontRenderer.drawString(I18n.format("gui.hbmstfr.injector.dose"), 83, 27, 0xC7CCCE);
		fontRenderer.drawString(I18n.format("gui.hbmstfr.injector.interval"), 83, 57, 0xC7CCCE);
		fontRenderer.drawString(I18n.format("container.inventory"), 8, 126, 0xD2D6D7);
	}

	@Override
	public void drawScreen(int x, int y, float partial) {
		drawDefaultBackground();
		super.drawScreen(x, y, partial);
		renderHoveredToolTip(x, y);
		if (x >= guiLeft + 55 && x < guiLeft + 75 && y >= guiTop + 30 && y < guiTop + 87)
			drawHoveringText(
					Arrays.asList("Polonium-210", container.polonium + " / 16000 mB"), x, y);
		if (x >= guiLeft + 90 && x < guiLeft + 167 && y >= guiTop + 39 && y < guiTop + 55)
			drawHoveringText(I18n.format("gui.hbmstfr.injector.dose_range"), x, y);
		if (x >= guiLeft + 90 && x < guiLeft + 167 && y >= guiTop + 69 && y < guiTop + 85)
			drawHoveringText(I18n.format("gui.hbmstfr.injector.interval_range"), x, y);
	}
}

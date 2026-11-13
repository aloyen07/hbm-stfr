package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.security.*;

public final class GuiStfrSecurity extends GuiContainer {

	private final ContainerStfrSecurity access;
	private GuiTextField revoke;

	public GuiStfrSecurity(ContainerStfrSecurity container) {
		super(container);
		access = container;
		xSize = 176;
		ySize = 260;
	}

	@Override
	public void initGui() {
		super.initGui();
		buttonList.clear();
		revoke = new GuiTextField(0, fontRenderer, guiLeft + 8, guiTop + 140, 119, 18);
		revoke.setMaxStringLength(36);
		if (access.detonator) {
			buttonList.add(
					new GuiStfrSecurityButton(
							10,
							guiLeft + 8,
							guiTop + 96,
							160,
							20,
							I18n.format("gui.hbmstfr.security.start")));
			buttonList.add(
					new GuiStfrSecurityButton(
							11,
							guiLeft + 8,
							guiTop + 124,
							160,
							20,
							I18n.format("gui.hbmstfr.security.abort")));
		} else {
			buttonList.add(new GuiStfrSecurityButton(0, guiLeft + 8, guiTop + 56, 160, 18, ""));
			String[] rights = {"control", "connect", "admin", "detonate"};
			for (int i = 0; i < 4; i++)
				buttonList.add(
						new GuiStfrSecurityButton(
								i + 1,
								guiLeft + 8 + (i % 2) * 82,
								guiTop + 78 + (i / 2) * 20,
								78,
								18,
								I18n.format("gui.hbmstfr.security." + rights[i])));
			buttonList.add(
					new GuiStfrSecurityButton(
							5,
							guiLeft + 8,
							guiTop + 119,
							160,
							18,
							I18n.format("gui.hbmstfr.security.revoke")));
			buttonList.add(
					new GuiStfrSecurityButton(6, guiLeft + 130, guiTop + 140, 38, 18, "UUID"));
		}
	}

	@Override
	public void drawScreen(int x, int y, float partial) {
		drawDefaultBackground();
		for (GuiButton b : buttonList) {
			b.enabled = access.actorValid != 0 && (b.id != 11 || access.abortable != 0);
			if (b.id == 0)
				b.displayString =
						I18n.format(
								"gui.hbmstfr.security.protection",
								I18n.format(
										access.enabled != 0
												? "gui.hbmstfr.security.on"
												: "gui.hbmstfr.security.off"));
			if (b.id >= 1 && b.id <= 4) {
				String[] keys = {"control", "connect", "admin", "detonate"};
				b.displayString =
						((access.targetRights & (1 << (b.id - 1))) != 0 ? "✓ " : "○ ")
								+ I18n.format("gui.hbmstfr.security." + keys[b.id - 1]);
			}
		}
		super.drawScreen(x, y, partial);
		if (!access.detonator) revoke.drawTextBox();
		renderHoveredToolTip(x, y);
	}

	protected void drawGuiContainerBackgroundLayer(float p, int mx, int my) {
		GlStateManager.color(1F, 1F, 1F, 1F);
		mc.getTextureManager()
				.bindTexture(
						new ResourceLocation(
								"hbmstfr",
								access.detonator
										? "textures/gui/stfr_detonator.png"
										: "textures/gui/stfr_privileges.png"));
		drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, xSize, ySize);
	}

	protected void drawGuiContainerForegroundLayer(int x, int y) {
		fontRenderer.drawString(
				I18n.format(
						access.detonator
								? "gui.hbmstfr.security.detonator"
								: "gui.hbmstfr.security.title"),
				8,
				6,
				0xFFFFFF);
		fontRenderer.drawString(I18n.format("gui.hbmstfr.security.actor"), 8, 21, 0xB0DEE8);
		if (!access.detonator)
			fontRenderer.drawString(I18n.format("gui.hbmstfr.security.target"), 97, 21, 0xB0DEE8);
		if (access.result > 0)
			fontRenderer.drawString(
					fontRenderer.trimStringToWidth(
							I18n.format("gui.hbmstfr.security.result." + access.result), 160),
					8,
					164,
					access.result == 3 ? 0x80FFAA : 0xFF9090);
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.enabled) {
			UUID id = null;
			if (button.id == 6) {
				try {
					id = UUID.fromString(revoke.getText());
				} catch (IllegalArgumentException ex) {
					return;
				}
			}
			StfrLogicPacket.CHANNEL.sendToServer(
					new StfrSecurityActionPacket(inventorySlots.windowId, button.id, id));
		}
	}

	protected void keyTyped(char c, int key) throws IOException {
		if (!access.detonator && revoke.textboxKeyTyped(c, key)) return;
		super.keyTyped(c, key);
	}

	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if (!access.detonator) revoke.mouseClicked(x, y, button);
	}

	public void updateScreen() {
		super.updateScreen();
		revoke.updateCursorCounter();
	}
}

package fun.aloyenz.stfr.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Exact 18/20px button rows; the bottom edge is never cropped. */
public final class GuiStfrSecurityButton extends GuiButton {

	private static final ResourceLocation WIDGETS =
			new ResourceLocation("hbmstfr", "textures/gui/stfr_security_widgets.png");
	private final boolean danger;

	public GuiStfrSecurityButton(int id, int x, int y, int width, int height, String text) {
		super(id, x, y, width, height, text);
		if (height != 18 && height != 20) {
			throw new IllegalArgumentException("STFR security buttons must be 18 or 20px high");
		}
		if (width < 4 || width > 160) {
			throw new IllegalArgumentException("STFR security button width must be 4..160px");
		}
		danger = id == 10;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) return;
		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		int state = getHoverState(hovered);
		int row = (height == 18 ? 0 : danger ? 54 : 114) + state * height;
		mc.getTextureManager().bindTexture(WIDGETS);
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		int left = width / 2;
		int right = width - left;
		// Preserve both end caps, including for odd widths. Each row contains
		// the complete frame at the actual height of this button.
		drawTexturedModalRect(x, y, 0, row, left, height);
		drawTexturedModalRect(x + left, y, 160 - right, row, right, height);
		mouseDragged(mc, mouseX, mouseY);
		int textColor = enabled ? 0xE5EBE7 : 0x9BA7AA;
		drawCenteredString(
				mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, textColor);
	}
}

package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** STFR pages in the familiar HBM guide book spread and cover. */
public final class GuiOperatorBookMk2 extends GuiScreen {

	private static final ResourceLocation OPEN =
			new ResourceLocation("hbm", "textures/gui/book/book.png");
	private static final ResourceLocation COVER =
			new ResourceLocation("hbm", "textures/gui/book/book_cover.png");
	private static final int WIDTH = 272;
	private static final int HEIGHT = 182;
	private static final int INK = 0x342D24;
	private static final int HEADING = 0x683820;
	private static final float TEXT_SCALE = 0.78F;

	private final boolean secondEdition;
	private List<StfrOperatorBookPages.Page> pages;
	private boolean russian;

	public GuiOperatorBookMk2(boolean secondEdition) {
		this.secondEdition = secondEdition;
	}
	private int spread = -1;
	private int left;
	private int top;

	@Override
	public void initGui() {
		russian = mc.getLanguageManager().getCurrentLanguage().getLanguageCode().toLowerCase().startsWith("ru");
		pages = StfrOperatorBookPages.all(secondEdition, russian);
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		mc.getTextureManager().bindTexture(spread < 0 ? COVER : OPEN);
		GlStateManager.color(1F, 1F, 1F, 1F);
		drawModalRectWithCustomSizedTexture(left, top, 0, 0, WIDTH, HEIGHT, 512, 512);
		if (spread < 0) {
			drawCenteredString(fontRenderer, "STFR", left + WIDTH / 2, top + 50, 0xD7DDE0);
			drawCenteredString(fontRenderer, russian ? "РУКОВОДСТВО" : "OPERATOR", left + WIDTH / 2, top + 69, 0xB4C8D0);
			drawCenteredString(fontRenderer, russian ? "ОПЕРАТОРА" : "MANUAL", left + WIDTH / 2, top + 82, 0xB4C8D0);
			drawCenteredString(fontRenderer, russian ? (secondEdition ? "ВТОРОЕ ИЗДАНИЕ" : "ПЕРВОЕ ИЗДАНИЕ")
						: (secondEdition ? "SECOND EDITION" : "FIRST EDITION"), left + WIDTH / 2, top + 111, 0xDEB56E);
		} else {
			int first = spread * 2;
			drawPage(first, left + 18);
			if (first + 1 < pages.size()) drawPage(first + 1, left + 151);
		}
		mc.getTextureManager().bindTexture(OPEN);
		if (spread >= 0) drawArrow(left + 24, top + 155, true, mouseX, mouseY);
		if (spread < (pages.size() - 1) / 2)
			drawArrow(left + 230, top + 155, false, mouseX, mouseY);
	}

	private void drawPage(int index, int x) {
		StfrOperatorBookPages.Page page = pages.get(index);
		int y = top + 14;
		y = drawWrapped(page.title, x, y, 108, 0.88F, HEADING);
		y += 4;
		for (String paragraph : page.body.split("\\n\\s*\\n")) {
			if (!paragraph.trim().isEmpty()) {
				y = drawWrapped(paragraph.replace('\n', ' ').trim(), x, y, 108, TEXT_SCALE, INK);
				y += 4;
			}
		}
		drawCenteredString(fontRenderer, Integer.toString(index + 1), x + 54, top + 158, 0x766956);
	}

	private int drawWrapped(String value, int x, int y, int width, float scale, int color) {
		List<String> lines = fontRenderer.listFormattedStringToWidth(value, (int) (width / scale));
		for (String line : lines) {
			GlStateManager.pushMatrix();
			GlStateManager.scale(scale, scale, 1F);
			fontRenderer.drawString(line, (int) (x / scale), (int) (y / scale), color);
			GlStateManager.popMatrix();
			y += 8;
		}
		return y;
	}

	private void drawArrow(int x, int y, boolean previous, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 10;
		drawModalRectWithCustomSizedTexture(
				x, y, hover ? 26 : 3, previous ? 207 : 194, 18, 10, 512, 512);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		super.mouseClicked(mouseX, mouseY, button);
		if (button != 0 || mouseY < top + 155 || mouseY >= top + 165) return;
		if (spread >= 0 && mouseX >= left + 24 && mouseX < left + 42) spread--;
		else if (spread < (pages.size() - 1) / 2
				&& mouseX >= left + 230 && mouseX < left + 248) spread++;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_PRIOR) {
			spread = Math.max(-1, spread - 1);
			return;
		}
		if (keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_NEXT) {
			spread = Math.min((pages.size() - 1) / 2, spread + 1);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}

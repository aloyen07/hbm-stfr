package fun.aloyenz.stfr.client.render;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMonitor;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.logic.StfrMonitorInterference;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Original sign skin and four-line, full-bright STFR readout. */
public final class RenderLogicMonitor extends TileEntitySpecialRenderer<TileEntityLogicDevice> {

	private static final ResourceLocation SIGN =
			new ResourceLocation("hbmstfr", "textures/entity/logic_monitor_sign.png");
	private final ModelSign sign = new ModelSign();

	@Override
	public void render(
			TileEntityLogicDevice tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		if (tile.kind() != BlockLogicDevice.Kind.MONITOR) return;
		IBlockState state = tile.getWorld().getBlockState(tile.getPos());
		if (!(state.getBlock() instanceof BlockLogicMonitor)) return;
		EnumFacing facing = state.getValue(BlockLogicMonitor.FACING);
		boolean wall = state.getValue(BlockLogicMonitor.WALL);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
		if (wall) GlStateManager.translate(0, -0.3125, -0.4375);
		float oldX = OpenGlHelper.lastBrightnessX;
		float oldY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		GlStateManager.disableLighting();
		bindTexture(SIGN);
		sign.signStick.showModel = !wall;
		GlStateManager.pushMatrix();
		GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
		sign.renderSign();
		GlStateManager.popMatrix();
		GlStateManager.translate(0, 0.33333334, 0.046666667);

		FontRenderer font = getFontRenderer();
		boolean oldUnicode = font.getUnicodeFlag();
		font.setUnicodeFlag(true);
		try {
			if (tile.monitorDisplay() == StfrMonitorDisplay.REACTOR && tile.catStage() > 0) {
				StfrCatMonitorRenderer.render(font, tile.catStage());
			} else if (StfrMonitorInterference.active(
					tile.getPos().toLong(),
					tile.getWorld().getTotalWorldTime(),
					tile.monitorUnstable())) {
				interference(tile);
			} else if (!tile.monitorConnected()) {
				redStripe(-0.165, -0.13);
				redStripe(0.13, 0.165);
				drawLine(font, "§l" + I18n.format("gui.hbmstfr.monitor.no_signal.first"), -8, 0xFF5555, 0.012F);
				drawLine(font, "§l" + I18n.format("gui.hbmstfr.monitor.no_signal.second"), 3, 0xFF5555, 0.012F);
			} else if (tile.monitorDisplay() != StfrMonitorDisplay.TEXT
					|| tile.monitorGraphics().getBoolean("selfPending")
					|| "SELF_DESTRUCT".equals(tile.monitorGraphics().getString("state"))) {
				StfrMonitorGraphics.render(font, tile);
			} else {
				boolean redPhase = (tile.getWorld().getTotalWorldTime() / 4L & 1L) == 0L;
				for (int i = 0; i < 4; i++) {
					int alarm = tile.monitorColor(i);
					int color = alarm == 0xFF5555 && !redPhase ? 0xF4F4F4 : alarm;
					drawLine(font, tile.monitorLine(i), -18 + i * 10, color, 0.008F);
				}
			}
		} finally {
			font.setUnicodeFlag(oldUnicode);
		}
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.popMatrix();
	}

	private static void drawLine(
			FontRenderer font, String value, int y, int color, float maxScale) {
		if (value.isEmpty()) return;
		// Tokens do not consume editor space. Their expanded values are
		// clipped to the sign instead of shrinking the whole line.
		String clipped = font.trimStringToWidth(value, 105);
		int width = Math.max(1, font.getStringWidth(clipped));
		float scale = maxScale;
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, -scale, scale);
		font.drawString(clipped, -width / 2, y, color);
		GlStateManager.popMatrix();
	}

	private static void redStripe(double bottom, double top) {
		GlStateManager.disableTexture2D();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(-0.44, bottom, 0.001).color(180, 18, 25, 255).endVertex();
		buffer.pos(0.44, bottom, 0.001).color(180, 18, 25, 255).endVertex();
		buffer.pos(0.44, top, 0.001).color(180, 18, 25, 255).endVertex();
		buffer.pos(-0.44, top, 0.001).color(180, 18, 25, 255).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
	}

	private static void interference(TileEntityLogicDevice tile) {
		Random noise = new Random(tile.getPos().toLong() ^ tile.getWorld().getTotalWorldTime());
		GlStateManager.disableTexture2D();
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (int row = 0; row < 16; row++)
			for (int col = 0; col < 24; col++) {
				int value = 40 + noise.nextInt(200);
				double x = -.44 + col * .88 / 24, y = -.165 + row * .33 / 16;
				b.pos(x, y, .003).color(value, value, value, 255).endVertex();
				b.pos(x + .88 / 24, y, .003).color(value, value, value, 255).endVertex();
				b.pos(x + .88 / 24, y + .33 / 16, .003).color(value, value, value, 255).endVertex();
				b.pos(x, y + .33 / 16, .003).color(value, value, value, 255).endVertex();
			}
		Tessellator.getInstance().draw();
		GlStateManager.enableTexture2D();
	}
}

package fun.aloyenz.stfr.client.render;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Pixel art stays on the physical screen, with normal world depth testing. */
final class StfrCatMonitorRenderer {
    private static final ResourceLocation[] IMAGES = {
        new ResourceLocation("hbmstfr", "textures/meme/cat_1.png"),
        new ResourceLocation("hbmstfr", "textures/meme/cat_2.png"),
        new ResourceLocation("hbmstfr", "textures/meme/cat_3.png")
    };
    static void render(FontRenderer font, int stage) {
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-.44, .164, .003);
        GlStateManager.scale(.004, -.004, .004);
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        try {
            GlStateManager.disableTexture2D();
            GlStateManager.color(stage == 2 ? .25F : 0, 0, 0, 1);
            quad(0, 0, 220, 82, 0, 1);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            Minecraft.getMinecraft().getTextureManager().bindTexture(IMAGES[Math.max(0, Math.min(2, stage - 1))]);
            if (stage == 3) quad(0, 0, 220, 82, .2515, .7485);
            else {
                quad(55.333, 0, 109.334, 82, 0, 1);
                GlStateManager.disableTexture2D();
                GlStateManager.color(0, 0, 0, .85F);
                quad(0, 61, 220, 21, 0, 1);
                GlStateManager.enableTexture2D();
                GlStateManager.color(1, 1, 1, 1);
                List<String> lines = font.listFormattedStringToWidth(
                    I18n.format(stage == 1 ? "gui.hbmstfr.cat.warning" : "gui.hbmstfr.cat.threat"), 210);
                for (int i=0; i<Math.min(2, lines.size()); i++) {
                    String line=lines.get(i);
                    font.drawString(line, (220-font.getStringWidth(line))/2, 63+i*9, stage==1 ? 0xFFFFFF : 0xFF5555);
                }
            }
        } finally {
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.depthMask(depthWrite);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
    private static void quad(double x,double y,double width,double height,double v0,double v1) {
        BufferBuilder b=Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.pos(x,y,0).tex(0,v0).endVertex();
        b.pos(x+width,y,0).tex(1,v0).endVertex();
        b.pos(x+width,y+height,0).tex(1,v1).endVertex();
        b.pos(x,y+height,0).tex(0,v1).endVertex();
        Tessellator.getInstance().draw();
    }
}

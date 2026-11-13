package fun.aloyenz.stfr.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.entity.EntityStfrDigammaDagger;

public final class RenderDigammaDagger extends Render<EntityStfrDigammaDagger> {

	public RenderDigammaDagger(RenderManager manager) {
		super(manager);
	}

	@Override
	public void doRender(
			EntityStfrDigammaDagger e, double x, double y, double z, float yaw, float partial) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI * .5, beta = (i + 1) * Math.PI * .5;
			b.pos(0, -7, 0).color(1F, .04F, .02F, .95F).endVertex();
			b.pos(Math.cos(a) * 2, 5, Math.sin(a) * 2).color(1F, .15F, .04F, .65F).endVertex();
			b.pos(Math.cos(beta) * 2, 5, Math.sin(beta) * 2)
					.color(1F, .15F, .04F, .65F)
					.endVertex();
		}
		Tessellator.getInstance().draw();
		GlStateManager.glLineWidth(10F);
		b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		b.pos(0, -7, 0).color(1F, 1F, 1F, .9F).endVertex();
		b.pos(0, 96, 0).color(1F, 0F, 0F, 0F).endVertex();
		Tessellator.getInstance().draw();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityStfrDigammaDagger entity) {
		return null;
	}
}

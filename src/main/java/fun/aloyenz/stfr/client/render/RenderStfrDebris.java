package fun.aloyenz.stfr.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderFallingBlock;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import fun.aloyenz.stfr.entity.EntityStfrDebris;

/** Rotate around each fragment's center while retaining its world block model and textures. */
public final class RenderStfrDebris extends RenderFallingBlock {
	public RenderStfrDebris(RenderManager manager) { super(manager); }

	@Override
	public void doRender(EntityFallingBlock entity, double x, double y, double z,
			float yaw, float partialTicks) {
		IBlockState state = entity.getBlock();
		if (state == null) return;
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		try {
			GlStateManager.disableLighting();
			GlStateManager.color(1, 1, 1, 1);
			GlStateManager.translate(x, y + .5, z);
			GlStateManager.rotate(((EntityStfrDebris) entity).spin(partialTicks), .4F, 1F, .25F);
			BlockPos modelPos = new BlockPos(entity);
			GlStateManager.translate(-modelPos.getX() - .5, -modelPos.getY() - .5,
					-modelPos.getZ() - .5);
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			buffer.begin(7, DefaultVertexFormats.BLOCK);
			Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
					.renderModel(entity.world,
							Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state),
							state, modelPos, buffer, false, MathHelper.getPositionRandom(entity.getOrigin()));
			tessellator.draw();
		} finally {
			GlStateManager.enableLighting();
			GlStateManager.popMatrix();
		}
	}
}

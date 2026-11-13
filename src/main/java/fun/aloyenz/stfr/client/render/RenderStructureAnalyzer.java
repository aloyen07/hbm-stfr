package fun.aloyenz.stfr.client.render;

import java.nio.ByteOrder;
import java.util.List;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

/** Temporary full-structure hologram. */
public final class RenderStructureAnalyzer
		extends TileEntitySpecialRenderer<TileEntityStructureAnalyzer> {
	private static final double EMPTY_SCALE = 0.86D;
	private static final double OCCUPIED_SCALE = 1.02D;
	private static final int GHOST_ALPHA = 190;

	@Override
	public void render(
			TileEntityStructureAnalyzer tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		if (tile.getWorld() == null || !tile.isProjectionEnabled()) return;
		BlockPos origin = tile.getOrigin();
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(
				GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.depthMask(false);
		GlStateManager.enablePolygonOffset();
		GlStateManager.doPolygonOffset(-1F, -10F);
		GlStateManager.color(1F, 1F, 1F, 1F);
		Minecraft mc = Minecraft.getMinecraft();
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (tile.hasRotor(cell.localPos)) continue;
			BlockPos target = origin.add(cell.localPos);
			if (!tile.getWorld().isBlockLoaded(target)) continue;
			IBlockState actual = tile.getWorld().getBlockState(target);
			if (StfrStructureValidator.matchesCell(
					cell, tile.getRole(cell.localPos), actual, tile.getWorld(), target, tile))
				continue;
			GlStateManager.pushMatrix();
			// A ghost inside an incorrect solid block is entirely hidden by depth testing.
			// Place its textured shell just outside that block instead.
			double scale = actual.getMaterial().isReplaceable()
					? EMPTY_SCALE : OCCUPIED_SCALE;
			double inset = (1D - scale) * 0.5D;
			GlStateManager.translate(
					x + target.getX() - tile.getPos().getX() + inset,
					y + target.getY() - tile.getPos().getY() + inset,
					z + target.getZ() - tile.getPos().getZ() + inset);
			GlStateManager.scale(scale, scale, scale);
			IBlockState reference = StfrStructureValidator.referenceState(cell, tile, actual);
			IBlockState renderState =
					reference.getBlock() instanceof BlockStairs
							? reference.getActualState(tile.getWorld(), target)
							: reference;
			IBakedModel model = dispatcher.getModelForState(renderState);
			renderModel(mc, model, renderState, tile, target);
			GlStateManager.popMatrix();
		}
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.doPolygonOffset(0F, 0F);
		GlStateManager.disablePolygonOffset();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	private static void renderModel(
			Minecraft mc,
			IBakedModel model,
			IBlockState state,
			TileEntityStructureAnalyzer tile,
			BlockPos pos) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
		for (EnumFacing face : EnumFacing.values())
			renderQuads(mc, buffer, model.getQuads(state, face, 0L), state, tile, pos);
		renderQuads(mc, buffer, model.getQuads(state, null, 0L), state, tile, pos);
		tessellator.draw();
	}

	private static void renderQuads(
			Minecraft mc,
			BufferBuilder buffer,
			List<BakedQuad> quads,
			IBlockState state,
			TileEntityStructureAnalyzer tile,
			BlockPos pos) {
		for (BakedQuad quad : quads) {
			int tint = quad.hasTintIndex()
					? mc.getBlockColors().colorMultiplier(state, tile.getWorld(), pos, quad.getTintIndex())
					: 0xFFFFFF;
			float shade = !quad.shouldApplyDiffuseLighting() ? 1F
					: quad.getFace() == EnumFacing.DOWN ? 0.65F
					: quad.getFace() == EnumFacing.UP ? 1F : 0.82F;
			int[] vertexData = quad.getVertexData().clone();
			int stride = vertexData.length / 4;
			int colorOffset = DefaultVertexFormats.ITEM.getColorOffset() / 4;
			for (int vertex = 0; vertex < 4; vertex++) {
				int index = vertex * stride + colorOffset;
				int packed = vertexData[index];
				int red, green, blue;
				if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
					red = packed & 255;
					green = packed >>> 8 & 255;
					blue = packed >>> 16 & 255;
				} else {
					red = packed >>> 24 & 255;
					green = packed >>> 16 & 255;
					blue = packed >>> 8 & 255;
				}
				red = (int) (red * ((tint >>> 16 & 255) / 255F) * shade);
				green = (int) (green * ((tint >>> 8 & 255) / 255F) * shade);
				blue = (int) (blue * ((tint & 255) / 255F) * shade);
				vertexData[index] = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
						? GHOST_ALPHA << 24 | blue << 16 | green << 8 | red
						: red << 24 | green << 16 | blue << 8 | GHOST_ALPHA;
			}
			buffer.addVertexData(vertexData);
			EnumFacing face = quad.getFace();
			buffer.putNormal(face.getXOffset(), face.getYOffset(), face.getZOffset());
		}
	}
}

package fun.aloyenz.stfr.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.reactor.physics.StfrRotorRunout;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;

/** Each invisible rotor anchor draws its own saved source block. */
public final class RenderRotorPlaceholder
		extends TileEntitySpecialRenderer<TileEntityRotorPlaceholder> {
	@Override
	public void render(
			TileEntityRotorPlaceholder tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		IBlockState state = tile.original();
		if (state == null || tile.getWorld() == null) return;
		BlockPos pos = tile.getPos(), pivot = tile.pivot();
		BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
		float oldX = OpenGlHelper.lastBrightnessX, oldY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
		Minecraft.getMinecraft()
				.getTextureManager()
				.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.enableLighting();
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.translate(
				x + pivot.getX() - pos.getX() + .5D,
				y + pivot.getY() - pos.getY() + .5D,
				z + pivot.getZ() - pos.getZ() + .5D);
		EnumFacing axis=tile.axis();
		StfrRotorRunout.Sample runout=StfrClientVisuals.rotorRunout(
				tile.getWorld(),tile.center(),partialTicks,axis);
		// Both tilt axes are transverse to this stabilizer, including the vertical pair.
		int tx=axis.getAxis()==EnumFacing.Axis.X ? 0 : 1;
		int ty=axis.getAxis()==EnumFacing.Axis.X ? 1 : 0;
		GlStateManager.rotate((float)runout.tilt,tx,ty,0);
		GlStateManager.rotate((float)runout.nutation,
				-axis.getZOffset()*ty,axis.getZOffset()*tx,axis.getXOffset()*ty-axis.getYOffset()*tx);
		GlStateManager.translate(axis.getXOffset()*runout.axial,
				axis.getYOffset()*runout.axial,axis.getZOffset()*runout.axial);
		GlStateManager.rotate(
				(float)
						Math.toDegrees(
								-StfrClientVisuals.angle(
										tile.getWorld(), tile.center(), partialTicks)),
				tile.axis().getXOffset(),
				tile.axis().getYOffset(),
				tile.axis().getZOffset());
		GlStateManager.translate(
				pos.getX() - pivot.getX() - .5D,
				pos.getY() - pivot.getY() - .5D,
				pos.getZ() - pivot.getZ() - .5D);
		IBakedModel model = dispatcher.getModelForState(state);
		dispatcher.getBlockModelRenderer().renderModelBrightnessColor(state, model, 1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
	}
}

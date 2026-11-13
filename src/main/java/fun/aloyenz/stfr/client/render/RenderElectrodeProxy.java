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
import net.minecraft.util.math.Vec3d;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;

/** Aims the original baked block model toward the core without changing its breakability. */
public final class RenderElectrodeProxy
		extends TileEntitySpecialRenderer<TileEntityElectrodeProxy> {
	@Override
	public void render(
			TileEntityElectrodeProxy tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		IBlockState state = tile.original();
		if (state == null || tile.getWorld() == null) return;
		BlockPos pos = tile.getPos(), center = tile.center();
		Vec3d mount = StfrElectrodes.mount(pos, state);
		double dx = center.getX() + .5 - mount.x,
				dy = center.getY() + .5 - mount.y,
				dz = center.getZ() + .5 - mount.z;
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < .001D) return;
		dx /= length;
		dy /= length;
		dz /= length;
		EnumFacing facing = StfrElectrodes.facing(state);
		double fx = facing.getXOffset(), fy = facing.getYOffset(), fz = facing.getZOffset();
		double ax = fy * dz - fz * dy, ay = fz * dx - fx * dz, az = fx * dy - fy * dx;
		double sine = Math.sqrt(ax * ax + ay * ay + az * az);
		double angle = Math.toDegrees(Math.atan2(sine, fx * dx + fy * dy + fz * dz));
		BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
		float oldX = OpenGlHelper.lastBrightnessX, oldY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
		Minecraft.getMinecraft()
				.getTextureManager()
				.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.enableLighting();
		GlStateManager.color(1F, 1F, 1F, 1F);
		double px = .5 - fx * .5, py = .5 - fy * .5, pz = .5 - fz * .5;
		double magneticTick=StfrClientVisuals.magneticTick(center,partialTicks);
        double jitter=StfrMagneticCollapse.vibration(magneticTick,pos.toLong());
        GlStateManager.translate(x + px, y + py, z + pz);
        GlStateManager.rotate((float)(jitter*65),1,.35F,.7F);
		if (sine > 1E-5D)
			GlStateManager.rotate(
					(float) angle, (float) (ax / sine), (float) (ay / sine), (float) (az / sine));
		else if (angle > 90D)
			GlStateManager.rotate(
					180F, Math.abs(fx) < .5 ? 1F : 0F, Math.abs(fx) < .5 ? 0F : 1F, 0F);
		GlStateManager.translate(-px, -py, -pz);
		IBakedModel model = dispatcher.getModelForState(state);
		dispatcher.getBlockModelRenderer().renderModelBrightnessColor(state, model, 1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
	}
}

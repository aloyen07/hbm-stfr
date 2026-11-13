package fun.aloyenz.stfr.client.render;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Persistent core rays; every visible path is traced against actual collision shapes. */
public final class StfrEmergencyLight {

	private StfrEmergencyLight() {}

	static boolean transparent(IBlockState state) {
		if (state.getMaterial() == Material.AIR
				|| state.getMaterial() == Material.GLASS
				|| state.getMaterial().isLiquid()) return true;
		ResourceLocation name = state.getBlock().getRegistryName();
		return name != null && name.getPath().contains("glass");
	}

	public static Vec3d trace(World world, Vec3d start, Vec3d direction, double range) {
		return trace(world,start,direction,range,null);
	}

	public static Vec3d trace(World world, Vec3d start, Vec3d direction, double range, BlockPos ignored) {
		Vec3d end = start.add(direction.scale(range)), cursor = start;
		for (int attempt = 0; attempt < 128; attempt++) {
			RayTraceResult hit = world.rayTraceBlocks(cursor, end, false, true, false);
			if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return end;
			BlockPos pos = hit.getBlockPos();
			if (!pos.equals(ignored) && !transparent(world.getBlockState(pos)))
				return hit.hitVec.subtract(direction.scale(.015));
			double exit = Double.POSITIVE_INFINITY;
			if (Math.abs(direction.x) > 1E-7)
				exit =
						Math.min(
								exit,
								((direction.x > 0 ? pos.getX() + 1 : pos.getX()) - hit.hitVec.x)
										/ direction.x);
			if (Math.abs(direction.y) > 1E-7)
				exit =
						Math.min(
								exit,
								((direction.y > 0 ? pos.getY() + 1 : pos.getY()) - hit.hitVec.y)
										/ direction.y);
			if (Math.abs(direction.z) > 1E-7)
				exit =
						Math.min(
								exit,
								((direction.z > 0 ? pos.getZ() + 1 : pos.getZ()) - hit.hitVec.z)
										/ direction.z);
			cursor = hit.hitVec.add(direction.scale(Math.max(0, exit) + .001));
			if (cursor.squareDistanceTo(start) >= range * range) return end;
		}
		return cursor;
	}

	static void startupFailure(World world,BlockPos center,double cx,double cy,double cz,double time,double ticks,double recovery) {
		StfrVolumetricLight.startupFailure(world,center,cx,cy,cz,time,ticks,recovery);
		render(world,center,cx,cy,cz,time,false,false,false,false);
	}

	static void render(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			boolean meltdown) {
		render(world, center, cx, cy, cz, time, meltdown, false);
	}

	static void render(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			boolean meltdown,
			boolean white) {
		render(world,center,cx,cy,cz,time,meltdown,white,false);
	}

	static void renderSelfDestruct(World world,BlockPos center,double cx,double cy,double cz,double time) {
		render(world,center,cx,cy,cz,time,true,true,false,false);
	}
	static void render(World world,BlockPos center,double cx,double cy,double cz,double time,
			boolean meltdown,boolean white,boolean preview) {
		render(world,center,cx,cy,cz,time,meltdown,white,preview,true);
	}
	private static void render(World world,BlockPos center,double cx,double cy,double cz,double time,
			boolean meltdown,boolean white,boolean preview,boolean volume) {
		if(volume)StfrVolumetricLight.render(world, center, cx, cy, cz, time, meltdown);
		Vec3d core = new Vec3d(center.getX() + .5, center.getY() + .5, center.getZ() + .5);
		Vec3d camera =
				new Vec3d(
						cx, cy + Minecraft.getMinecraft().getRenderViewEntity().getEyeHeight(), cz);
		boolean previousAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		GlStateManager.pushMatrix();
		GlStateManager.translate(-cx, -cy, -cz);
		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		int count = meltdown ? 96 : 24;
		for (int i = 0; i < count; i++) {
			double y = 1 - 2 * (i + .5) / count,
					radius = Math.sqrt(1 - y * y),
					angle = i * 2.399963229728653 + time * .003;
			Vec3d direction = new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
			Vec3d end = trace(world, core, direction, 34, preview ? center : null);
			if (end.squareDistanceTo(core) < 1) continue;
			Vec3d side = direction.crossProduct(camera.subtract(core)).normalize();
			float pulse = (float) (.55 + .35 * Math.sin(time * .055 + i * 1.917));
			for (int layer = 0; layer < 3; layer++) {
				double width = layer == 0 ? .17 : layer == 1 ? .08 : .025;
				float alpha =
						pulse
								* (layer == 0 ? .10F : layer == 1 ? .20F : .5F)
								* (meltdown ? 1.6F : 1F);
				Vec3d offset = side.scale(width);
				vertex(b, core.subtract(offset), meltdown || white, alpha);
				vertex(b, core.add(offset), meltdown || white, alpha);
				vertex(b, end.add(offset), meltdown || white, alpha * .2F);
				vertex(b, end.subtract(offset), meltdown || white, alpha * .2F);
			}
		}
		Tessellator.getInstance().draw();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		if (previousAlpha) GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.popMatrix();
	}

	private static void vertex(BufferBuilder b, Vec3d point, boolean white, float alpha) {
		b.pos(point.x, point.y, point.z)
				.color(white ? 1F : .62F, white ? 1F : .12F, 1F, alpha)
				.endVertex();
	}
}

package fun.aloyenz.stfr.client.render;

import java.util.Random;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.*;
import fun.aloyenz.stfr.structure.StfrElectrodes;

/** Contingency rays and purge flares share the existing core renderer. */
final class RenderStfrEmergencyEffects {

	static void render(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			StfrReactor.RunState state,
			int tick,
			double recovery,
			int destabilizerStage,
			double destabilizerTicks,
			double rotorAngle) {
		int count = recovery > 0 || state==StfrReactor.RunState.SELF_DESTRUCT ? 0 : StfrEmergencyEffects.rays(state, tick);
		boolean loss =
				state == StfrReactor.RunState.STABILIZER_LOSS && tick >= 270 && recovery < 410;
		boolean vent =
				state == StfrReactor.RunState.SHUTDOWN_FAILURE
						&& recovery >= 695
						&& recovery < 1200;
		boolean remnant =
				state == StfrReactor.RunState.SHUTDOWN_FAILURE
						&& recovery >= 990
						&& recovery < 1400;
		boolean overload = state == StfrReactor.RunState.SELF_DESTRUCT && tick >= 2800;
		boolean digammaSpin =
				state == StfrReactor.RunState.ONLINE
						&& destabilizerStage >= 2
						&& destabilizerStage <= 5;
		boolean digammaArc = digammaSpin && destabilizerStage == 5;
		if (count == 0 && !loss && !vent && !overload && !remnant && !digammaSpin) return;
		boolean oldDepthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		GlStateManager.pushMatrix();
		GlStateManager.translate(-cx, -cy, -cz);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.disableCull();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.depthMask(false);
		Vec3d start = new Vec3d(center).add(.5, .5, .5), camera = new Vec3d(cx, cy, cz);
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (int i = 0; i < count; i++) {
			Vec3d direction =
					StfrEmergencyEffects.rayDirection(
							center.toLong(), world.getTotalWorldTime(), i);
			Vec3d end = StfrEmergencyLight.trace(world, start, direction, 20);
			boolean red = state == StfrReactor.RunState.SHUTDOWN_FAILURE;
			beam(b, start, end, camera, .35, 1, red ? .25F : 1, red ? .1F : 1, .18F);
			beam(b, start, end, camera, .055, 1, 1, 1, .9F);
		}
		if (loss)
			for (StfrElectrodes.Chain chain : StfrElectrodes.chains())
				if (chain.index < 0) {
					lightning(
							b,
							start.add(chain.relativeTip()),
							start,
							camera,
							center.toLong() + chain.direction.ordinal(),
							world.getTotalWorldTime());
				}
		if (overload || vent)
			for (EnumFacing face : EnumFacing.VALUES) {
				Vec3d origin =
						start.add(new Vec3d(face.getDirectionVec()).scale(overload ? 10 : 9));
				Vec3d end =
						overload
								? origin.add(
										StfrEmergencyEffects.rayDirection(
														center.toLong() + face.ordinal(),
														world.getTotalWorldTime() / 2,
														0)
												.scale(6))
								: start.add(new Vec3d(face.getDirectionVec()).scale(13));
				lightning(
						b,
						origin,
						end,
						camera,
						center.toLong() + face.ordinal(),
						world.getTotalWorldTime());
			}
		if (digammaSpin) {
			double progress =
					digammaArc ? Math.min(1D, Math.max(0D, destabilizerTicks / 460D)) : 0D;
			for (EnumFacing face : EnumFacing.VALUES) {
				Vec3d axis = new Vec3d(face.getDirectionVec());
				Vec3d tangent = axis.crossProduct(new Vec3d(0, 1, 0));
				if (tangent.lengthSquared() < .1D) tangent = axis.crossProduct(new Vec3d(1, 0, 0));
				tangent = tangent.normalize();
				Vec3d bitangent = axis.crossProduct(tangent).normalize();
				rotorTrails(
						b,
						start.add(axis.scale(10.5D)),
						tangent,
						bitangent,
						camera,
						rotorAngle,
						progress);
				if (!digammaArc) continue;
				// The digamma discharge uses the same turbine-origin lightning as SELF_DESTRUCT.
				Vec3d origin = start.add(axis.scale(10D));
				Vec3d end =
						origin.add(
								StfrEmergencyEffects.rayDirection(
												center.toLong() + face.ordinal(),
												world.getTotalWorldTime() / 2,
												0)
										.scale(6D));
				lightning(
						b,
						origin,
						end,
						camera,
						center.toLong() + face.ordinal(),
						world.getTotalWorldTime());
			}
		}
		if (vent)
			for (EnumFacing face : EnumFacing.VALUES) {
				Vec3d origin = start.add(new Vec3d(face.getDirectionVec()).scale(9));
				Vec3d end =
						StfrEmergencyLight.trace(
								world, origin, new Vec3d(face.getDirectionVec()), 7);
				float fade = (float) Math.min(1, Math.max(0, (1200 - recovery) / 210D));
				beam(b, origin, end, camera, .7, 1, .2F, .05F, .22F * fade);
				beam(b, origin, end, camera, .12, 1, .9F, .7F, .6F * fade);
			}
		if (remnant) {
			float fade = (float) Math.max(0, (1400 - recovery) / 410D);
			double time = world.getTotalWorldTime() + recovery % 1;
			double pulse = 1 + .12 * Math.sin(time * .9);
			Vec3d upper = start.add(0, 9, 0), lower = start.add(0, -9, 0);
			solidBeam(b, upper, lower, camera, 1.4 * fade * pulse, 1, .08F, .03F, .65F * fade);
			solidBeam(b, upper, lower, camera, .55 * fade * pulse, 1, .5F, .3F, fade);
			solidBeam(b, upper, lower, camera, .24 * fade, 1, 1, 1, fade);
			for (int i = 0; i < 16; i++) {
				double y = 9 - ((time * .6 + i * 1.125) % 18);
				double angle = time * .25 + i * 2.39996;
				Vec3d knot = start.add(Math.cos(angle) * .4 * fade, y, Math.sin(angle) * .4 * fade);
				beam(
						b,
						knot.add(0, .35, 0),
						knot.add(0, -.35, 0),
						camera,
						.16 * fade,
						1,
						.7F,
						.4F,
						fade);
			}
		}
		Tessellator.getInstance().draw();
		GlStateManager.depthMask(oldDepthWrite);
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.popMatrix();
	}

	private static void lightning(
			BufferBuilder b, Vec3d start, Vec3d end, Vec3d camera, long seed, long tick) {
		Random random = new Random(seed ^ (tick / 2) * 0x9E3779B97F4A7C15L);
		Vec3d delta = end.subtract(start), previous = start;
		for (int i = 1; i <= 12; i++) {
			Vec3d next =
					i == 12
							? end
							: start.add(delta.scale(i / 12D))
									.add(
											(random.nextDouble() - .5) * .7,
											(random.nextDouble() - .5) * .7,
											(random.nextDouble() - .5) * .7);
			beam(b, previous, next, camera, .28, .4F, .65F, 1, .4F);
			beam(b, previous, next, camera, .075, 1, 1, 1, .95F);
			previous = next;
		}
	}

	private static void rotorTrails(
			BufferBuilder b,
			Vec3d origin,
			Vec3d u,
			Vec3d v,
			Vec3d camera,
			double rotorAngle,
			double progress) {
		for (int spoke = 0; spoke < 4; spoke++) {
			double phase = -rotorAngle + spoke * Math.PI / 2D;
			Vec3d previous = null;
			for (int segment = 0; segment <= 6; segment++) {
				double angle = phase - segment * (.12D + progress * .09D);
				Vec3d point =
						origin.add(u.scale(Math.cos(angle) * 1.65D))
								.add(v.scale(Math.sin(angle) * 1.65D));
				if (previous != null)
					beam(
							b,
							previous,
							point,
							camera,
							.075D,
							1F,
							1F,
							1F,
							(float) (.28D + progress * .16D) * (7 - segment) / 6F);
				previous = point;
			}
		}
	}

	private static void solidBeam(
			BufferBuilder b,
			Vec3d start,
			Vec3d end,
			Vec3d camera,
			double width,
			float r,
			float g,
			float blue,
			float alpha) {
		beam(b, start, end, camera, width, r, g, blue, alpha, 1F);
	}

	private static void beam(
			BufferBuilder b,
			Vec3d start,
			Vec3d end,
			Vec3d camera,
			double width,
			float r,
			float g,
			float blue,
			float alpha) {
		beam(b, start, end, camera, width, r, g, blue, alpha, .15F);
	}

	private static void beam(
			BufferBuilder b,
			Vec3d start,
			Vec3d end,
			Vec3d camera,
			double width,
			float r,
			float g,
			float blue,
			float alpha,
			float endFade) {
		Vec3d direction = end.subtract(start);
		if (direction.lengthSquared() < 1E-10) return;
		Vec3d side = direction.crossProduct(camera.subtract(start));
		if (side.lengthSquared() < 1E-10) side = direction.crossProduct(new Vec3d(0, 1, 0));
		if (side.lengthSquared() < 1E-10) side = new Vec3d(1, 0, 0);
		side = side.normalize().scale(width);
		vertex(b, start.subtract(side), r, g, blue, alpha);
		vertex(b, start.add(side), r, g, blue, alpha);
		vertex(b, end.add(side), r, g, blue, alpha * endFade);
		vertex(b, end.subtract(side), r, g, blue, alpha * endFade);
	}

	private static void vertex(
			BufferBuilder b, Vec3d p, float r, float g, float blue, float alpha) {
		b.pos(p.x, p.y, p.z).color(r, g, blue, alpha).endVertex();
	}
}

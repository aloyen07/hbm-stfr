package fun.aloyenz.stfr.client.render;

import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructPalette;
import fun.aloyenz.stfr.structure.StfrElectrodes;

/** Client-only visual of the six electrode groups, forming core and shield. */
final class RenderReactorCore {

	private RenderReactorCore() {}

	static void render(
			double x,
			double y,
			double z,
			int stateId,
			double ticks,
			double animationTime,
			double recoveryTicks,
			List<StfrElectrodes.Chain> electrodes,
			boolean coreExists,
			boolean digamma,
			int destabilizerStage) {
        render(x,y,z,stateId,ticks,animationTime,recoveryTicks,electrodes,coreExists,digamma,destabilizerStage,0);
    }
	static void render(
			double x,
			double y,
			double z,
			int stateId,
			double ticks,
			double animationTime,
			double recoveryTicks,
			List<StfrElectrodes.Chain> electrodes,
			boolean coreExists,
			boolean digamma,
			int destabilizerStage,float selfCharge) {
		StfrReactor.RunState[] states = StfrReactor.RunState.values();
		if (stateId < 0 || stateId >= states.length) return;
		StfrReactor.RunState state = states[stateId];
		double beam, core, shield, fuel;
		if (state == StfrReactor.RunState.STARTUP_CONFIRMED) {
			beam = progress(ticks, 375, 430);
			// Before ignition completes the fuel is only an unstable particle cloud.
			fuel = progress(ticks, 615, 860);
			core = progress(ticks, 860, 895);
			shield = progress(ticks, 415, 585);
		} else if (state == StfrReactor.RunState.ONLINE) {
			beam = core = shield = fuel = 1;
		} else if (state == StfrReactor.RunState.STARTUP_FAILURE) {
			beam = core = fuel = 1;
			shield =
					recoveryTicks > 0
							? .2D + .8D * progress(recoveryTicks, 500, 600)
							: .15D + .15D * Math.sin(animationTime * .8D);
		} else if (state == StfrReactor.RunState.MELTDOWN) {
			core = fuel = 1;
			beam = shield = 0;
		} else if (state == StfrReactor.RunState.SHUTDOWN) {
			// The core survives until the fiery deactivation burst at tick 470.
			beam = shield = 1D - progress(ticks, 445, 470);
			core = ticks < 470 ? 1D : 0D;
			fuel = core;
		} else if (state.ordinal() >= StfrReactor.RunState.OVERLOAD.ordinal()) {
			beam = 1;
			core = fuel = 1;
			shield = .3;
			if (state == StfrReactor.RunState.OVERLOAD) shield = 0;
			if (state == StfrReactor.RunState.REACTION_LOSS) {
				core = .85;
				shield = .12;
			}
			if (state == StfrReactor.RunState.REIGNITION) {
				shield = .12 + .88 * progress(ticks, 380, 1000);
			}
			if (state == StfrReactor.RunState.UNDERLOAD) {
				beam = shield = 0;
				core = 1;
				fuel = 0;
			}
			if (state == StfrReactor.RunState.STABILIZER_LOSS) {
				beam = ticks >= 280 || recoveryTicks > 0 ? 1 : 0;
				shield = .7;
			}
			if (state == StfrReactor.RunState.MANUAL_RESTABILIZATION)
				shield =
						recoveryTicks > 0
								? .4 + .6 * progress(recoveryTicks, 0, 598)
								: Math.max(0, 1 - ticks / 3710D);
			if (state == StfrReactor.RunState.SHUTDOWN_FAILURE) {
				core = recoveryTicks >= 990 ? 0 : Math.max(.08, 1 - ticks / 4050D);
				fuel = core;
				beam = ticks >= 4050 || recoveryTicks >= 990 ? 0 : 1;
				shield = core > 0 ? .9 : 0;
			}
			if (state == StfrReactor.RunState.SELF_DESTRUCT) {
				core = fuel = coreExists ? 1 : 0;
				beam = coreExists && ticks < 3620 ? 1 : 0;
				shield = coreExists ? (ticks < 2910 ? 1 : .2) : 0;
			}
		} else return;
		if (beam <= 0 && core <= 0 && shield <= 0 && fuel <= 0) return;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		StfrGlowMotes.prepare();
		float previousLightX = OpenGlHelper.lastBrightnessX;
		float previousLightY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableAlpha();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableDepth();
		GlStateManager.disableCull();

		// Both fields breathe independently. Keep the core safely inside the shield even
		// at the extrema of the two animations.
		double shieldRadius = 1.20D * (1D + .035D * Math.sin(animationTime * .043D));
		double coreRadius =
				(.08D + .86D * core) * (1D + .055D * Math.sin(animationTime * .087D + .8D));
		coreRadius = Math.min(coreRadius, shieldRadius - .12D);
		// The red core is solid: write its depth before translucent scan rings and
		// sparks, so the back halves of those effects cannot shine through it.
		if (core > 0) {
			GlStateManager.depthMask(true);
			boolean white = state == StfrReactor.RunState.MELTDOWN;
			boolean self=state==StfrReactor.RunState.SELF_DESTRUCT;
			StfrSelfDestructPalette.Color tint=StfrSelfDestructPalette.color(ticks,selfCharge);
			boolean cold =
					state == StfrReactor.RunState.REACTION_LOSS
							|| state == StfrReactor.RunState.REIGNITION;
			boolean empty = state == StfrReactor.RunState.UNDERLOAD;
            float magneticEmission=1;
            sphere(
                    coreRadius,
					magneticEmission*(self ? tint.red : digamma ? .48F : cold ? .15F : 1F),
					magneticEmission*(self ? tint.green : digamma ? .005F : white || cold || empty ? 1F : .08F + .13F * (float) core),
					magneticEmission*(self ? tint.blue : digamma ? .008F : white || cold || empty ? 1F : .06F),
					1F);
		}
		GlStateManager.depthMask(false);
		if (digamma && core > 0 && state!=StfrReactor.RunState.SELF_DESTRUCT) {
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			double pulse = 1.75D + .18D * Math.sin(animationTime * .16D);
			sphere(coreRadius * pulse, 1F, .005F, .008F, .22F);
			sphere(coreRadius * (pulse + .7D), .7F, 0F, .01F, .07F);
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		}
		if (destabilizerStage == 5 && state!=StfrReactor.RunState.SELF_DESTRUCT) {
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			sphere(
					coreRadius * (2.2D + .25D * Math.sin(animationTime * .4D)),
					1F,
					.12F,
					.04F,
					.20F);
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		}
		if (state == StfrReactor.RunState.MELTDOWN) {
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			sphere(coreRadius * 1.7D, 1F, 1F, 1F, .34F);
			sphere(coreRadius * 2.5D, 1F, .88F, 1F, .13F);
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		}
		if (beam > 0) {
			beams(beam, animationTime, false, electrodes);
			beams(beam, animationTime, true, electrodes);
			primaryBeams(beam, animationTime, electrodes);
			beamParticles(beam, animationTime, electrodes);
			primaryBeamSparks(beam, animationTime, electrodes);
		}
		if (fuel > 0) {
			double cloudRadius = core > 0 ? coreRadius : .16D + .35D * fuel;
			coreParticles(cloudRadius, Math.max(.35D, fuel), animationTime,
                state==StfrReactor.RunState.SELF_DESTRUCT?StfrSelfDestructPalette.color(ticks,selfCharge):null);
		}
		if (shield > 0) {
			// Smoothstep makes the shield a genuine opacity fade-in rather than a pop.
			double shieldOpacity = smooth(shield);
			shieldParticles(shieldRadius, shieldOpacity, animationTime);
			sphere(shieldRadius, .95F, .97F, 1F, .30F * (float) shieldOpacity);
		}

		GlStateManager.enableCull();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.enableAlpha();
		GlStateManager.color(1F, 1F, 1F, 1F);
		OpenGlHelper.setLightmapTextureCoords(
				OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
		GlStateManager.popMatrix();
	}

	private static double progress(double tick, double from, double until) {
		return Math.max(0D, Math.min(1D, (tick - from) / (until - from)));
	}

	private static double smooth(double value) {
		return value * value * (3D - 2D * value);
	}

	private static void beams(
			double progress, double time, boolean bright, List<StfrElectrodes.Chain> electrodes) {
		GlStateManager.glLineWidth(bright ? 3.5F : 7.0F);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		for (StfrElectrodes.Chain chain : electrodes) {
			if (chain.index < 0) continue;
			EnumFacing direction = chain.direction;
			int spoke = chain.index;
			Vec3d tip = chain.relativeTip();
			double sx = tip.x, sy = tip.y, sz = tip.z;
			// The beam reaches the center as it forms; a brighter band travels inward.
			double reach = progress;
			double phase = (time * .032D + direction.ordinal() * .13D + spoke * .19D) % 1D;
			double bandStart = Math.max(0D, Math.min(reach, phase * reach));
			double bandEnd = Math.min(reach, bandStart + .28D * reach);
			float pulse = (float) (Math.sin(Math.PI * phase) * progress);
			float a = (float) progress * (bright ? .28F : .16F);
			buffer.pos(sx, sy, sz)
					.color(bright ? .78F : .18F, bright ? .95F : .60F, 1F, a)
					.endVertex();
			buffer.pos(sx * (1D - reach), sy * (1D - reach), sz * (1D - reach))
					.color(1F, 1F, 1F, a)
					.endVertex();
			float bandAlpha = pulse * (bright ? .85F : .30F);
			buffer.pos(sx * (1D - bandStart), sy * (1D - bandStart), sz * (1D - bandStart))
					.color(bright ? .78F : .18F, bright ? .95F : .60F, 1F, bandAlpha)
					.endVertex();
			buffer.pos(sx * (1D - bandEnd), sy * (1D - bandEnd), sz * (1D - bandEnd))
					.color(1F, 1F, 1F, bandAlpha)
					.endVertex();
		}
		Tessellator.getInstance().draw();
		GlStateManager.glLineWidth(1F);
	}

	/** Six central CRITICAL electrode tips carry a denser, pulsing ignition current. */
	private static void primaryBeams(
			double progress, double time, List<StfrElectrodes.Chain> electrodes) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		for (int layer = 0; layer < 3; layer++) {
			GlStateManager.glLineWidth(layer == 0 ? 12F : layer == 1 ? 7F : 3F);
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			for (StfrElectrodes.Chain chain : electrodes) {
				if (chain.index >= 0) continue;
				EnumFacing direction = chain.direction;
				Vec3d tip = chain.relativeTip();
				double pulse = .78D + .22D * Math.sin(time * .25D + direction.ordinal());
				float alpha =
						(float) (progress * pulse * (layer == 0 ? .18D : layer == 1 ? .38D : .88D));
				double endpoint = 1D - progress;
				buffer.pos(tip.x, tip.y, tip.z)
						.color(layer == 0 ? .12F : .72F, layer == 0 ? .58F : .96F, 1F, alpha)
						.endVertex();
				buffer.pos(tip.x * endpoint, tip.y * endpoint, tip.z * endpoint)
						.color(1F, 1F, 1F, alpha)
						.endVertex();
			}
			Tessellator.getInstance().draw();
		}
		GlStateManager.glLineWidth(1F);
	}

	private static void primaryBeamSparks(
			double progress, double time, List<StfrElectrodes.Chain> electrodes) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (StfrElectrodes.Chain chain : electrodes) {
			if (chain.index >= 0) continue;
			EnumFacing direction = chain.direction;
			Vec3d tip = chain.relativeTip();
			for (int spark = 0; spark < 28; spark++) {
				double phase =
						(time * .075D + spark * .6180339887D + direction.ordinal() * .17D) % 1D;
				double along = 1D - phase * progress;
				double twist = time * .31D + spark * 2.4D;
				double radius = .11D + .11D * Math.sin(time * .42D + spark * 1.7D);
				double ox =
						direction.getAxis() == EnumFacing.Axis.X ? 0D : Math.cos(twist) * radius;
				double oy =
						direction.getAxis() == EnumFacing.Axis.Y ? 0D : Math.sin(twist) * radius;
				double oz =
						direction.getAxis() == EnumFacing.Axis.Z
								? 0D
								: Math.cos(twist + 1.57D) * radius;
				mote(
						buffer,
						tip.x * along + ox,
						tip.y * along + oy,
						tip.z * along + oz,
						.027D + .018D * Math.sin(time * .21D + spark),
						.78F,
						.96F,
						1F,
						(float) (progress * (.35D + .65D * Math.sin(Math.PI * phase))));
			}
		}
		Tessellator.getInstance().draw();
	}

	/** Small energy motes travel from each electrode to the forming core. */
	private static void beamParticles(
			double progress, double time, List<StfrElectrodes.Chain> electrodes) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (StfrElectrodes.Chain chain : electrodes) {
			if (chain.index < 0) continue;
			EnumFacing direction = chain.direction;
			int spoke = chain.index;
			Vec3d tip = chain.relativeTip();
			double sx = tip.x, sy = tip.y, sz = tip.z;
			for (int mote = 0; mote < 3; mote++) {
				double phase =
						(time * .046D + direction.ordinal() * .17D + spoke * .23D + mote / 3D) % 1D;
				double along = phase * progress;
				mote(
						buffer,
						sx * (1D - along),
						sy * (1D - along),
						sz * (1D - along),
						.035D,
						.76F,
						.96F,
						1F,
						(float) (progress * Math.sin(Math.PI * phase)));
			}
		}
		Tessellator.getInstance().draw();
	}

	/** A horizontal circle of particles sweeps repeatedly over the shield's surface. */
	private static void shieldParticles(double radius, double opacity, double time) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (int echo=2; echo>=0; echo--) {
			double phase=time-echo*1.5D;
			double y=Math.sin(phase*.038D)*radius*.82D;
			double ringRadius=Math.sqrt(Math.max(0D,radius*radius-y*y));
			for (int point=0;point<72;point++) {
				double angle=2D*Math.PI*point/72D+phase*.018D;
				double jitter=1D+.012D*Math.sin(phase*.12D+point*1.71D);
				float brightness=(float)(.85+.15*Math.sin(time*.09+point*1.71));
				mote(buffer,Math.cos(angle)*ringRadius*jitter,y,Math.sin(angle)*ringRadius*jitter,
						echo==0 ? .045D : .03D,.78F,.92F,1F,
						(float)(opacity*(echo==0 ? .9 : .12/echo))*brightness);
			}
		}
		Tessellator.getInstance().draw();
	}

	/** Sparse red plasma motes orbit the active core. */
	private static void coreParticles(double radius, double intensity, double time,StfrSelfDestructPalette.Color tint) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (int mote = 0; mote < 28; mote++) {
			double phase = time * .055D + mote * 2.399D;
			double latitude = Math.sin(phase * 1.37D) * .72D;
			double horizontal = Math.sqrt(1D - latitude * latitude) * radius * 1.04D;
			mote(
					buffer,
					Math.cos(phase) * horizontal,
					latitude * radius,
					Math.sin(phase) * horizontal,
					.045D,
					1F,
					tint==null?.18F + .15F * (float) intensity:tint.green,
					tint==null?.07F:tint.blue,
					.25F + .65F * (float) intensity);
		}
		Tessellator.getInstance().draw();
	}

	private static void mote(
			BufferBuilder buffer,
			double x,
			double y,
			double z,
			double radius,
			float red,
			float green,
			float blue,
			float alpha) {
		StfrGlowMotes.append(buffer, new Vec3d(x,y,z), radius, red, green, blue, alpha);
	}

	private static void sphere(double radius, float red, float green, float blue, float alpha) {
		final int rings = 16, sectors = 32;
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
		for (int ring = 0; ring < rings; ring++) {
			double a0 = Math.PI * ring / rings;
			double a1 = Math.PI * (ring + 1) / rings;
			for (int sector = 0; sector < sectors; sector++) {
				double b0 = 2D * Math.PI * sector / sectors;
				double b1 = 2D * Math.PI * (sector + 1) / sectors;
				vertex(buffer, radius, a0, b0, red, green, blue, alpha);
				vertex(buffer, radius, a1, b0, red, green, blue, alpha);
				vertex(buffer, radius, a1, b1, red, green, blue, alpha);
				vertex(buffer, radius, a0, b0, red, green, blue, alpha);
				vertex(buffer, radius, a1, b1, red, green, blue, alpha);
				vertex(buffer, radius, a0, b1, red, green, blue, alpha);
			}
		}
		Tessellator.getInstance().draw();
	}

	private static void vertex(
			BufferBuilder buffer,
			double radius,
			double polar,
			double azimuth,
			float red,
			float green,
			float blue,
			float alpha) {
		double sin = Math.sin(polar);
		buffer.pos(
						radius * sin * Math.cos(azimuth),
						radius * Math.cos(polar),
						radius * sin * Math.sin(azimuth))
				.color(red, green, blue, alpha)
				.endVertex();
	}
}

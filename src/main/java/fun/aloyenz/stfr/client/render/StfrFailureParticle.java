package fun.aloyenz.stfr.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/** Compatibility particles for post-1.12 smoke, soul fire and flashes. */
public final class StfrFailureParticle extends Particle {

	private final int kind;
	private final float scale;
	private final double originX, originZ, phase;

	public StfrFailureParticle(
			World world, double x, double y, double z, double vx, double vy, double vz, int kind) {
		super(world, x, y, z, 0, 0, 0);
		this.kind = kind;
		originX = x;
		originZ = z;
		phase = world.rand.nextDouble() * Math.PI * 2;
		motionX = vx;
		motionY = vy;
		motionZ = vz;
		setParticleTextureIndex(kind == 1 ? 48 : 0);
		particleMaxAge =
				kind == 0
						? 80
						: kind == 2 ? 2 : kind == 3 ? 30 : kind == 4 ? 55 : kind == 5 ? 14 : 20;
		scale = kind == 2 ? 4F : kind == 3 ? 2F : kind == 0 ? 1.5F : .8F;
		particleScale = scale;
		if (kind == 1 || kind == 4 || kind == 5) setRBGColorF(.5F, .25F, 1F);
		else if (kind == 0) setRBGColorF(.35F, .35F, .35F);
		else if (kind == 3) setRBGColorF(1F, .5F, .12F);
		else setRBGColorF(1, 1, 1);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (kind == 4) {
			double a = phase + particleAge * .19, r = .6 + particleAge * .035;
			setPosition(originX + Math.cos(a) * r, posY + .15, originZ + Math.sin(a) * r);
			motionX = motionZ = 0;
			motionY = .12;
			// Frozen, fading afterimages make the rising spiral read as a magical trail.
			// One every six ticks bounds a four-source emission to about 150 live motes.
			if (particleAge % 6 == 0 && Minecraft.getMinecraft().gameSettings.particleSetting < 2) {
				StfrFailureParticle trail =
						new StfrFailureParticle(world, posX, posY, posZ, 0, 0, 0, 5);
				trail.particleScale = .32F;
				Minecraft.getMinecraft().effectRenderer.addEffect(trail);
			}
		}
		float life = particleAge / (float) particleMaxAge;
		particleAlpha = (kind == 2 ? .4F : .8F) * (1 - life);
		particleScale = (kind == 5 ? .32F : scale) * (kind == 0 ? 1 + life * 3 : 1);
		if (kind == 0) {
			motionY += .002;
			motionX *= .98;
			motionZ *= .98;
		}
		if (kind == 3)
			setRBGColorF(
					Math.max(.15F, 1 - life), Math.max(.03F, .5F - life), Math.max(0, .12F - life));
	}

	@Override
	public int getBrightnessForRender(float partial) {
		return kind == 0 ? super.getBrightnessForRender(partial) : 0xF000F0;
	}
}

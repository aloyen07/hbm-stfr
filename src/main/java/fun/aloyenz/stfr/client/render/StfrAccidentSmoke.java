package fun.aloyenz.stfr.client.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/** Slowly expanding smoke with ordinary depth testing and a long, soft fade. */
final class StfrAccidentSmoke extends Particle {
	private final float baseScale;
	StfrAccidentSmoke(World world, double x, double y, double z, float scale) {
		super(world,x,y,z,0,0,0);
		baseScale=scale;
		particleMaxAge=100+world.rand.nextInt(60);
		motionX=world.rand.nextGaussian()*.035;
		motionZ=world.rand.nextGaussian()*.035;
		motionY=.08+world.rand.nextDouble()*.07;
		canCollide=false;
		setRBGColorF(.19F,.18F,.21F);
		setParticleTextureIndex(7);
	}
	@Override public void onUpdate() {
		super.onUpdate();
		float life=particleAge/(float)particleMaxAge;
		particleScale=baseScale*(1+life*3);
		particleAlpha=.38F*Math.min(1,particleAge/12F)*(1-life);
		setParticleTextureIndex(Math.max(0,7-(int)(life*8)));
		motionY=Math.min(.22,motionY+.001);
	}
}

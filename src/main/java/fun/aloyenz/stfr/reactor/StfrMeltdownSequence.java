package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrFailureEffects.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;

/** Meltdown countdown, pressure fronts and bounded debris release. */
final class StfrMeltdownSequence {
	private StfrMeltdownSequence() {}

	static void meltdown(StfrFailureEffects c, World world) {
		c.resolveProtection(world);
		c.crashMainframes(world, 1300);
		StfrReactorStatus.prepareDestruction(c.reactor, world);
		c.stop(world, "hbmstfr:failure.music.idunno_reactor_meltdown_sequence", 512);
		c.stop(world, "hbmstfr:failure.music.hacknet_ost_paki_trak", 512);
		c.silenceEmergencyMusic(world);
		for (int i = 0; i < 4; i++)
			c.sound(world, "explosion.pressuregigaboom", SoundCategory.AMBIENT, 20F, 1F);
		c.sound(world, "charge_up.reactordetonationcharging", SoundCategory.AMBIENT, 24F, 0F);
		c.sound(world, "charge_up.reactordetonationcharging", SoundCategory.AMBIENT, 24F, .05F);
		// Falling blocks are released in tickMeltdown: never as one instant burst.
		c.shockwave(world, 20);
		c.shockwave(world, 20);
		c.siren(world, 16);
		c.broadcast(world, "meltdown", "", 4);
		c.shake(world, 80, 1398, 100, 50);
	}

	static void tickMeltdown(StfrFailureEffects c, World world, int tick) {
		c.decayBroadcastLoad(world);
		c.particles(
				world,
				c.reactor.center,
				EnumParticleTypes.SMOKE_NORMAL,
				tick < 1280 ? 12 : 24,
				1,
				tick < 1280 ? .15 : .3);
		c.particles(world, c.reactor.center, EnumParticleTypes.EXPLOSION_NORMAL, 1, 1, 2);
		c.compatParticles(world, c.reactor.center, 1, 12, 1, .1);
		c.compatParticles(world, c.reactor.center, 2, 6, 10, .1);
		c.compatParticles(world, c.reactor.center, 3, 2, 1, .1);
		c.compatParticles(world, c.reactor.center, 4, 4, .5, .08);
		if (c.allowed(STRUCTURE) && tick % 4 == 0) c.liquifyBatch(world, 4);
		c.advanceDebris(world);
		if (tick <= 1280) {
			c.particles(world, c.reactor.center, EnumParticleTypes.FLAME, 12, 1, .15);
			c.particles(world, c.reactor.center, EnumParticleTypes.FIREWORKS_SPARK, 8, 1, .15);
		} else c.particles(world, c.reactor.center, EnumParticleTypes.EXPLOSION_HUGE, 1, 1, 1);
		if (tick <= 1201 && tick % 240 == 1) c.siren(world, 50);
		// TERF's orbit marker places fire, emits particles, then immediately dies
		// if its position is no longer air (including the fire it just placed).
		BlockPos orbit = c.reactor.center;
		if (world.isAirBlock(orbit.up())) {
			if (world.isAirBlock(orbit)) {
				c.resolveProtection(world);
				if (c.allowed(FIRE) && c.allowed(STRUCTURE))
					world.setBlockState(orbit, Blocks.FIRE.getDefaultState(), 3);
			}
			c.particles(world, orbit.up(), EnumParticleTypes.EXPLOSION_NORMAL, 1, 0, 0);
			c.compatParticles(world, orbit.up(), 3, 1, 0, 0);
		}
		if (tick == 60) c.broadcast(world, "shelter", "", 4);
		if (tick == 400) c.sound(world, "alarms.civil_alert_siren", SoundCategory.AMBIENT, 200F, 1F);
		int visualStage=StfrFailureTimeline.visualBlastStage(tick);
		if (visualStage>0) {
			boolean finalVisual=visualStage==3;
			c.resolveProtection(world);
			for (int i = 0; i < 2; i++)
				c.sound(
						world,
						"explosion.pressuregigaboom",
						SoundCategory.AMBIENT,
						finalVisual ? 150F : 100F,
						tick == 1280 ? 1F : 2F);
			for (EntityPlayer player : world.playerEntities)
				if (player instanceof EntityPlayerMP
						&& player.getDistanceSq(c.reactor.center) <= 128D * 128D)
					((EntityPlayerMP) player)
							.connection.setPlayerLocation(
									player.posX,
									player.posY,
									player.posZ,
									player.rotationYaw,
									Math.max(-90, player.rotationPitch - (finalVisual ? 8 : 4)));
			c.blindness(world, 512);
			c.shake(world, finalVisual ? 192 : 128, finalVisual ? 100 : 80, 100, finalVisual ? 85 : 50);
			c.visualShockwave(world,finalVisual ? 18 : 13);
		}
	}

	static void siren(StfrFailureEffects c, World world, float volume) {
		for (int i = 0; i < 3; i++)
			c.sound(world, "alarms.airraidsiren", SoundCategory.AMBIENT, volume, 1F);
	}

	static void visualShockwave(StfrFailureEffects c, World world,float radius) {
		if(!c.allowed(SHOCKWAVE))return;
		if(!c.allowed(PLAYER_DAMAGE)) {
			world.playSound(null,c.reactor.center,SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS,4F,1F);
			c.particles(world,c.reactor.center,EnumParticleTypes.EXPLOSION_HUGE,1,0,0);
			return;
		}
		world.newExplosion(null,c.reactor.center.getX()+.5,c.reactor.center.getY()+.5,
				c.reactor.center.getZ()+.5,radius,false,false);
	}

	static void shockwave(StfrFailureEffects c, World world, float radius) {
		if (!c.allowed(SHOCKWAVE) || !c.allowed(PLAYER_DAMAGE)) return;
		world.newExplosion(
				null,
				c.reactor.center.getX() + .5,
				c.reactor.center.getY() + .5,
				c.reactor.center.getZ() + .5,
				radius,
				c.allowed(FIRE) && c.allowed(STRUCTURE),
				c.allowed(STRUCTURE));
	}
}

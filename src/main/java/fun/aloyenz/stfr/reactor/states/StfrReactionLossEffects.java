package fun.aloyenz.stfr.reactor.states;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;

/** Cold failure soundscape, bounded discharges and visible pressure pulses. */
final class StfrReactionLossEffects {
	private StfrReactionLossEffects() {}
	static void transition(StfrEmergencyController c,World w) {
		c.sound(w,"explosion.shieldboom",SoundCategory.BLOCKS,1.5F,1F);
		c.particles(w,c.r.center,EnumParticleTypes.EXPLOSION_HUGE,2,1,0);
		c.particles(w,c.r.center,EnumParticleTypes.FIREWORKS_SPARK,180,1,1);
		c.particles(w,c.r.center,EnumParticleTypes.END_ROD,100,1,.4);
		c.particles(w,c.r.center,EnumParticleTypes.CLOUD,60,2,.15);
		c.r.failure().glassBlowout(w);
		c.r.failure().shake(w,60,8,120,2);
		if(c.r.failure().allowsPhysical(w,StfrFailureEffects.PLAYER_DAMAGE))
			c.r.failure().blastDamage(w,c.r.center,3F);
	}
	static void tick(StfrEmergencyController c,World w,int tick,boolean irreversible) {
		if((irreversible && tick>=920 || !irreversible && tick>=1780)
				&& StfrMagneticCollapse.dischargeStart(c.r.runState(),tick)==tick) {
			w.playSound(null,c.r.center,SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,SoundCategory.BLOCKS,.35F,1.6F);
		}
		if((!irreversible && tick==1780) || irreversible && (tick==920 || tick==1980 || tick==2740))
			c.sound(w,"rumbling",SoundCategory.AMBIENT,.35F,.9F);
		if(irreversible && tick%10==0 && c.r.failure().allowsPhysical(w,StfrFailureEffects.PLAYER_DAMAGE))
			for(EntityPlayer player:w.getEntitiesWithinAABB(EntityPlayer.class,new AxisAlignedBB(c.r.center).grow(5))) {
				double d=player.getDistanceSqToCenter(c.r.center);
				if(!player.isSpectator() && !player.capabilities.isCreativeMode && d<=25)
					player.attackEntityFrom(DamageSource.MAGIC,d<=9?7:1);
			}
	}
}

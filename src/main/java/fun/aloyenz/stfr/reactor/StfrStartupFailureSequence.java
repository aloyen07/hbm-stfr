package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrFailureEffects.*;

import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.timelines.StfrFailureTimeline;
import fun.aloyenz.stfr.sound.STFRSounds;

/** Startup-failure timeline and recovery handshake. */
final class StfrStartupFailureSequence {
	private StfrStartupFailureSequence() {}

	static void startup(StfrFailureEffects c, World world, String reason) {
		c.reset();
		c.cause = reason;
		c.resolveProtection(world);
		c.crashMainframes(world, 473);
		StfrAdvancements.grant(world, c.reactor.center, "failure");
		c.stop(world, "hbmstfr:reactor.startup_music", 128);
		c.sound(world, "music.idunno_reactor_meltdown_sequence", SoundCategory.RECORDS, 100000F, 1F);
		c.sound(world, "explosion.shieldboom", SoundCategory.AMBIENT, 16F, 2F);
		c.sound(world, "explosion.shieldboom", SoundCategory.AMBIENT, 16F, 0F);
		c.sound(world, "powerpurge", SoundCategory.AMBIENT, 10F, 0F);
		for (EnumFacing face : EnumFacing.VALUES) {
			BlockPos p = c.reactor.center.offset(face, 5);
			c.compatParticles(world, p, 0, 400, 0, .2);
		}
		c.particles(world, c.reactor.center, EnumParticleTypes.SMOKE_LARGE, 80, 0, .3);
		c.compatParticles(world, c.reactor.center, 2, 1, 0, 0);
		c.blindness(world, 35);
		c.shake(world, 256, 100, 10, 5);
		c.broadcast(
				world,
				"failure",
				"",
				5,
				new TextComponentTranslation(
						"message.hbmstfr.failure.cause." + reason.toLowerCase(Locale.ROOT)));
	}

	static boolean canRecover(StfrFailureEffects c) {
		return c.reactor.runState() == StfrReactor.RunState.STARTUP_FAILURE
				&& StfrFailureTimeline.recoveryAllowed(
						c.reactor.stateTicks(),
						c.recoveryTicks,
						c.reactor.resources().energy(),
						StfrConfig.recoveryCharge,
						c.reactor.workingTurbines());
	}

	static boolean recover(StfrFailureEffects c, World world) {
		if (!c.canRecover()) return false;
		int links=StfrLogicNetwork.connectedStabilizers(world,c.reactor);
		if(links<4) {
			c.recoveryLinksRequested=true;c.previousRecoveryLinks=links;
			c.broadcast(world,"recovery_links","",3);
			c.recoveryLinkProgress(world,links);
			StfrManager.get(world).markDirty();return false;
		}
		c.recoveryLinksRequested=false;
		c.reactor.resources().takeEnergy(StfrConfig.recoveryCharge);
		c.recoveryTicks = 1;
		c.stop(world, "hbmstfr:failure.music.idunno_reactor_meltdown_sequence", 512);
		c.sound(world, "music.hacknet_ost_paki_trak", SoundCategory.RECORDS, 8F, 1F);
		c.sound(world, "music.hacknet_music_end", SoundCategory.RECORDS, 8F, 1F);
		c.broadcast(world, "recovering", "restab_start", 1);
		return true;
	}

	static void recoveryLinkProgress(StfrFailureEffects c, World world,int links) {
		for(EntityPlayer player:world.playerEntities)
			if(c.reactor.announcementMessages() && player.getDistanceSq(c.reactor.center)<=128D*128D)
				player.sendMessage(new TextComponentTranslation("message.hbmstfr.failure.recovery_links_progress",links));
	}

	static boolean tickStartup(StfrFailureEffects c, World world, int tick) {
		c.decayBroadcastLoad(world);
		if(c.recoveryLinksRequested && tick%20==0) {
			int links=StfrLogicNetwork.connectedStabilizers(world,c.reactor);
			if(links!=c.previousRecoveryLinks) {c.previousRecoveryLinks=links;c.recoveryLinkProgress(world,links);}
		}
		c.particles(world, c.reactor.center, EnumParticleTypes.SMOKE_LARGE, 10, 0, 1);
		c.compatParticles(world, c.reactor.center, 2, 1, 0, 0);
		c.sound(world, "block.sculk.charge", SoundCategory.MASTER, 5F, 0F);
		if (c.recovering()) {
			c.recoveryTicks++;
			if (!StfrFailureTimeline.recoveryComplete(c.recoveryTicks)) return false;
			for (int i = 0; i < 4; i++) {
				world.playSound(
						null,
						c.reactor.center,
						STFRSounds.BEACON_POWER_SELECT,
						SoundCategory.AMBIENT,
						20F,
						0F);
				world.playSound(
						null,
						c.reactor.center,
						STFRSounds.HEAVY_SHUTDOWN,
						SoundCategory.AMBIENT,
						15F,
						1F);
			}
			c.broadcast(world, "restabilized", "restabilize", 1);
			StfrAdvancements.grant(world, c.reactor.center, "restabilization");
			c.shake(world, 80, 20, 10, 1);
			c.recoveryTicks = 0;
			return true;
		}
		if (tick == 481) c.broadcast(world, "shield_failure", "options", 4);
		if (tick == 482) c.broadcast(world, "calculating", "", 1);
		if (tick >= 481 && tick <= 493)
			c.sound(world, "alarms.notification", SoundCategory.MASTER, 8F, 2F);
		if (tick >= 483 && tick <= 493) {
			if (tick == 483)
				for (int i = 0; i < 3; i++)
					c.sound(world, "lightsswitch", SoundCategory.AMBIENT, 7F, 2F);
			if (tick == 483) c.broadcast(world, "instructions", "", 5, StfrConfig.recoveryCharge);
			if (tick >= 484)
				c.broadcast(world, "instruction_" + (tick - 484), "", 1, StfrConfig.recoveryCharge);
		}
		if (tick >= 783) {
			c.reactor.core().failureHeat();
			c.particles(world, c.reactor.center, EnumParticleTypes.FLAME, 20, 2, .1);
		}
		if (tick >= 483)
			for (EnumFacing face : EnumFacing.VALUES)
				c.particles(
						world,
						c.reactor.center.offset(face, 5),
						EnumParticleTypes.SMOKE_NORMAL,
						10,
						0,
						0);
		if (tick == 793) c.broadcast(world, "recovery_available", "output_stable", 1);
		String countdown = StfrFailureTimeline.countdown(tick);
		if (!countdown.isEmpty())
			c.broadcast(
					world,
					"countdown",
					"detonation_" + countdown,
					2,
					new TextComponentTranslation("message.hbmstfr.failure.time." + countdown));
		if (tick == StfrFailureTimeline.CASE_BREACH) c.caseBreach(world);
		if (tick >= 7090) c.compatParticles(world, c.reactor.center, 0, 12, 0, .2);
		return false;
	}

	static void caseBreach(StfrFailureEffects c, World world) {
		c.resolveProtection(world);
		if (c.allowed(STRUCTURE)) {c.glassBlowout(world);c.removeDestructible(world);}
		c.particles(world, c.reactor.center, EnumParticleTypes.SMOKE_NORMAL, 128, .5, .2);
		c.particles(world, c.reactor.center, EnumParticleTypes.SMOKE_LARGE, 64, .5, .1);
		c.particles(world, c.reactor.center, EnumParticleTypes.EXPLOSION_NORMAL, 10, 0, 10);
		for (float pitch : new float[] {0F, .6F, .7F, .8F, .9F})
			c.sound(world, "explosion.pressuregigaboom", SoundCategory.AMBIENT, 10F, pitch);
		for (float pitch : new float[] {0F, .6F, .7F})
			c.sound(world, "block.respawn_anchor.deplete", SoundCategory.AMBIENT, 8F, pitch);
		if (c.allowed(PLAYER_DAMAGE))
			for (EntityPlayer player :
					world.getEntitiesWithinAABB(
							EntityPlayer.class, new AxisAlignedBB(c.reactor.center).grow(14)))
				player.attackEntityFrom(DamageSource.GENERIC, 15F);
		c.blindness(world, 35);
		c.shake(world, 80, 100, 100, 5);
		c.broadcast(world, "case_breach", "", 5);
	}
}

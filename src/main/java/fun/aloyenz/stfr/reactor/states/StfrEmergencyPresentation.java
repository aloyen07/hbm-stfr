package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.logic.StfrMusicPacket;
import fun.aloyenz.stfr.logic.StfrVoicePacket;
import fun.aloyenz.stfr.reactor.StfrFailureEffects;
import fun.aloyenz.stfr.sound.StfrFailureSounds;
import fun.aloyenz.stfr.sound.StfrSceneSounds;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

/** Scene audio, localized announcements and bounded particle effects. */
final class StfrEmergencyPresentation {
	private StfrEmergencyPresentation() {}

	static void notice(StfrEmergencyController c, World w, String key) {
		if (!c.r.announcementMessages()) return;
		for (EntityPlayer player : w.playerEntities)
			if (player.getDistanceSq(c.r.center) <= 128D * 128D)
				player.sendMessage(new TextComponentTranslation("message.hbmstfr.recovery." + key));
	}

	static void burst(StfrEmergencyController c, World w) {
		c.burst(w, true);
	}

	static void burst(StfrEmergencyController c, World w, boolean damage) {
		if (damage) c.r.failure().blastDamage(w, c.r.center, 4F);
		c.particles(w, c.r.center, EnumParticleTypes.EXPLOSION_HUGE, 20, 1, 0);
		c.particles(w, c.r.center, EnumParticleTypes.FIREWORKS_SPARK, 1000, 1, 1);
		c.repeated(w, "explosion.shieldboom", 3, 24, 1);
		c.r.failure().blindness(w, 35);
		c.r.failure().shake(w, 80, 60, 100, 5);
	}

	static void cold(StfrEmergencyController c, World w, int tick) {

		c.particles(w, c.r.center, EnumParticleTypes.SNOW_SHOVEL, 12, .5, .05);
		c.rotorParticles(w, EnumParticleTypes.SNOW_SHOVEL, 10);
		if (tick % 16 == 0)
			c.r.mainframeSound(w, SoundEvents.BLOCK_NOTE_BELL, SoundCategory.MASTER, 5, 0);
		w.playSound(null, c.r.center, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.AMBIENT, 4, .5F);
		if (c.r.failure().allowsPhysical(w, StfrFailureEffects.PLAYER_DAMAGE))
			for (EntityPlayer p :
					w.getEntitiesWithinAABB(
							EntityPlayer.class, new AxisAlignedBB(c.r.center).grow(5)))
				p.attackEntityFrom(DamageSource.MAGIC, p.getDistanceSq(c.r.center) <= 9 ? 7 : 1);
	}

	static void plasma(StfrEmergencyController c, World w, int tick) {
		c.particles(w, c.r.center, EnumParticleTypes.CRIT_MAGIC, 12, 1, .2);
		double angle = tick * .09;
		BlockPos p =
				c.r.center.add(Math.cos(angle) * 3, Math.sin(angle * .7) * 3, Math.sin(angle) * 3);
		c.particles(w, p, EnumParticleTypes.FLAME, 10, .1, .1);
	}

	static void fire(StfrEmergencyController c, World w) {
		c.rotorParticles(w, EnumParticleTypes.FLAME, 10);
		if (c.r.failure().allowsPhysical(w, StfrFailureEffects.FIRE)
				&& c.r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE)) {
			EnumFacing side = EnumFacing.VALUES[w.rand.nextInt(6)];
			BlockPos p = c.r.center.offset(side, 4);
			if (w.isAirBlock(p) && Blocks.FIRE.canPlaceBlockAt(w, p))
				w.setBlockState(p, Blocks.FIRE.getDefaultState(), 3);
		}
	}

	static void rotorParticles(StfrEmergencyController c, World w, EnumParticleTypes particle, int count) {
		for (EnumFacing side : EnumFacing.VALUES)
			c.particles(w, c.r.center.offset(side, 9), particle, count, .7, 0);
	}

	static void particles(StfrEmergencyController c,
			World w, BlockPos p, EnumParticleTypes type, int count, double spread, double speed){
		if (w instanceof WorldServer)
			((WorldServer) w)
					.spawnParticle(
							type,
							true,
							p.getX() + .5,
							p.getY() + .5,
							p.getZ() + .5,
							count,
							spread,
							spread,
							spread,
							speed);
	}

	static void sound(StfrEmergencyController c, World w, String id, float volume, float pitch) {
		c.sound(w, id, SoundCategory.AMBIENT, volume, pitch);
	}

	static void sound(StfrEmergencyController c, World w, String id, SoundCategory category, float volume, float pitch) {
		SoundEvent event = StfrSceneSounds.get(id);
		if (event == null) event = StfrFailureSounds.get(id);
		if (event == null) return;
		if (id.startsWith("music.") || id.equals("braam"))
			for (EntityPlayer p : w.playerEntities) {
				if (p instanceof EntityPlayerMP && p.getDistanceSq(c.r.center) <= 128D * 128D)
					StfrMusicPacket.send(
							(EntityPlayerMP) p, c.r.center, StfrSceneSounds.location(id).toString(), pitch, true);
			}
		else if (id.startsWith("alarms.")) c.r.eventSound(w, event, category, volume, pitch);
		else w.playSound(null, c.r.center, event, category, volume, pitch);
	}

	static void repeated(StfrEmergencyController c, World w, String id, int times, float volume, float pitch) {
		for (int i = 0; i < times; i++) c.sound(w, id, volume, pitch);
	}

	static void music(StfrEmergencyController c, World w, String id, float volume, float pitch) {
		c.stopMusic(w);
		c.music = id;
		c.musicPitch = pitch;
		c.sound(w, id, volume, pitch);
		if (c.r.announcementMusic())
			for (EntityPlayer p : w.playerEntities)
				if (p.getDistanceSq(c.r.center) <= 128D * 128D) c.musicAudience.add(p.getUniqueID());
	}

	static void resumeMusic(StfrEmergencyController c, World w) {
		if (c.music.isEmpty()) return;
		SoundEvent event = StfrSceneSounds.get(c.music);
		if (event == null) return;
		c.musicAudience.removeIf(id -> w.getPlayerEntityByUUID(id) == null);
		for (EntityPlayer p : w.playerEntities)
			if (p instanceof EntityPlayerMP
					&& p.getDistanceSq(c.r.center) <= 128D * 128D
					&& c.musicAudience.add(p.getUniqueID()))
				StfrMusicPacket.send(
						(EntityPlayerMP) p, c.r.center, StfrSceneSounds.location(c.music).toString(), c.musicPitch, true);
	}

	static void stopMusic(StfrEmergencyController c, World w) {
		for (String id : StfrSceneSounds.musicIds())
			c.r.failure().stop(w, StfrSceneSounds.location(id).toString(), 512);
		c.music = "";
		c.musicAudience.clear();
	}

	static void broadcast(StfrEmergencyController c, World w, String voice, String text, int level) {
		c.broadcast(w, voice, text, level, StfrSceneText.key(voice, text));
	}

	static void broadcast(StfrEmergencyController c, World w, String voice, String text, int level, String key) {
		if (text.isEmpty()) return;
		String rendered =
				text.replace(
								"Replace 4 Stabilizer Lenses (Redstone Lamps) With Amethyst Blocks,"
									+ " Then Confirming Via. SSTB",
								"Install At Least 4 Auxiliary Quartz Lenses, Then Confirm"
									+ " Via SSTB")
						.replace("Amethyst", "Quartz")
						.replace("amethyst", "quartz")
						.replace("Stasis Laser Activation", "Unavailable: Stasis Laser");
		TextFormatting color =
				c.r.runState() == REACTION_LOSS || c.r.runState() == REIGNITION
						? TextFormatting.AQUA
						: level >= 3
								? TextFormatting.RED
								: level >= 1 ? TextFormatting.GOLD : TextFormatting.AQUA;
		ITextComponent content =
				key.isEmpty()
						? new TextComponentString(rendered)
						: new TextComponentTranslation(key);
		if (text.startsWith("Monitoring Error! CODE: "))
			content =
					new TextComponentTranslation(
							"message.hbmstfr.scene.monitoring_error",
							text.substring("Monitoring Error! CODE: ".length()));
		content.getStyle().setColor(color);
		for (EntityPlayer p : w.playerEntities)
			if (c.r.announcementMessages() && p.getDistanceSq(c.r.center) <= 128D * 128D)
				p.sendMessage(
						new TextComponentString("[STFR] ").appendSibling(content.createCopy()));
		if (level > 0) c.sound(w, "alarms.notification", 4, 1);
		c.r.failure().recordBroadcast(w);
		if (!key.isEmpty()) StfrVoicePacket.send(w, c.r.center, key);
	}
}

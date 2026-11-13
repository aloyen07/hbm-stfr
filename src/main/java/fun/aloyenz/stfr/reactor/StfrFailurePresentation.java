package fun.aloyenz.stfr.reactor;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrMusicPacket;
import fun.aloyenz.stfr.logic.StfrShakePacket;
import fun.aloyenz.stfr.logic.StfrVoicePacket;
import fun.aloyenz.stfr.sound.StfrFailureSounds;
import fun.aloyenz.stfr.sound.StfrMusicPolicy;

/** Failure announcements, audio delivery and camera feedback. */
final class StfrFailurePresentation {
	private StfrFailurePresentation() {}

	static void sound(StfrFailureEffects c, World world, String id, SoundCategory category, float volume, float pitch) {
		if(category==SoundCategory.RECORDS && !StfrMusicPolicy.allowed(c.reactor.runState()))return;
		SoundEvent sound = StfrFailureSounds.get(id);
		if (sound == null) return;
		if (id.startsWith("music.") || category == SoundCategory.RECORDS) {
			int radius = 128;
			for (EntityPlayer player : world.playerEntities)
				if (player instanceof EntityPlayerMP
						&& player.getDistanceSq(c.reactor.center) <= radius * (double) radius)
					StfrMusicPacket.send(
							(EntityPlayerMP) player,
							c.reactor.center,
							"hbmstfr:failure." + id,
							pitch,
							true);
		} else if (id.startsWith("alarms."))
			c.reactor.eventSound(world, sound, category, volume, pitch);
		else world.playSound(null, c.reactor.center, sound, category, volume, pitch);
	}

	static void stop(StfrFailureEffects c, World world, String soundId, int radius) {
		if (soundId == null || soundId.isEmpty()) return;
		for (EntityPlayer player : world.playerEntities)
			if (player instanceof EntityPlayerMP)
				StfrMusicPacket.send((EntityPlayerMP) player, c.reactor.center, soundId, 1, false);
		for (EntityPlayer player : world.playerEntities)
			if (player instanceof EntityPlayerMP
					&& player.getDistanceSq(c.reactor.center) <= radius * (double) radius) {
				PacketBuffer data = new PacketBuffer(Unpooled.buffer());
				data.writeString("");
				data.writeString(soundId);
				((EntityPlayerMP) player)
						.connection.sendPacket(new SPacketCustomPayload("MC|StopSound", data));
			}
	}

	static void broadcast(StfrFailureEffects c, World world, String event, String voice, int level, Object... args) {
		for (EntityPlayer player : world.playerEntities)
			if (c.reactor.announcementMessages()
					&& player.getDistanceSq(c.reactor.center) <= 128D * 128D)
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.failure." + event, args)
								.setStyle(
										new Style()
												.setColor(
														level >= 4
																? TextFormatting.DARK_RED
																: TextFormatting.AQUA)));
		if (level > 0)
			c.sound(world, "alarms.notification", SoundCategory.AMBIENT, 4F, 1F);
		// Old fixed-language speech is replaced by the localized voice packet.
		String voiceKey = "message.hbmstfr.failure." + event;
		if ("countdown".equals(event) && voice.startsWith("detonation_"))
			voiceKey += "." + voice.substring("detonation_".length());
		StfrVoicePacket.send(world, c.reactor.center, voiceKey);
		c.broadcastLoad += 10;
		if (c.broadcastLoad >= 300) c.crashMainframes(world, 1300);
	}

	static void recordBroadcast(StfrFailureEffects c, World world) {
		for (EntityPlayer p : world.playerEntities)
			if (p instanceof EntityPlayerMP && p.getDistanceSq(c.reactor.center) <= 128D * 128D) {
				PacketBuffer data = new PacketBuffer(Unpooled.buffer());
				data.writeString("voice");
				data.writeString("");
				((EntityPlayerMP) p)
						.connection.sendPacket(new SPacketCustomPayload("MC|StopSound", data));
			}
		c.broadcastLoad += 10;
		if (c.broadcastLoad >= 300) c.crashMainframes(world, 1300);
	}

	static void blindness(StfrFailureEffects c, World world, int radius) {
		for (EntityPlayer player : world.playerEntities)
			if (player.getDistanceSq(c.reactor.center) <= radius * (double) radius)
				player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 20, 0, false, false));
	}

	static void fadeShake(StfrFailureEffects c, World world, int radius, int ticks) {
		StfrLogicPacket.CHANNEL.sendToAllAround(
				new StfrShakePacket(c.reactor.center, ticks),
				new NetworkRegistry.TargetPoint(
						world.provider.getDimension(),
						c.reactor.center.getX(),
						c.reactor.center.getY(),
						c.reactor.center.getZ(),
						radius));
	}

	static void shake(StfrFailureEffects c, World world, int radius, int ticks, int frequency, int magnitude) {
		c.shake(world, radius, ticks, frequency, magnitude, false);
	}

	static void shake(StfrFailureEffects c, World world, int radius, int ticks, int frequency, int magnitude, boolean sustained) {
		StfrLogicPacket.CHANNEL.sendToAllAround(
				new StfrShakePacket(c.reactor.center, ticks, frequency, magnitude, sustained),
				new NetworkRegistry.TargetPoint(
						world.provider.getDimension(),
						c.reactor.center.getX(),
						c.reactor.center.getY(),
						c.reactor.center.getZ(),
						radius));
	}
}

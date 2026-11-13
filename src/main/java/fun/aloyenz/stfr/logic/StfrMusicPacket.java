package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.client.StfrClientMusic;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.sound.StfrMusicPolicy;

/** Reactor music is a non-positional client sound; equipment still uses physical alarm sources. */
public final class StfrMusicPacket implements IMessage {

	private BlockPos center;
	private String sound;
	private float pitch;
	private boolean play;

	public StfrMusicPacket() {}

	public StfrMusicPacket(BlockPos center, String sound, float pitch, boolean play) {
		this.center = center;
		this.sound = sound;
		this.pitch = pitch;
		this.play = play;
	}

	public static void send(
			EntityPlayerMP player, BlockPos center, String sound, float pitch, boolean play) {
		if (sound != null && !sound.isEmpty())
		{
			StfrReactor reactor = StfrManager.get(player.world).get(center);
			if (play && reactor != null && (!reactor.announcementMusic()
                    || !StfrMusicPolicy.allowed(reactor.runState()))) return;
			StfrLogicPacket.CHANNEL.sendTo(new StfrMusicPacket(center, sound, pitch, play), player);
		}
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(center.toLong());
		ByteBufUtils.writeUTF8String(buf, sound);
		buf.writeFloat(pitch);
		buf.writeBoolean(play);
	}

	public void fromBytes(ByteBuf buf) {
		center = BlockPos.fromLong(buf.readLong());
		sound = ByteBufUtils.readUTF8String(buf);
		pitch = buf.readFloat();
		play = buf.readBoolean();
	}

	public static final class Handler implements IMessageHandler<StfrMusicPacket, IMessage> {

		public IMessage onMessage(StfrMusicPacket packet, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() ->
									StfrClientMusic.receive(
											packet.center,
											packet.sound,
											packet.pitch,
											packet.play));
			return null;
		}
	}
}

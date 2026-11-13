package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Settings belong to the reactor resolved through this alarm's authorized cable network. */
public final class StfrAlarmSettingsPacket implements IMessage {
	private BlockPos pos;
	private boolean messages, voice, music;

	public StfrAlarmSettingsPacket() {}

	public StfrAlarmSettingsPacket(BlockPos pos, boolean messages, boolean voice, boolean music) {
		this.pos = pos;
		this.messages = messages;
		this.voice = voice;
		this.music = music;
	}

	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		messages = buf.readBoolean();
		voice = buf.readBoolean();
		music = buf.readBoolean();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeBoolean(messages);
		buf.writeBoolean(voice);
		buf.writeBoolean(music);
	}

	public static final class Handler implements IMessageHandler<StfrAlarmSettingsPacket, IMessage> {
		public IMessage onMessage(StfrAlarmSettingsPacket packet, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				if (player.getDistanceSq(packet.pos) > 64
						|| !StfrSecurity.allows(player, player.world, packet.pos,
								StfrAccessPolicy.CONTROL)) return;
				TileEntity tile = player.world.getTileEntity(packet.pos);
				if (!(tile instanceof TileEntityLogicDevice)
						|| ((TileEntityLogicDevice) tile).kind() != BlockLogicDevice.Kind.ALARM)
					return;
				StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(player.world, packet.pos);
				if (!network.ioAvailable || !network.deviceAvailable(packet.pos)
						|| network.reactor == null) return;
				StfrReactor reactor = network.reactor;
				boolean wasMusic = reactor.announcementMusic();
				reactor.setAnnouncementOptions(packet.messages, packet.voice, packet.music);
				if (wasMusic && !packet.music)
					for (EntityPlayerMP listener : player.getServerWorld().getPlayers(EntityPlayerMP.class,
							candidate -> true))
						StfrMusicPacket.send(listener, reactor.center, "*", 1F, false);
				if (!packet.voice) StfrVoicePacket.stop(player.world, reactor.center);
				if (wasMusic != packet.music) {
					reactor.emergency().clearMusicAudience();
					reactor.clearStartupMusicAudience();
				}
				StfrManager.get(player.world).markDirty();
			});
			return null;
		}
	}
}

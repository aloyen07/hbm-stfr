package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.client.StfrClientVoice;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.sound.StfrVoiceCatalog;
import fun.aloyenz.stfr.sound.StfrVoiceRange;

/** Sends an announcement ID so each listener can select a recording in their own language. */
public final class StfrVoicePacket implements IMessage {

	private BlockPos center;
	private String key;
	private BlockPos source;

	public StfrVoicePacket() {}

	private StfrVoicePacket(BlockPos center, BlockPos source, String key) {
		this.center = center;
		this.key = key;
		this.source = source;
	}

	public static void send(World world, BlockPos center, String key) {
		if (world.isRemote || !StfrVoiceCatalog.contains(key)) return;
		StfrReactor reactor = StfrManager.get(world).get(center);
		if (reactor != null && !reactor.announcementVoice()) return;
		List<BlockPos> sources=reactor==null?Collections.singletonList(center):reactor.announcementSources(world);
		for(EntityPlayer player:world.playerEntities) {
			if(!(player instanceof EntityPlayerMP))continue;
			BlockPos closest=StfrVoiceRange.closest(player.getPositionEyes(1),sources);
			if(closest!=null)StfrLogicPacket.CHANNEL.sendTo(new StfrVoicePacket(center,closest,key),(EntityPlayerMP)player);
		}
	}

	public static void stop(World world, BlockPos center) {
		StfrVoicePacket packet = new StfrVoicePacket(center, center, "");
		for (EntityPlayer player : world.playerEntities)
			if (player instanceof EntityPlayerMP)
				StfrLogicPacket.CHANNEL.sendTo(packet, (EntityPlayerMP) player);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		center = BlockPos.fromLong(buf.readLong());
		key = ByteBufUtils.readUTF8String(buf);
		source = BlockPos.fromLong(buf.readLong());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(center.toLong());
		ByteBufUtils.writeUTF8String(buf, key);
		buf.writeLong(source.toLong());
	}

	public static final class Handler implements IMessageHandler<StfrVoicePacket, IMessage> {
		@Override
		public IMessage onMessage(StfrVoicePacket packet, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(() -> {
						if (packet.key.isEmpty()) StfrClientVoice.stop(packet.center);
						else StfrClientVoice.play(packet.center, packet.source, packet.key);
					});
			return null;
		}
	}
}

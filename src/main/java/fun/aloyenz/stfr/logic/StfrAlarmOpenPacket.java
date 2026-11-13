package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.client.gui.GuiStfrAlarm;

public final class StfrAlarmOpenPacket implements IMessage {
	private BlockPos pos;
	private boolean messages, voice, music;

	public StfrAlarmOpenPacket() {}

	public StfrAlarmOpenPacket(BlockPos pos, boolean messages, boolean voice, boolean music) {
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

	public static final class Handler implements IMessageHandler<StfrAlarmOpenPacket, IMessage> {
		public IMessage onMessage(StfrAlarmOpenPacket packet, MessageContext context) {
			Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft()
					.displayGuiScreen(new GuiStfrAlarm(packet.pos, packet.messages,
							packet.voice, packet.music)));
			return null;
		}
	}
}

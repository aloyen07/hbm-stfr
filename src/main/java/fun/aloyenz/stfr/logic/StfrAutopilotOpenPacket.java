package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.client.gui.GuiAutopilot;

public final class StfrAutopilotOpenPacket implements IMessage {

	private BlockPos pos;
	private long threshold;
	private int level;

	public StfrAutopilotOpenPacket() {}

	public StfrAutopilotOpenPacket(BlockPos pos, long threshold, int level) {
		this.pos = pos;
		this.threshold = threshold;
		this.level = level;
	}

	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		threshold = buf.readLong();
		level = buf.readUnsignedByte();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeLong(threshold);
		buf.writeByte(level);
	}

	public static final class Handler
			implements IMessageHandler<StfrAutopilotOpenPacket, IMessage> {
		public IMessage onMessage(StfrAutopilotOpenPacket message, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() ->
									Minecraft.getMinecraft()
											.displayGuiScreen(
													new GuiAutopilot(
															message.pos,
															message.threshold,
															message.level)));
			return null;
		}
	}
}

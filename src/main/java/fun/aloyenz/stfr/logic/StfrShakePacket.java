package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.client.render.StfrClientVisuals;

public final class StfrShakePacket implements IMessage {

	private BlockPos pos;
	private int ticks, frequency, magnitude;
	private boolean fade;
	private boolean sustained;

	public StfrShakePacket() {}

	public StfrShakePacket(BlockPos pos, int ticks, int frequency, int magnitude) {
		this.pos = pos;
		this.ticks = ticks;
		this.frequency = frequency;
		this.magnitude = magnitude;
	}

	public StfrShakePacket(BlockPos pos, int ticks) {
		this.pos = pos;
		this.ticks = ticks;
		this.fade = true;
	}

	public StfrShakePacket(BlockPos pos, int ticks, int frequency, int magnitude, boolean sustained) {
		this(pos,ticks,frequency,magnitude);
		this.sustained=sustained;
	}

	public void fromBytes(ByteBuf b) {
		pos = BlockPos.fromLong(b.readLong());
		ticks = b.readInt();
		frequency = b.readInt();
		magnitude = b.readInt();
		fade = b.readBoolean();
		sustained = b.isReadable() && b.readBoolean();
	}

	public void toBytes(ByteBuf b) {
		b.writeLong(pos.toLong());
		b.writeInt(ticks);
		b.writeInt(frequency);
		b.writeInt(magnitude);
		b.writeBoolean(fade);
		b.writeBoolean(sustained);
	}

	public static final class Handler implements IMessageHandler<StfrShakePacket, IMessage> {

		public IMessage onMessage(StfrShakePacket m, MessageContext ctx) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								if (m.fade) StfrClientVisuals.fadeShake(m.pos, m.ticks);
								else
									StfrClientVisuals.shake(
											m.pos, m.ticks, m.frequency, m.magnitude, m.sustained);
							});
			return null;
		}
	}
}

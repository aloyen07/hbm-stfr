package fun.aloyenz.stfr.security;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public final class StfrSecurityActionPacket implements IMessage {

	private int window, action;
	private UUID revoke;

	public StfrSecurityActionPacket() {}

	public StfrSecurityActionPacket(int window, int action) {
		this.window = window;
		this.action = action;
	}

	public StfrSecurityActionPacket(int window, int action, UUID revoke) {
		this(window, action);
		this.revoke = revoke;
	}

	public void fromBytes(ByteBuf buf) {
		window = buf.readInt();
		action = buf.readUnsignedByte();
		if (buf.readBoolean()) revoke = new UUID(buf.readLong(), buf.readLong());
	}

	public void toBytes(ByteBuf buf) {
		buf.writeInt(window);
		buf.writeByte(action);
		buf.writeBoolean(revoke != null);
		if (revoke != null) {
			buf.writeLong(revoke.getMostSignificantBits());
			buf.writeLong(revoke.getLeastSignificantBits());
		}
	}

	public static final class Handler
			implements IMessageHandler<StfrSecurityActionPacket, IMessage> {
		public IMessage onMessage(StfrSecurityActionPacket m, MessageContext ctx) {
			EntityPlayerMP p = ctx.getServerHandler().player;
			p.getServerWorld()
					.addScheduledTask(
							() -> {
								if (p.openContainer instanceof ContainerStfrSecurity
										&& p.openContainer.windowId == m.window)
									((ContainerStfrSecurity) p.openContainer)
											.act(p, m.action, m.revoke);
							});
			return null;
		}
	}
}

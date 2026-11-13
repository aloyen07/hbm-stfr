package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.inventory.ContainerSolidInjector;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;

public final class StfrInjectorConfigPacket implements IMessage {

	private int window, dose, interval, mode;

	public StfrInjectorConfigPacket() {}

	public StfrInjectorConfigPacket(int window, int dose, int interval, int mode) {
		this.window = window;
		this.dose = dose;
		this.interval = interval;
		this.mode = mode;
	}

	@Override
	public void fromBytes(ByteBuf b) {
		window = b.readInt();
		dose = b.readInt();
		interval = b.readInt();
		mode = b.readInt();
	}

	@Override
	public void toBytes(ByteBuf b) {
		b.writeInt(window);
		b.writeInt(dose);
		b.writeInt(interval);
		b.writeInt(mode);
	}

	public static final class Handler
			implements IMessageHandler<StfrInjectorConfigPacket, IMessage> {
		@Override
		public IMessage onMessage(StfrInjectorConfigPacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!(player.openContainer instanceof ContainerSolidInjector)
										|| player.openContainer.windowId != message.window) return;
								ContainerSolidInjector container =
										(ContainerSolidInjector) player.openContainer;
								if (!container.canInteractWith(player)
										|| message.dose < 1
										|| message.dose > TileEntitySolidInjector.CAPACITY
										|| message.interval < 1
										|| message.interval > TileEntitySolidInjector.MAX_INTERVAL
										|| message.mode < 0
										|| message.mode > 2) return;
								container.tile.configure(
										message.dose, message.interval, message.mode);
								container.detectAndSendChanges();
							});
			return null;
		}
	}
}

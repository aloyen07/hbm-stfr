package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.reactor.StfrAutopilot;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Configuration is a normal CONTROL action; redstone operation needs no player. */
public final class StfrAutopilotPacket implements IMessage {

	private BlockPos pos;
	private long threshold;
	private boolean forceShutdown;

	public StfrAutopilotPacket() {}

	public StfrAutopilotPacket(BlockPos pos, long threshold) {
		this.pos = pos;
		this.threshold = threshold;
	}

	public static StfrAutopilotPacket forceShutdown(BlockPos pos) {
		StfrAutopilotPacket packet = new StfrAutopilotPacket(pos, 0L);
		packet.forceShutdown = true;
		return packet;
	}

	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		threshold = buf.readLong();
		forceShutdown = buf.readBoolean();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeLong(threshold);
		buf.writeBoolean(forceShutdown);
	}

	public static final class Handler implements IMessageHandler<StfrAutopilotPacket, IMessage> {

		public IMessage onMessage(StfrAutopilotPacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (player.getDistanceSq(message.pos) > 64
										|| !StfrSecurity.allows(
												player,
												player.world,
												message.pos,
												StfrAccessPolicy.CONTROL)) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (!(tile instanceof TileEntityLogicDevice)
										|| ((TileEntityLogicDevice) tile).kind()
												!= BlockLogicDevice.Kind.AUTOPILOT_3) return;
								TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
								if (message.forceShutdown)
									StfrAutopilot.requestForceShutdown(player.world, device);
								else device.setAutopilotFuelThreshold(message.threshold);
							});
			return null;
		}
	}
}

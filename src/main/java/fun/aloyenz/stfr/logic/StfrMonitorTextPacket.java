package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Server-authoritative sign edit, including visible-length validation. */
public final class StfrMonitorTextPacket implements IMessage {

	private BlockPos pos;
	private final String[] lines = new String[4];

	public StfrMonitorTextPacket() {}

	public StfrMonitorTextPacket(BlockPos pos, String[] lines) {
		this.pos = pos;
		System.arraycopy(lines, 0, this.lines, 0, 4);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		for (int i = 0; i < 4; i++) lines[i] = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		for (String line : lines) ByteBufUtils.writeUTF8String(buf, line);
	}

	public static final class Handler implements IMessageHandler<StfrMonitorTextPacket, IMessage> {

		@Override
		public IMessage onMessage(StfrMonitorTextPacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!StfrSecurity.allows(
										player,
										player.world,
										message.pos,
										StfrAccessPolicy.CONTROL)) return;

								if (player.getDistanceSq(message.pos) > 64) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (!(tile instanceof TileEntityLogicDevice)) return;
								TileEntityLogicDevice monitor = (TileEntityLogicDevice) tile;
								if (monitor.kind() != BlockLogicDevice.Kind.MONITOR) return;
								monitor.setMonitorText(message.lines);
							});
			return null;
		}
	}
}

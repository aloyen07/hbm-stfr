package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Only the signal modifier can select a graphical monitor mode. */
public final class StfrMonitorDisplayPacket implements IMessage {

	private BlockPos pos;
	private int mode;

	public StfrMonitorDisplayPacket() {}

	public StfrMonitorDisplayPacket(BlockPos pos, StfrMonitorDisplay mode) {
		this.pos = pos;
		this.mode = mode.ordinal();
	}

	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		mode = buf.readUnsignedByte();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(mode);
	}

	public static final class Handler
			implements IMessageHandler<StfrMonitorDisplayPacket, IMessage> {
		public IMessage onMessage(StfrMonitorDisplayPacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!StfrSecurity.allows(
										player,
										player.world,
										message.pos,
										StfrAccessPolicy.CONTROL)) return;

								if (message.mode >= StfrMonitorDisplay.values().length
										|| player.getDistanceSq(message.pos) > 64
										|| !player.world.isBlockLoaded(message.pos)
										|| player.getHeldItemMainhand().getItem()
														!= STFRItems.SIGNAL_MODIFIER
												&& player.getHeldItemOffhand().getItem()
														!= STFRItems.SIGNAL_MODIFIER) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (tile instanceof TileEntityLogicDevice
										&& ((TileEntityLogicDevice) tile).kind()
												== BlockLogicDevice.Kind.MONITOR)
									((TileEntityLogicDevice) tile)
											.setMonitorDisplay(
													StfrMonitorDisplay.values()[message.mode]);
							});
			return null;
		}
	}
}

package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.client.gui.GuiCrashSuppressor;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Server verifies creative privileges, reach and block type for every configuration edit. */
public final class StfrSuppressorPacket implements IMessage {

	public BlockPos pos;
	public int mask;

	public StfrSuppressorPacket() {}

	public StfrSuppressorPacket(BlockPos pos, int mask) {
		this.pos = pos;
		this.mask = mask;
	}

	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		mask = buf.readInt();
	}

	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeInt(mask);
	}

	public static final class Server implements IMessageHandler<StfrSuppressorPacket, IMessage> {

		public IMessage onMessage(StfrSuppressorPacket message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!StfrSecurity.allows(
										player,
										player.world,
										message.pos,
										StfrAccessPolicy.CONTROL)) return;

								if (!player.capabilities.isCreativeMode
										|| player.getDistanceSq(message.pos) > 64
										|| !player.world.isBlockLoaded(message.pos)) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (!(tile instanceof TileEntityLogicDevice)
										|| ((TileEntityLogicDevice) tile).kind()
												!= BlockLogicDevice.Kind.CRASH_SUPPRESSOR) return;
								((TileEntityLogicDevice) tile).setSuppressionMask(message.mask);
								StfrLogicPacket.CHANNEL.sendTo(
										new StfrSuppressorPacket(
												message.pos,
												((TileEntityLogicDevice) tile).suppressionMask()),
										player);
							});
			return null;
		}
	}

	public static final class Client implements IMessageHandler<StfrSuppressorPacket, IMessage> {

		public IMessage onMessage(StfrSuppressorPacket message, MessageContext ctx) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								Minecraft mc = Minecraft.getMinecraft();
								if (mc.currentScreen instanceof GuiCrashSuppressor
										&& ((GuiCrashSuppressor) mc.currentScreen)
												.pos()
												.equals(message.pos))
									((GuiCrashSuppressor) mc.currentScreen).update(message.mask);
								else
									mc.displayGuiScreen(
											new GuiCrashSuppressor(message.pos, message.mask));
							});
			return null;
		}
	}
}

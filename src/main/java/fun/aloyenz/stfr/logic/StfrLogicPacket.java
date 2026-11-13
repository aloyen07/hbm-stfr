package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.security.StfrSecurityActionPacket;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public final class StfrLogicPacket implements IMessage {

	public static final SimpleNetworkWrapper CHANNEL =
			NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID + "_logic");
	private BlockPos pos;
	private int face;
	private int signal;
	private int repeatInterval;

	public static StfrLogicPacket repeat(BlockPos pos, EnumFacing face, int ticks) {
		StfrLogicPacket packet = new StfrLogicPacket(pos, face, StfrSignal.NONE);
		packet.signal = 253;
		packet.repeatInterval = Math.max(0, ticks);
		return packet;
	}

	public StfrLogicPacket() {}

	public StfrLogicPacket(BlockPos pos, EnumFacing face, StfrSignal signal) {
		this.pos = pos;
		this.face = face.getIndex();
		this.signal = signal.ordinal();
	}

	private StfrLogicPacket(BlockPos pos, boolean inline) {
		this.pos = pos;
		this.face = 0;
		this.signal = inline ? 255 : 254;
	}

	public static StfrLogicPacket mode(BlockPos pos, boolean inline) {
		return new StfrLogicPacket(pos, inline);
	}

	public static void register() {
		CHANNEL.registerMessage(
				StfrInjectorConfigPacket.Handler.class,
				StfrInjectorConfigPacket.class,
				16,
				Side.SERVER);
		CHANNEL.registerMessage(
				StfrAutopilotPacket.Handler.class, StfrAutopilotPacket.class, 13, Side.SERVER);
		CHANNEL.registerMessage(StfrAlarmSettingsPacket.Handler.class,
				StfrAlarmSettingsPacket.class, 18, Side.SERVER);
		CHANNEL.registerMessage(
				StfrSecurityActionPacket.Handler.class,
				StfrSecurityActionPacket.class,
				11,
				Side.SERVER);
		CHANNEL.registerMessage(Handler.class, StfrLogicPacket.class, 0, Side.SERVER);
		CHANNEL.registerMessage(
				StfrMonitorTextPacket.Handler.class, StfrMonitorTextPacket.class, 2, Side.SERVER);
		CHANNEL.registerMessage(
				StfrMainframePacket.Handler.class, StfrMainframePacket.class, 3, Side.SERVER);
		CHANNEL.registerMessage(
				StfrSuppressorPacket.Server.class, StfrSuppressorPacket.class, 7, Side.SERVER);
		CHANNEL.registerMessage(
				StfrMonitorDisplayPacket.Handler.class,
				StfrMonitorDisplayPacket.class,
				10,
				Side.SERVER);
		if (FMLCommonHandler.instance().getSide().isClient()) {
			StfrClientPackets.register();
		} else {
			// The server must know client-bound discriminators to encode outbound packets,
			// but must not instantiate handlers which reference Minecraft client classes.
			CHANNEL.registerMessage(
					LogicSyncClientStub.class, StfrLogicSyncPacket.class, 1, Side.CLIENT);
			CHANNEL.registerMessage(
					MainframeOpenClientStub.class, StfrMainframeOpenPacket.class, 4, Side.CLIENT);
			CHANNEL.registerMessage(
					RotorSyncClientStub.class, StfrRotorSyncPacket.class, 5, Side.CLIENT);
			CHANNEL.registerMessage(ShakeClientStub.class, StfrShakePacket.class, 6, Side.CLIENT);
			CHANNEL.registerMessage(
					SuppressorClientStub.class, StfrSuppressorPacket.class, 8, Side.CLIENT);
			CHANNEL.registerMessage(
					ParticleClientStub.class, StfrCompatParticlePacket.class, 9, Side.CLIENT);
			CHANNEL.registerMessage(MusicClientStub.class, StfrMusicPacket.class, 12, Side.CLIENT);
			CHANNEL.registerMessage(CoreFlashClientStub.class,StfrCoreFlashPacket.class,20,Side.CLIENT);
			CHANNEL.registerMessage(VoiceClientStub.class, StfrVoicePacket.class, 17, Side.CLIENT);
			CHANNEL.registerMessage(AlarmOpenClientStub.class, StfrAlarmOpenPacket.class, 19, Side.CLIENT);
			CHANNEL.registerMessage(
					AutopilotOpenClientStub.class, StfrAutopilotOpenPacket.class, 14, Side.CLIENT);
			CHANNEL.registerMessage(
					DestabilizerOpenClientStub.class,
					StfrDestabilizerOpenPacket.class,
					15,
					Side.CLIENT);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		face = buf.readUnsignedByte();
		signal = buf.readUnsignedByte();
		if (signal == 253) repeatInterval = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(face);
		buf.writeByte(signal);
		if (signal == 253) buf.writeInt(repeatInterval);
	}

	public static final class Handler implements IMessageHandler<StfrLogicPacket, IMessage> {

		@Override
		public IMessage onMessage(StfrLogicPacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!StfrSecurity.allows(
										player,
										player.world,
										message.pos,
										StfrAccessPolicy.CONTROL)) return;

								if (message.face < 0
										|| message.face >= 6
										|| player.getDistanceSq(message.pos) > 64
										|| player.getHeldItemMainhand().getItem()
														!= STFRItems.SIGNAL_MODIFIER
												&& player.getHeldItemOffhand().getItem()
														!= STFRItems.SIGNAL_MODIFIER) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (!(tile instanceof TileEntityLogicDevice)) return;
								TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
								if (device.kind() != BlockLogicDevice.Kind.TRANSMITTER) return;
								if (message.signal == 253) {
									if (message.repeatInterval >= 0)
										device.setRepeatInterval(
												EnumFacing.byIndex(message.face),
												message.repeatInterval);
									return;
								}
								if (message.signal < 0
										|| message.signal >= StfrSignal.values().length) return;
								StfrSignal signal = StfrSignal.values()[message.signal];
								if (!signal.availableFor(device.kind())) return;
								device.setSignal(EnumFacing.byIndex(message.face), signal);
							});
			return null;
		}
	}

	public abstract static class ClientStub<T extends IMessage>
			implements IMessageHandler<T, IMessage> {
		@Override
		public IMessage onMessage(T message, MessageContext context) {
			return null;
		}
	}

	public static final class LogicSyncClientStub extends ClientStub<StfrLogicSyncPacket> {}

	public static final class MainframeOpenClientStub extends ClientStub<StfrMainframeOpenPacket> {}

	public static final class RotorSyncClientStub extends ClientStub<StfrRotorSyncPacket> {}

	public static final class ShakeClientStub extends ClientStub<StfrShakePacket> {}

	public static final class SuppressorClientStub extends ClientStub<StfrSuppressorPacket> {}

	public static final class ParticleClientStub extends ClientStub<StfrCompatParticlePacket> {}

	public static final class MusicClientStub extends ClientStub<StfrMusicPacket> {}

	public static final class CoreFlashClientStub extends ClientStub<StfrCoreFlashPacket> {}

	public static final class VoiceClientStub extends ClientStub<StfrVoicePacket> {}
	public static final class AlarmOpenClientStub extends ClientStub<StfrAlarmOpenPacket> {}

	public static final class AutopilotOpenClientStub extends ClientStub<StfrAutopilotOpenPacket> {}

	public static final class DestabilizerOpenClientStub
			extends ClientStub<StfrDestabilizerOpenPacket> {}
}

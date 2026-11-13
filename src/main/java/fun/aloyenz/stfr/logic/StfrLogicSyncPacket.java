package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.client.gui.GuiMonitorDisplay;
import fun.aloyenz.stfr.client.gui.GuiMonitorEditor;
import fun.aloyenz.stfr.client.gui.GuiSignalModifier;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Authoritative menu snapshot sent when the modifier is used. */
public final class StfrLogicSyncPacket implements IMessage {

	private BlockPos pos;
	private int face;
	private int kind;
	private boolean inline;
	private String displayMode;
	private final int[] signals = new int[6];
	private final int[] repeatIntervals = new int[6];
	private final String[] lines = new String[4];

	public StfrLogicSyncPacket() {}

	public StfrLogicSyncPacket(BlockPos pos, EnumFacing face, TileEntityLogicDevice device) {
		this.pos = pos;
		this.face = face.getIndex();
		this.kind = device.kind().ordinal();
		this.inline = device.inlineMode();
		this.displayMode = device.monitorDisplay().name();
		for (EnumFacing side : EnumFacing.VALUES)
			signals[side.getIndex()] = device.signal(side).ordinal();
		for (EnumFacing side : EnumFacing.VALUES)
			repeatIntervals[side.getIndex()] = device.repeatInterval(side);
		for (int i = 0; i < 4; i++) lines[i] = device.templateLine(i);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		face = buf.readUnsignedByte();
		kind = buf.readUnsignedByte();
		inline = buf.readBoolean();
		displayMode = ByteBufUtils.readUTF8String(buf);
		for (int i = 0; i < 6; i++) signals[i] = buf.readUnsignedByte();
		for (int i = 0; i < 6; i++) repeatIntervals[i] = buf.readInt();
		for (int i = 0; i < 4; i++) lines[i] = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(face);
		buf.writeByte(kind);
		buf.writeBoolean(inline);
		ByteBufUtils.writeUTF8String(buf, displayMode);
		for (int signal : signals) buf.writeByte(signal);
		for (int interval : repeatIntervals) buf.writeInt(interval);
		for (String line : lines) ByteBufUtils.writeUTF8String(buf, line);
	}

	public static final class Handler implements IMessageHandler<StfrLogicSyncPacket, IMessage> {

		@Override
		public IMessage onMessage(StfrLogicSyncPacket message, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								if (message.face >= 6
										|| message.kind >= BlockLogicDevice.Kind.values().length)
									return;
								if (BlockLogicDevice.Kind.values()[message.kind]
										== BlockLogicDevice.Kind.MONITOR) {
									EntityPlayer player = Minecraft.getMinecraft().player;
									if (player != null
											&& (player.getHeldItemMainhand().getItem()
															== STFRItems.SIGNAL_MODIFIER
													|| player.getHeldItemOffhand().getItem()
															== STFRItems.SIGNAL_MODIFIER)) {
										Minecraft.getMinecraft()
												.displayGuiScreen(
														new GuiMonitorDisplay(
																message.pos,
																StfrMonitorDisplay.read(
																		message.displayMode)));
										return;
									}
									Minecraft.getMinecraft()
											.displayGuiScreen(
													new GuiMonitorEditor(
															message.pos, message.lines));
									return;
								}
								StfrSignal[] selected = new StfrSignal[6];
								for (int i = 0; i < 6; i++)
									selected[i] =
											StfrSignal.values()[
													Math.min(
															message.signals[i],
															StfrSignal.values().length - 1)];
								Minecraft.getMinecraft()
										.displayGuiScreen(
												new GuiSignalModifier(
														message.pos,
														EnumFacing.byIndex(message.face),
														selected,
														message.inline,
														BlockLogicDevice.Kind.values()[
																message.kind],
														message.repeatIntervals));
							});
			return null;
		}
	}
}

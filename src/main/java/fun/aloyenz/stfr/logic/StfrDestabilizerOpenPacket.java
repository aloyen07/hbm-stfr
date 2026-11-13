package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.client.gui.GuiDestabilizer;
import fun.aloyenz.stfr.reactor.StfrReactor;

public final class StfrDestabilizerOpenPacket implements IMessage {

	private BlockPos pos;
	private int stage;
	private float compression, charge;
	private boolean powered, active;

	public StfrDestabilizerOpenPacket() {}

	public StfrDestabilizerOpenPacket(BlockPos pos, StfrReactor reactor, boolean powered) {
		this.pos = pos;
		this.powered = powered;
		if (reactor != null) {
			stage = reactor.destabilizerStage();
			compression = (float) reactor.destabilizerCompression();
			charge = (float) (reactor.digammaCharge() / 100_000_000D);
			active = reactor.digammaActive();
		}
	}

	@Override
	public void fromBytes(ByteBuf b) {
		pos = BlockPos.fromLong(b.readLong());
		stage = b.readUnsignedByte();
		compression = b.readFloat();
		charge = b.readFloat();
		powered = b.readBoolean();
		active = b.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf b) {
		b.writeLong(pos.toLong());
		b.writeByte(stage);
		b.writeFloat(compression);
		b.writeFloat(charge);
		b.writeBoolean(powered);
		b.writeBoolean(active);
	}

	public static final class Handler
			implements IMessageHandler<StfrDestabilizerOpenPacket, IMessage> {
		@Override
		public IMessage onMessage(StfrDestabilizerOpenPacket m, MessageContext c) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() ->
									Minecraft.getMinecraft()
											.displayGuiScreen(
													new GuiDestabilizer(
															m.stage,
															m.compression,
															m.charge,
															m.powered,
															m.active)));
			return null;
		}
	}
}

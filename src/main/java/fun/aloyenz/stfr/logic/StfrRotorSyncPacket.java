package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.client.render.StfrClientVisuals;

/** Compact reactor visual phase update, addressed by the reactor center. */
public final class StfrRotorSyncPacket implements IMessage {

	private BlockPos pos;
	private double angle;
	private int velocity;
	private int runState;
	private int stateTicks;
	private int recoveryTicks;
	private boolean unstable;
	private boolean coreExists;
	private int electrodeMask;
	private boolean digamma;
	private int destabilizerStage, destabilizerTicks;
	private float selfCharge;

	public StfrRotorSyncPacket() {}

	public StfrRotorSyncPacket(
			BlockPos pos, double angle, int velocity, int runState, int stateTicks) {
		this.pos = pos;
		this.angle = angle;
		this.velocity = velocity;
		this.runState = runState;
		this.stateTicks = stateTicks;
	}

	public StfrRotorSyncPacket(
			BlockPos pos,
			double angle,
			int velocity,
			int runState,
			int stateTicks,
			int recoveryTicks,
			boolean unstable,
			boolean coreExists,
			int electrodeMask,
			boolean digamma,
			int destabilizerStage,
			int destabilizerTicks, float selfCharge) {
		this(pos, angle, velocity, runState, stateTicks);
		this.recoveryTicks = recoveryTicks;
		this.unstable = unstable;
		this.coreExists = coreExists;
		this.electrodeMask = electrodeMask;
		this.digamma = digamma;
		this.destabilizerStage = destabilizerStage;
		this.destabilizerTicks = destabilizerTicks;
		this.selfCharge=selfCharge;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		angle = buf.readDouble();
		velocity = buf.readInt();
		runState = buf.readUnsignedByte();
		stateTicks = buf.readInt();
		recoveryTicks = buf.readInt();
		unstable = buf.readBoolean();
		coreExists = buf.readBoolean();
		electrodeMask = buf.readInt();
		digamma = buf.readBoolean();
		destabilizerStage = buf.readUnsignedByte();
		destabilizerTicks = buf.readInt();
		selfCharge=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeDouble(angle);
		buf.writeInt(velocity);
		buf.writeByte(runState);
		buf.writeInt(stateTicks);
		buf.writeInt(recoveryTicks);
		buf.writeBoolean(unstable);
		buf.writeBoolean(coreExists);
		buf.writeInt(electrodeMask);
		buf.writeBoolean(digamma);
		buf.writeByte(destabilizerStage);
		buf.writeInt(destabilizerTicks);
		buf.writeFloat(selfCharge);
	}

	public static final class Handler implements IMessageHandler<StfrRotorSyncPacket, IMessage> {

		@Override
		public IMessage onMessage(StfrRotorSyncPacket message, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								Minecraft mc = Minecraft.getMinecraft();
								if (mc.world == null) return;
								StfrClientVisuals.update(
										mc.world,
										message.pos,
										message.angle,
										message.velocity,
										message.runState,
										message.stateTicks,
										message.recoveryTicks,
										message.unstable,
										message.coreExists,
										message.electrodeMask,
										message.digamma,
										message.destabilizerStage,
										message.destabilizerTicks,message.selfCharge);
							});
			return null;
		}
	}
}

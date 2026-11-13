package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.client.render.StfrFailureParticle;

public final class StfrCompatParticlePacket implements IMessage {

	private double x, y, z, spread, speed;
	private int kind, count;

	public StfrCompatParticlePacket() {}

	public StfrCompatParticlePacket(
			double x, double y, double z, int kind, int count, double spread, double speed) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.kind = kind;
		this.count = count;
		this.spread = spread;
		this.speed = speed;
	}

	public void fromBytes(ByteBuf b) {
		x = b.readDouble();
		y = b.readDouble();
		z = b.readDouble();
		kind = b.readUnsignedByte();
		count = b.readInt();
		spread = b.readDouble();
		speed = b.readDouble();
	}

	public void toBytes(ByteBuf b) {
		b.writeDouble(x);
		b.writeDouble(y);
		b.writeDouble(z);
		b.writeByte(kind);
		b.writeInt(count);
		b.writeDouble(spread);
		b.writeDouble(speed);
	}

	public static final class Handler
			implements IMessageHandler<StfrCompatParticlePacket, IMessage> {
		public IMessage onMessage(StfrCompatParticlePacket m, MessageContext ctx) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								Minecraft mc = Minecraft.getMinecraft();
								if (mc.world == null
										|| m.kind > 4
										|| m.kind == 2 && StfrConfig.reducedFailureFlashes
										|| mc.player == null
										|| mc.player.getDistanceSq(m.x, m.y, m.z) > 128D * 128D)
									return;
								Random random = mc.world.rand;
								int cap =
										mc.gameSettings.particleSetting == 2
												? 4
												: mc.gameSettings.particleSetting == 1 ? 16 : 48;
								for (int i = 0; i < Math.min(cap, Math.max(0, m.count)); i++)
									mc.effectRenderer.addEffect(
											new StfrFailureParticle(
													mc.world,
													m.x + random.nextGaussian() * m.spread,
													m.y + random.nextGaussian() * m.spread,
													m.z + random.nextGaussian() * m.spread,
													random.nextGaussian() * m.speed,
													random.nextGaussian() * m.speed,
													random.nextGaussian() * m.speed,
													m.kind));
							});
			return null;
		}
	}
}

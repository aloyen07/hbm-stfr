package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.reactor.physics.StfrRotorVibration;
import fun.aloyenz.stfr.reactor.physics.StfrShutdownDebris;
import fun.aloyenz.stfr.reactor.physics.StfrShutdownWater;

final class StfrShutdownVisualsTest {
	@Test
	void shutdownPurgeRequiresTenBucketsInEachOfFourCapsules() {
		assertFalse(StfrShutdownWater.enough(0));
		assertFalse(StfrShutdownWater.enough(9999));
		assertTrue(StfrShutdownWater.enough(10000));
		assertTrue(StfrShutdownWater.enough(100000));
		assertEquals(40000, StfrShutdownWater.CAPSULES
				* StfrShutdownWater.PER_CAPSULE);
	}
	@Test
	void rotorVibrationReturnsAfterAnImpactAndFollowsSpeedAndDistance() {
		double baseline = StfrRotorVibration.amplitude(300, 10);
		assertTrue(baseline > .05 && baseline < .5);
		assertEquals(0, StfrRotorVibration.amplitude(100, 0));
		assertEquals(0, StfrRotorVibration.amplitude(0, 0));
		assertEquals(0, StfrRotorVibration.amplitude(700, 80));
		assertTrue(StfrRotorVibration.amplitude(700, 10) > baseline);
		assertTrue(StfrRotorVibration.amplitude(300, 40) < baseline);
		for (int explosion = 0; explosion < 3; explosion++) {
			assertEquals(0, baseline * StfrRotorVibration.backgroundWeight(2));
			assertEquals(baseline, baseline * StfrRotorVibration.backgroundWeight(0));
		}
	}

	@Test
	void breakupStartsAtTheShieldBlastAndAcceleratesAtTheChargingSound() {
		assertEquals(0, StfrShutdownDebris.releaseCount(4949, false));
		assertEquals(4, StfrShutdownDebris.releaseCount(4950, false));
		assertEquals(0, StfrShutdownDebris.releaseCount(4951, false));
		assertEquals(8, StfrShutdownDebris.releaseCount(5150, false));
		assertEquals(0, StfrShutdownDebris.releaseCount(4950, true));
		assertEquals(0, StfrShutdownDebris.releaseCount(5300, false));
	}

	@Test
	void debrisSpawnPreservesMaterialAgeSpinAndReleaseState() {
		Bootstrap.register();
		EntityStfrDebris server = new EntityStfrDebris(null, new BlockPos(10, 65, 12),
				Blocks.PLANKS.getStateFromMeta(3), new BlockPos(0, 64, 0), 80, 350, 200);
		server.fallTime = 210;
		ByteBuf packet = Unpooled.buffer();
		try {
			server.writeSpawnData(packet);
			EntityStfrDebris client = new EntityStfrDebris(null);
			client.readSpawnData(packet);
			assertEquals(server.getBlock(), client.getBlock());
			assertEquals(server.fallTime, client.fallTime);
			assertEquals(server.spin(.5F), client.spin(.5F));
			assertFalse(client.shouldDropItem);
			assertFalse(client.canBeCollidedWith());
			assertFalse(client.released());
			assertFalse(client.isDead);

			// A spawn/NBT round trip does not tick an entity without a world.
			// Release and falling progress must survive saving without losing the spin.
			packet.readerIndex(0);
			NBTTagCompound state =
					ByteBufUtils.readTag(packet);
			assertEquals(80D, state.getDouble("stfrReleaseY"));
			state.setBoolean("stfrReleased", true);
			state.setInteger("stfrFallingTicks", 47);
			packet.clear();
			ByteBufUtils.writeTag(packet, state);
			EntityStfrDebris falling = new EntityStfrDebris(null);
			falling.readSpawnData(packet);
			assertTrue(falling.released());
			assertTrue(falling.canBeCollidedWith());
			assertEquals(server.getBlock(), falling.getBlock());
			assertEquals(server.fallTime, falling.fallTime);
			assertEquals(server.spin(.5F), falling.spin(.5F));
			assertFalse(falling.isDead);
			packet.clear();
			falling.writeSpawnData(packet);
			state = ByteBufUtils.readTag(packet);
			assertEquals(47, state.getInteger("stfrFallingTicks"));
			assertTrue(state.getBoolean("stfrReleased"));
		} finally {
			packet.release();
		}
	}
}

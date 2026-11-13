package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

public final class StfrAutopilotPersistenceTest {
	private static final BlockPos OWNER = new BlockPos(125, 65, -35);

	@Test
	void savedControllerCanResumeThroughAWarning() {
		StfrAutopilotLease connected = new StfrAutopilotLease();
		connected.remember(OWNER, 2, 1200L);
		NBTTagCompound saved = new NBTTagCompound();
		connected.write(saved);

		StfrAutopilotLease loaded = new StfrAutopilotLease();
		loaded.read(saved, true);
		assertTrue(StfrAutopilotArming.hasBlockingWarnings(1 << 4, true, true));
		assertTrue(loaded.canResume(OWNER, 2, true, false),
				"arming warnings only restrict the initial connection");
		assertEquals(1200L, loaded.engagedTick());
		assertFalse(loaded.canResume(OWNER.add(1, 0, 0), 2, true, false));
		assertFalse(loaded.canResume(OWNER, 3, true, false));
	}

	@Test
	void oldSavesAndStoppedReactorsDoNotResume() {
		StfrAutopilotLease lease = new StfrAutopilotLease();
		lease.read(new NBTTagCompound(), true);
		assertFalse(lease.canResume(OWNER, 1, true, false));

		lease.remember(OWNER, 1, 80L);
		NBTTagCompound saved = new NBTTagCompound();
		lease.write(saved);
		lease.read(saved, false);
		assertFalse(lease.armed());
		lease.remember(OWNER, 1, 80L);
		lease.clear();
		assertFalse(lease.canResume(OWNER, 1, true, false));
	}

	@Test
	void malformedOrInterlockedLeaseCannotResume() {
		StfrAutopilotLease lease = new StfrAutopilotLease();
		lease.remember(OWNER, 3, 100L);
		NBTTagCompound invalid = new NBTTagCompound();
		lease.write(invalid);
		invalid.setInteger("autopilotArmedLevel", 9);
		StfrAutopilotLease loaded = new StfrAutopilotLease();
		loaded.read(invalid, true);
		assertFalse(loaded.armed());

		lease.write(invalid);
		loaded.read(invalid, true);
		assertFalse(loaded.canResume(OWNER, 3, true, true));
		assertFalse(loaded.canResume(OWNER, 3, false, false));
	}
}

package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.tile.TileEntityFrozenLens;

final class StfrLensThawingTest {
	private static final class FixedRoll extends Random {
		final int roll;
		int calls;
		FixedRoll(int passes) { roll = passes - 3; }
		@Override public int nextInt(int bound) {
			assertEquals(14, bound);
			calls++;
			return roll;
		}
	}

	@Test void everyAllowedRollSurvivesPlacementAndReloadWithoutReroll() {
		TileEntity.register("hbmstfr:frozen_lens", TileEntityFrozenLens.class);
		for (int passes = 3; passes <= 16; passes++) {
			FixedRoll random = new FixedRoll(passes);
			NBTTagCompound data = new NBTTagCompound();
			assertEquals(passes, StfrLensThawing.remaining(data, random));
			for (int completed = 1; completed <= passes; completed++) {
				// Simulate placing the partially thawed item, saving/loading its tile,
				// then retrieving its drop. The world RNG must not reset its progress.
				TileEntityFrozenLens placed = new TileEntityFrozenLens();
				placed.setLensData(data);
				TileEntityFrozenLens loaded = new TileEntityFrozenLens();
				loaded.readFromNBT(placed.writeToNBT(new NBTTagCompound()));
				data = loaded.lensData();
				assertEquals(passes - completed, StfrLensThawing.process(data, random));
			}
			assertEquals(1, random.calls);
		}
	}

	@Test void copiesDoNotShareTheirCounter() {
		FixedRoll random = new FixedRoll(8);
		TileEntityFrozenLens tile = new TileEntityFrozenLens();
		NBTTagCompound original = new NBTTagCompound();
		original.setInteger(StfrLensThawing.KEY, 8);
		tile.setLensData(original);
		original.setInteger(StfrLensThawing.KEY, 1);
		NBTTagCompound drop = tile.lensData();
		assertEquals(7, StfrLensThawing.process(drop, random));
		assertEquals(8, tile.lensData().getInteger(StfrLensThawing.KEY));
		assertEquals(0, random.calls);
	}
}

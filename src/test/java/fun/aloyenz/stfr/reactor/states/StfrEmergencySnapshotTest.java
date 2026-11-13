package fun.aloyenz.stfr.reactor.states;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class StfrEmergencySnapshotTest {
	@Test void legacySnapshotRemainsReadableAndMigratableInPlace() {
		NBTTagCompound saved=new NBTTagCompound(),legacy=new NBTTagCompound();
		legacy.setInteger("stage",3);saved.setTag("selfPreviousOther",legacy);
		NBTTagCompound snapshot=StfrEmergencyPersistence.previousSnapshot(saved);
		assertEquals(3,snapshot.getInteger("stage"));
		snapshot.setInteger("stage",4);
		assertEquals(4,saved.getCompoundTag("selfPreviousOther").getInteger("stage"));
	}
	@Test void newSnapshotTakesPrecedenceAndMissingSnapshotIsEmpty() {
		NBTTagCompound saved=new NBTTagCompound(),current=new NBTTagCompound(),legacy=new NBTTagCompound();
		current.setInteger("stage",2);legacy.setInteger("stage",5);
		saved.setTag("previousEmergencySnapshot",current);saved.setTag("selfPreviousOther",legacy);
		assertEquals(2,StfrEmergencyPersistence.previousSnapshot(saved).getInteger("stage"));
		assertTrue(StfrEmergencyPersistence.previousSnapshot(new NBTTagCompound()).isEmpty());
	}
}

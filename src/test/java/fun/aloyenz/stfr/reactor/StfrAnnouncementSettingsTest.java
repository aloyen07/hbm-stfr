package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

public final class StfrAnnouncementSettingsTest {
	@Test
	void settingsBelongToReactorAndSurviveSave() {
		StfrAnnouncementOptions options = new StfrAnnouncementOptions();
		assertTrue(options.messages);
		assertTrue(options.voice);
		assertTrue(options.music);
		options.set(false, false, false);
		NBTTagCompound saved = new NBTTagCompound();
		options.write(saved);
		StfrAnnouncementOptions loaded = new StfrAnnouncementOptions();
		loaded.read(saved);
		assertFalse(loaded.messages);
		assertFalse(loaded.voice);
		assertFalse(loaded.music);
		NBTTagCompound legacy = saved.copy();
		legacy.removeTag("announcementMessages");
		legacy.removeTag("announcementVoice");
		legacy.removeTag("announcementMusic");
		StfrAnnouncementOptions oldSave = new StfrAnnouncementOptions();
		oldSave.read(legacy);
		assertTrue(oldSave.messages);
		assertTrue(oldSave.voice);
		assertTrue(oldSave.music);
	}
}

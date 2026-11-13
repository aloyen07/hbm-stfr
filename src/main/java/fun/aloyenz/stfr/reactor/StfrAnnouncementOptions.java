package fun.aloyenz.stfr.reactor;

import net.minecraft.nbt.NBTTagCompound;

/** Saved per-reactor delivery options; missing legacy keys retain the enabled defaults. */
final class StfrAnnouncementOptions {
	boolean messages = true;
	boolean voice = true;
	boolean music = true;

	void set(boolean messages, boolean voice, boolean music) {
		this.messages = messages;
		this.voice = voice;
		this.music = music;
	}

	void write(NBTTagCompound tag) {
		tag.setBoolean("announcementMessages", messages);
		tag.setBoolean("announcementVoice", voice);
		tag.setBoolean("announcementMusic", music);
	}

	void read(NBTTagCompound tag) {
		messages = !tag.hasKey("announcementMessages") || tag.getBoolean("announcementMessages");
		voice = !tag.hasKey("announcementVoice") || tag.getBoolean("announcementVoice");
		music = !tag.hasKey("announcementMusic") || tag.getBoolean("announcementMusic");
	}
}

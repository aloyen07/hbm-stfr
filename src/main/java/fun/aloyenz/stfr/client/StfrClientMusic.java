package fun.aloyenz.stfr.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.sound.StfrMusicPolicy;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class StfrClientMusic {

	private static final Map<String, ISound> PLAYING = new HashMap<>();
	private static final Map<String, ISound> WAITING = new LinkedHashMap<>();
	private static World world;

	private StfrClientMusic() {}

	public static void receive(BlockPos center, String sound, float pitch, boolean play) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null) return;
		if (world != mc.world) {
			for (ISound old : PLAYING.values()) mc.getSoundHandler().stopSound(old);
			PLAYING.clear();
			WAITING.clear();
			world = mc.world;
		}
		String key = center.toLong() + ":" + sound;
		if (!play) {
			if ("*".equals(sound)) {
				String prefix = center.toLong() + ":";
				WAITING.keySet().removeIf(active -> active.startsWith(prefix));
				PLAYING.entrySet().removeIf(active -> {
					if (!active.getKey().startsWith(prefix)) return false;
					mc.getSoundHandler().stopSound(active.getValue());
					return true;
				});
				return;
			}
			WAITING.remove(key);
			ISound previous = PLAYING.remove(key);
			if (previous != null) mc.getSoundHandler().stopSound(previous);
			return;
		}
		if (PLAYING.containsKey(key) || WAITING.containsKey(key)) return;
		ISound record = new StfrMusicSound(sound,pitch);
		String prefix = center.toLong() + ":";
		if (PLAYING.keySet().stream().anyMatch(active -> active.startsWith(prefix) && StfrMusicPolicy.blocks(sound,active)))
			WAITING.put(key, record);
		else {
			PLAYING.put(key, record);
			mc.getSoundHandler().playSound(record);
			showTitle(mc, sound);
		}
	}

	private static boolean isAccent(String sound) { return StfrMusicPolicy.accent(sound); }

	private static void showTitle(Minecraft mc, String sound) {
		String file = sound.substring(sound.lastIndexOf('.') + 1);
		if (isAccent(sound) || "hacknet_music_end".equals(file)) return;
		if ("startup_music".equals(file)) file = "startup";
		mc.ingameGUI.setRecordPlayingMessage(I18n.format("music.hbmstfr." + file));
	}

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (world != mc.world) {
			for (ISound sound : PLAYING.values()) mc.getSoundHandler().stopSound(sound);
			PLAYING.clear();
			WAITING.clear();
			world = mc.world;
		}
		PLAYING.entrySet()
				.removeIf(entry -> !mc.getSoundHandler().isSoundPlaying(entry.getValue()));
		Iterator<Map.Entry<String, ISound>> queued = WAITING.entrySet().iterator();
		while (queued.hasNext()) {
			Map.Entry<String, ISound> entry = queued.next();
			String prefix = entry.getKey().substring(0, entry.getKey().indexOf(':') + 1);
			if (PLAYING.keySet().stream().anyMatch(active -> active.startsWith(prefix) && !isAccent(active))) continue;
			PLAYING.put(entry.getKey(), entry.getValue());
			mc.getSoundHandler().playSound(entry.getValue());
			showTitle(mc, entry.getKey().substring(entry.getKey().indexOf(':') + 1));
			queued.remove();
		}
	}
}

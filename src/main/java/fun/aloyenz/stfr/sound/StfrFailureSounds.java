package fun.aloyenz.stfr.sound;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrFailureSounds {

	private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();

	static {
		add("alarms.systemoverheat");
		add("block.sculk.charge");
		add("block.respawn_anchor.deplete");
		add("voicelines.stfr.startup_overload.start");
		add("voicelines.stfr.startup_overload.restabilize");
		add("voicelines.stfr.startup_overload.restab_start");
		add("voicelines.stfr.startup_overload.options");
		add("voicelines.stfr.startup_overload.output_stable");
		add("voicelines.stfr.startup_overload.detonation_5.5m");
		add("voicelines.stfr.startup_overload.detonation_5m");
		add("voicelines.stfr.startup_overload.detonation_4.5m");
		add("voicelines.stfr.startup_overload.detonation_4m");
		add("voicelines.stfr.startup_overload.detonation_3m");
		add("voicelines.stfr.startup_overload.detonation_2.5m");
		add("voicelines.stfr.startup_overload.detonation_60s");
		add("voicelines.stfr.startup_overload.detonation_30s");
		add("voicelines.stfr.startup_overload.stab1_connected");
		add("voicelines.stfr.startup_overload.stab1_disconnected");
		add("voicelines.stfr.startup_overload.stab2_connected");
		add("voicelines.stfr.startup_overload.stab2_disconnected");
		add("voicelines.stfr.startup_overload.stab3_connected");
		add("voicelines.stfr.startup_overload.stab3_disconnected");
		add("voicelines.stfr.startup_overload.stab4_connected");
		add("voicelines.stfr.startup_overload.stab4_disconnected");
		add("voicelines.stfr.startup_overload.stab5_connected");
		add("voicelines.stfr.startup_overload.stab5_disconnected");
		add("voicelines.stfr.startup_overload.stab6_connected");
		add("voicelines.stfr.startup_overload.stab6_disconnected");
		add("alarms.civil_alert_siren");
		add("alarms.civil_alert_siren_2");
		add("alarms.civil_alert_siren_3");
		add("alarms.civil_alert_siren_4");
		add("alarms.airraidsiren");
		add("alarms.notification");
		add("explosion.huge_explosion");
		add("explosion.shieldboom");
		add("explosion.pressuregigaboom");
		add("charge_up.reactordetonationcharging");
		add("lightsswitch");
		add("powerpurge");
		add("music.idunno_reactor_meltdown_sequence");
		add("music.hacknet_ost_paki_trak");
		add("music.explosionmusic");
		add("music.hacknet_music_end");
	}

	private StfrFailureSounds() {}

	private static void add(String name) {
		ResourceLocation id = new ResourceLocation(Tags.MODID, "failure." + name);
		SOUNDS.put(name, new SoundEvent(id).setRegistryName(id));
	}

	public static SoundEvent get(String name) {
		return SOUNDS.get(name);
	}

	@SubscribeEvent
	public static void register(RegistryEvent.Register<SoundEvent> event) {
		event.getRegistry().registerAll(SOUNDS.values().toArray(new SoundEvent[0]));
	}
}

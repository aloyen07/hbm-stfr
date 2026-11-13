package fun.aloyenz.stfr.sound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class StfrSceneSounds {

	private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();

	public static Collection<String> musicIds() {
		List<String> ids = new ArrayList<>();
		for (String id : SOUNDS.keySet()) if (id.startsWith("music.")) ids.add(id);
		return ids;
	}

	static {
		add("lightsswitch");
		add("alarms.beep");

		add("voicelines.stfr.bct.case_restore");
		add("voicelines.stfr.bct.stab1_restore");
		add("voicelines.stfr.bct.stab2_restore");
		add("voicelines.stfr.bct.stab3_restore");
		add("voicelines.stfr.bct.stab4_restore");
		add("voicelines.stfr.bct.stab5_restore");
		add("voicelines.stfr.bct.stab6_restore");

		add("alarms.airraidsiren");
		add("alarms.amogus");
		add("alarms.beep2");
		add("alarms.beep3");
		add("braam");
		add("charge_up.charge");
		add("charge_up.heavy_shutdown");
		add("charge_up.heavy_startup");
		add("charge_up.mega_chargeup");
		add("charge_up.motor_rampup");
		add("dem.metal_2");
		add("magnetic.snap");
		add("magnetic.tear");
		add("dem.purge");
		add("explosion.bang");
		add("explosion.bang2");
		add("explosion.mega_explosion");
		add("explosion.pressuregigaboom");
		add("explosion.rocketfly");
		add("explosion.shieldboom");
		add("music.carpenter_brut_run_sally_run");
		add("music.hacknet_labyrinths_ogre_snidelywhiplash");
		add("music.hacknet_music_end");
		add("music.hacknet_ost_carpenter_brut");
		add("music.hacknet_ost_malware_injection_remix");
		add("music.hacknet_ost_paki_trak");
		add("music.hacknet_ost_panic_track");
		add("music.joel_nielsen_blast_pit_3");
		add("music.remi_galleno_panic_track");
		add("music.restabilization_music");
		add("music.the_final_flash_of_existance_remix");
		add("opencore.lock_material");
		add("powerpurge");
		add("rumbling");
		add("voicelines.stfr.manual_restab.attempt_aux_shield");
		add("voicelines.stfr.manual_restab.aux_shield_formed");
		add("voicelines.stfr.manual_restab.breakers");
		add("voicelines.stfr.manual_restab.confirm");
		add("voicelines.stfr.manual_restab.disconnect_panel");
		add("voicelines.stfr.manual_restab.fail");
		add("voicelines.stfr.manual_restab.fail_in_10s");
		add("voicelines.stfr.manual_restab.fail_in_2m");
		add("voicelines.stfr.manual_restab.fail_in_30s");
		add("voicelines.stfr.manual_restab.fail_in_60s");
		add("voicelines.stfr.manual_restab.reconnect_panel");
		add("voicelines.stfr.manual_restab.replace_lenses");
		add("voicelines.stfr.manual_restab.restabilize");
		add("voicelines.stfr.manual_restab.sstb");
		add("voicelines.stfr.overload.field");
		add("voicelines.stfr.overload.int_shield_crit");
		add("voicelines.stfr.overload.overdrive");
		add("voicelines.stfr.overload.plasma");
		add("voicelines.stfr.overload.restab_options");
		add("voicelines.stfr.overload.start");
		add("voicelines.stfr.reaction_loss.collapse");
		add("voicelines.stfr.reaction_loss.instability");
		add("voicelines.stfr.reaction_loss.restab_complete");
		add("voicelines.stfr.reaction_loss.restab_firing");
		add("voicelines.stfr.reaction_loss.restab_options");
		add("voicelines.stfr.reaction_loss.restab_start");
		add("voicelines.stfr.reaction_loss.stabilizer_fail");
		add("voicelines.stfr.shut_fail.diameter");
		add("voicelines.stfr.shut_fail.foreign");
		add("voicelines.stfr.shut_fail.fusion");
		add("voicelines.stfr.shut_fail.ineffective");
		add("voicelines.stfr.shut_fail.instability");
		add("voicelines.stfr.shut_fail.monitoring_fail");
		add("voicelines.stfr.shut_fail.permeability");
		add("voicelines.stfr.shut_fail.pressure");
		add("voicelines.stfr.shut_fail.projection");
		add("voicelines.stfr.shut_fail.reactivity");
		add("voicelines.stfr.shut_fail.restab_options");
		add("voicelines.stfr.shut_fail.setpoint");
		add("voicelines.stfr.shut_fail.start");
		add("voicelines.stfr.shut_fail.temp_fail");
		add("voicelines.stfr.underload.backup_systems_online");
		add("voicelines.stfr.underload.connecting_backup_systems");
		add("voicelines.stfr.underload.evacuate");
		add("voicelines.stfr.underload.field_increasing");
		add("voicelines.stfr.underload.no_match");
		add("voicelines.stfr.underload.scanning");
		add("voicelines.stfr.underload.stabilization_failure");
		add("voicelines.stfr.underload.structural_failure");
		add("voicelines.stfr.underload.system_damage");
	}

	private StfrSceneSounds() {}

	private static void add(String key) {
		ResourceLocation id = location(key);
		SOUNDS.put(key, new SoundEvent(id).setRegistryName(id));
	}

	public static ResourceLocation location(String key) {
		String path=key.startsWith("music.") || key.startsWith("alarms.")?key:
				key.startsWith("voicelines.")?"scene_voice."+key.substring("voicelines.".length()):"effects."+key;
		return new ResourceLocation(Tags.MODID,path);
	}

	public static SoundEvent get(String id) {
		return SOUNDS.get(id);
	}

	public static Collection<SoundEvent> values() {
		return SOUNDS.values();
	}

	@SubscribeEvent
	public static void register(RegistryEvent.Register<SoundEvent> e) {
		e.getRegistry().registerAll(SOUNDS.values().toArray(new SoundEvent[0]));
	}
}

package fun.aloyenz.stfr.sound;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fun.aloyenz.stfr.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class STFRSounds {

	public static final SoundEvent SLIDE_IN = sound("fuel_slot.slide_in");
	public static final SoundEvent SLIDE_OUT = sound("fuel_slot.slide_out");
	public static final SoundEvent CLACK_IN = sound("fuel_slot.clack_in");
	public static final SoundEvent CLACK_OUT = sound("fuel_slot.clack_out");
	public static final SoundEvent CAT_BUFFER_STALL = sound("meme.cat_buffer_stall");
	public static final SoundEvent CAPSULE_FILLER = sound("capsule_filler.flow");
	public static final SoundEvent START_PRIME = sound("reactor.start_prime");
	public static final SoundEvent START_CONFIRM = sound("reactor.start_confirm");
	public static final SoundEvent START_IGNITION = sound("reactor.start_ignition");
	public static final SoundEvent START_COMPLETE = sound("reactor.start_complete");
	public static final SoundEvent SHUTDOWN_PRIME = sound("reactor.shutdown_prime");
	public static final SoundEvent SHUTDOWN_COMPLETE = sound("reactor.shutdown_complete");
	public static final SoundEvent ALARM_BEEP = sound("reactor.alarm_beep");
	public static final SoundEvent ALARM_BEEP_2 = sound("reactor.alarm_beep2");
	public static final SoundEvent ALARM_2 = sound("reactor.alarm2");
	public static final SoundEvent ALARM_3 = sound("reactor.alarm3");
	public static final SoundEvent TENSION = sound("reactor.tension");
	public static final SoundEvent ALARM = sound("reactor.alarm");
	public static final SoundEvent NOTE_BIT = sound("reactor.note_bit");
	public static final SoundEvent NOTE_GUITAR = sound("reactor.note_guitar");
	public static final SoundEvent BREAKER_SWITCH = sound("reactor.breaker_switch");
	public static final SoundEvent BREAKER_ARM = sound("reactor.breaker_arm");
	public static final SoundEvent START_ABORT = sound("reactor.start_abort");
	public static final SoundEvent START_ABORT_AUTO = sound("reactor.start_abort_auto");
	public static final SoundEvent START_BREAKERS = sound("reactor.start_breakers");
	public static final SoundEvent START_BREAKERS_FAIL = sound("reactor.start_breakers_fail");
	public static final SoundEvent START_SYSTEMS = sound("reactor.start_systems");
	public static final SoundEvent START_SYSTEMS_ERROR = sound("reactor.start_systems_error");
	public static final SoundEvent START_SHIELD = sound("reactor.start_shield");
	public static final SoundEvent START_INJECTING = sound("reactor.start_injecting");
	public static final SoundEvent START_LASERS = sound("reactor.start_lasers");
	public static final SoundEvent SHUTDOWN_STABILIZING = sound("reactor.shutdown_stabilizing");
	public static final SoundEvent SHUTDOWN_COOLING = sound("reactor.shutdown_cooling");
	public static final SoundEvent SHUTDOWN_SHIELD = sound("reactor.shutdown_shield");
	public static final SoundEvent SHUTDOWN_BREAKERS = sound("reactor.shutdown_breakers");
	public static final SoundEvent SHUTDOWN_VACUUMING = sound("reactor.shutdown_vacuuming");
	public static final SoundEvent SHUTDOWN_ABORT = sound("reactor.shutdown_abort");
	public static final SoundEvent STARTUP_CORE = sound("reactor.startup_core");
	public static final SoundEvent HEAVY_STARTUP = sound("reactor.heavy_startup");
	public static final SoundEvent MOTOR_RAMPUP = sound("reactor.motor_rampup");
	public static final SoundEvent HEAVY_SHUTDOWN = sound("reactor.heavy_shutdown");
	public static final SoundEvent SYSTEM_NOISE = sound("reactor.system_noise");
	public static final SoundEvent HUMMING = sound("reactor.humming");
	public static final SoundEvent HUMMING_HIGH = sound("reactor.humming_high");
	public static final SoundEvent DIGAMMA_SCREAM_NORMAL = sound("digamma.scream.normal");
	public static final SoundEvent DIGAMMA_SCREAM_SECRET = sound("digamma.scream.secret");
	public static final SoundEvent REACTOR_SHUTDOWN = sound("reactor.shutdown_effect");
	public static final SoundEvent VACUUM = sound("reactor.vacuum");
	public static final SoundEvent STARTUP_MUSIC = sound("reactor.startup_music");
	public static final SoundEvent[] REFUELING = {
		sound("reactor.refueling.1"), sound("reactor.refueling.2"), sound("reactor.refueling.3"),
		sound("reactor.refueling.4"), sound("reactor.refueling.5"), sound("reactor.refueling.6")
	};
	// Vanilla beacon sounds were introduced after 1.12.  Ship the source assets
	// under our namespace so the STFR sequence retains the datapack's cues.
	public static final SoundEvent BEACON_ACTIVATE = sound("reactor.beacon_activate");
	public static final SoundEvent BEACON_POWER_SELECT = sound("reactor.beacon_power_select");
	public static final SoundEvent BEACON_AMBIENT = sound("reactor.beacon_ambient");
	public static final SoundEvent BEACON_DEACTIVATE = sound("reactor.beacon_deactivate");

	private STFRSounds() {}

	private static SoundEvent sound(String name) {
		ResourceLocation id = new ResourceLocation(Tags.MODID, name);
		return new SoundEvent(id).setRegistryName(id);
	}

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
		event.getRegistry().registerAll(REFUELING);
		event.getRegistry()
				.registerAll(
						SLIDE_IN,
						SLIDE_OUT,
						CLACK_IN,
						CLACK_OUT,
						CAT_BUFFER_STALL,
						CAPSULE_FILLER,
						START_PRIME,
						START_CONFIRM,
						START_IGNITION,
						START_COMPLETE,
						SHUTDOWN_PRIME,
						SHUTDOWN_COMPLETE,
						ALARM_BEEP,
						ALARM_BEEP_2,
						ALARM,
						ALARM_2,
						ALARM_3,
						TENSION,
						NOTE_BIT,
						NOTE_GUITAR,
						BREAKER_SWITCH,
						BREAKER_ARM,
						START_ABORT,
						START_ABORT_AUTO,
						START_BREAKERS,
						START_BREAKERS_FAIL,
						START_SYSTEMS,
						START_SYSTEMS_ERROR,
						START_SHIELD,
						START_INJECTING,
						START_LASERS,
						SHUTDOWN_STABILIZING,
						SHUTDOWN_COOLING,
						SHUTDOWN_SHIELD,
						SHUTDOWN_BREAKERS,
						SHUTDOWN_VACUUMING,
						SHUTDOWN_ABORT,
						STARTUP_CORE,
						HEAVY_STARTUP,
						MOTOR_RAMPUP,
						HEAVY_SHUTDOWN,
						SYSTEM_NOISE,
						HUMMING,
						HUMMING_HIGH,
						DIGAMMA_SCREAM_NORMAL,
						DIGAMMA_SCREAM_SECRET,
						REACTOR_SHUTDOWN,
						VACUUM,
						STARTUP_MUSIC,
						BEACON_ACTIVATE,
						BEACON_POWER_SELECT,
						BEACON_AMBIENT,
						BEACON_DEACTIVATE);
	}
}

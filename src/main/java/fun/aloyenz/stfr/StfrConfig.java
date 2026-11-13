package fun.aloyenz.stfr;

import java.io.File;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;

/** Server-side balance values which may be changed without rebuilding the addon. */
public final class StfrConfig {

	public static double heliumPlasmaDensityFactor = .25D / .9D;
	public static double deuteriumHeliumYield = .9D,
			tritiumHeliumYield = .9D,
			helium3HeliumYield = .9D;
	public static double digammaConversionMbPerMinute = 4D;
	public static double safeCorePoloniumMb = 3D, maxCorePoloniumMb = 6D;
	public static double safeCoreYaroniteMb = 2D, maxCoreYaroniteMb = 4D;
	public static int mainframeSlots = 4,
			signalDeviceSlots = 1,
			alarmSlots = 1,
			detonatorSlots = 1,
			portSlots = 2,
			autopilotSlots = 4;
	public static int mainframePowerPerTick = 5000;
	public static double autopilotArmShieldPercent = 70D;
	public static int autopilotArmStableTicks = 40;
	public static int autopilotArmPowerVariationPercent = 10;
	public static int autopilotArmPowerVariationFloor = 1000;
	public static boolean autopilotAllowSealWarning = true;
	public static boolean autopilotAllowStructureWarning = true;
	public static int dirtyShutdownSourceDurationSeconds = 30;
	public static int dirtyShutdownRadiationRadius = 64;
	public static double dirtyShutdownRadiationStrength = 1000D;
	public static boolean radiationResistantConcrete;
	public static long maxOnlineGenerationPerTick = 10_000_000L;
	public static long recoveryCharge = 13_000_000_000L;
	public static int detonationRadius = 120;
	public static boolean nuclearDetonation = true;
	public static boolean reducedFailureFlashes;
	public static boolean directGeneration;
	public static int directHePerMw = 1000;
	public static boolean hardcoreMaintenance;
	public static boolean disableHbmDarkFusionCrafting;
	public static boolean disableHbmDfcYaroniteRecipe = true;
	public static boolean legacyWandFix;
	public static double selfDestructMaxMultiplier = 3;
	public static int selfDestructChargeTicks = 1029;
	public static double simulatedCoreRotation = 120_000_000;
	public static double simulatedCoreTemperature = 55_000_000;
	public static double simulatedMssr = 48;

	private static final Map<StfrReactor.RunState, Integer> simulationRemaining =
			new EnumMap<>(StfrReactor.RunState.class);

	public static int simulationInitialTicks(StfrReactor.RunState state) {
		int remaining = simulationRemaining.getOrDefault(state, 0);
		return remaining == 0 ? 0 : Math.max(0, simulationDuration(state) - remaining);
	}

	private static int simulationDuration(StfrReactor.RunState state) {
		switch (state) {
			case STARTUP_CONFIRMED:
				return 1030;
			case SHUTDOWN:
				return 1140;
			case STARTUP_FAILURE:
				return 8040;
			case MELTDOWN:
				return 1398;
			default:
				return StfrEmergencyTimeline.deadline(state);
		}
	}

	private StfrConfig() {}

	public static void load(File file) {
		Configuration config = new Configuration(file);
		config.load();
		disableHbmDarkFusionCrafting = config.getBoolean(
				"disableHbmDarkFusionCrafting", "recipes", false,
				"Remove HBM crafting recipes for the DFC core, emitters, injectors, receivers, stabilizers"
						+ " and the four AMS cores. Set false to restore them.");
		disableHbmDfcYaroniteRecipe = config.getBoolean(
				"disableHbmDfcYaroniteRecipe", "recipes", true,
				"Remove only DFC processing recipes producing HBM yaronite billets."
						+ " Does not remove DFC equipment crafting recipes.");
		legacyWandFix = config.getBoolean(
				"legacyWandFix", "tools", false,
				"If true, the reactor analysis wand only fixes blocks marked for rotation."
						+ " If false, it fixes the orientation of any reactor structure block.");
		heliumPlasmaDensityFactor =
				config.getFloat(
						"heliumPlasmaDensityFactor",
						"reactor",
						(float) (.25D / .9D),
						.01F,
						1F,
						"Helium contribution to TERF plasma density and heat capacity. Does not"
							+ " change physical mB or helium yield; calibrated for the default 0.9"
							+ " yield.");
		deuteriumHeliumYield =
				config.getFloat(
						"deuteriumHeliumYield",
						"reactor",
						.9F,
						0F,
						1F,
						"mB of helium-4 per physical mB of deuterium reacted.");
		tritiumHeliumYield =
				config.getFloat(
						"tritiumHeliumYield",
						"reactor",
						.9F,
						0F,
						1F,
						"mB of helium-4 per physical mB of tritium reacted.");
		helium3HeliumYield =
				config.getFloat(
						"helium3HeliumYield",
						"reactor",
						.9F,
						0F,
						1F,
						"mB of helium-4 per physical mB of helium-3 reacted.");
		digammaConversionMbPerMinute =
				config.getFloat(
						"digammaConversionMbPerMinute",
						"digamma",
						4F,
						.001F,
						1000F,
						"Maximum HBM mB of Po-210 converted to yaronite per minute at full reaction"
							+ " power. Default: 2 mB per 30 seconds.");
		maxCorePoloniumMb =
				config.getFloat(
						"maxCorePoloniumMb",
						"digamma",
						6F,
						.01F,
						16000F,
						"Absolute Po-210 capacity inside the core, in HBM mB. Excess remains in the"
							+ " injector.");
		safeCorePoloniumMb =
				Math.min(
						maxCorePoloniumMb,
						config.getFloat(
								"safeCorePoloniumMb",
								"digamma",
								3F,
								0F,
								16000F,
								"Po-210 warning threshold inside the core, in HBM mB."));
		maxCoreYaroniteMb =
				config.getFloat(
						"maxCoreYaroniteMb",
						"digamma",
						4F,
						.01F,
						16000F,
						"Absolute yaronite capacity inside the core, in HBM mB. Conversion pauses"
							+ " when full.");
		safeCoreYaroniteMb =
				Math.min(
						maxCoreYaroniteMb,
						config.getFloat(
								"safeCoreYaroniteMb",
								"digamma",
								2F,
								0F,
								16000F,
								"Yaronite warning threshold inside the core, in HBM mB."));
		mainframeSlots =
				config.getInt(
						"mainframeSlots",
						"logic",
						4,
						1,
						1024,
						"Connection capacity per powered Mainframe block.");
		mainframePowerPerTick =
				config.getInt(
						"mainframePowerPerTick",
						"logic",
						5000,
						0,
						100000,
						"HE consumed by each Mainframe per tick.");
		signalDeviceSlots =
				config.getInt(
						"signalDeviceSlots",
						"logic",
						1,
						0,
						1024,
						"Slots per signal transmitter/receiver and control device.");
		alarmSlots = config.getInt("alarmSlots", "logic", 1, 0, 1024, "Slots per alarm.");
		detonatorSlots =
				config.getInt(
						"detonatorSlots", "logic", 1, 0, 1024, "Slots per detonator connector.");
		portSlots = config.getInt("portSlots", "logic", 2, 0, 1024, "Slots per multi-fluid port.");
		autopilotSlots =
				config.getInt(
						"autopilotSlots",
						"logic",
						4,
						0,
						1024,
						"Slots per autopilot block (all levels).");
		autopilotArmShieldPercent =
				config.getFloat("armShieldPercent", "autopilot", 70F, 0F, 100F,
						"Minimum shield percentage for engaging the autopilot (strictly above).");
		autopilotArmStableTicks =
				config.getInt("armStableTicks", "autopilot", 40, 1, 1200,
						"Consecutive stable reaction ticks required before engagement.");
		autopilotArmPowerVariationPercent =
				config.getInt("armPowerVariationPercent", "autopilot", 10, 0, 100,
						"Maximum reaction power change per tick during arming, in percent.");
		autopilotArmPowerVariationFloor =
				config.getInt("armPowerVariationFloor", "autopilot", 1000, 0, 10000000,
						"Minimum allowed reaction power change per tick during arming.");
		autopilotAllowSealWarning =
				config.getBoolean("allowSealWarning", "autopilot", true,
						"Allow arming while the reactor seal warning is active.");
		autopilotAllowStructureWarning =
				config.getBoolean("allowStructureWarning", "autopilot", true,
						"Allow arming while a turbine structure warning is active.");
		dirtyShutdownSourceDurationSeconds =
				config.getInt("sourceDurationSeconds", "dirty_shutdown", 30, 1, 3600,
						"Seconds that the reactor adds HBM chunk radiation after the core disappears.");
		dirtyShutdownRadiationRadius =
				config.getInt("radiationRadiusBlocks", "dirty_shutdown", 64, 16, 128,
						"Maximum distance from the reactor center for fallout deposits, in blocks.");
		dirtyShutdownRadiationStrength =
				config.getFloat("radiationPerPulse", "dirty_shutdown", 1000F, 0F, 10000F,
						"HBM chunk radiation added at the reactor per five-second pulse;"
								+ " decreases with distance and respects radiation shielding.");
		radiationResistantConcrete =
				config.getBoolean(
						"radiationResistantConcrete",
						"radiation",
						false,
						"Make HBM concrete, concrete_smooth and concrete_colored block radiation.");
		selfDestructMaxMultiplier =
				config.getFloat(
						"selfDestructMaxMultiplier",
						"reactor",
						3,
						1,
						27,
						"Maximum SELF_DESTRUCT blast-volume multiplier. HBM range scales by its"
							+ " cube root, not linearly.");
		selfDestructChargeTicks =
				config.getInt(
						"selfDestructChargeTicks",
						"reactor",
						1029,
						1,
						72000,
						"Active ticks with an existing core and at least one available turbine"
							+ " needed for maximum SELF_DESTRUCT charge. Preserved across pauses;"
							+ " scripted turbine explosions do not pause the finale.");
		simulatedCoreRotation =
				config.getFloat(
						"coreRotation",
						"simulation",
						120000000,
						1,
						1000000000,
						"Initial core rotation in original scoreboard units for the simulation wand"
							+ " (1000 units per RPM).");
		simulatedCoreTemperature =
				config.getFloat(
						"coreTemperature",
						"simulation",
						55000000,
						1,
						1000000000,
						"Initial core temperature for the simulation wand.");
		simulatedMssr =
				config.getFloat(
						"mssr",
						"simulation",
						48,
						1,
						10000,
						"Initial internal shield rigidity for the simulation wand. Subsequent"
							+ " physics derives it from rotation.");
		directGeneration =
				config.getBoolean(
						"directGeneration",
						"reactor",
						true,
						"Collect HE directly from the fusion reaction with working electrodes. Does"
							+ " not consume cooling steam.");
		directHePerMw =
				config.getInt(
						"directHePerMw",
						"reactor",
						1000,
						0,
						1000000,
						"Electrical HE/t per reaction MW at full electrode coverage; independent of"
							+ " cooling steam.");
		hardcoreMaintenance =
				config.getBoolean(
						"hardcoreMaintenance",
						"reactor",
						false,
						"Original STFR hardcore maintenance probability (0.002 per unlubricated"
							+ " ONLINE tick).");
		Property generation =
				config.get(
						"reactor",
						"maxOnlineGenerationPerTick",
						"10000000",
						"Maximum total HE generated and exported by one ONLINE STFR per tick.");
		maxOnlineGenerationPerTick = nonNegativeLong(generation.getString(), 10_000_000L);
		// Forge 1.12's numeric overload stored the old default as `1.0E7`, while
		// Property#getLong only accepts integer notation. Normalize that file in place.
		generation.set(Long.toString(maxOnlineGenerationPerTick));
		Property recovery =
				config.get(
						"reactor",
						"recoveryCharge",
						"13000000000",
						"HE charged once to restabilize STARTUP_FAILURE (600 ticks). Must be"
							+ " positive.");
		recoveryCharge = Math.max(1, nonNegativeLong(recovery.getString(), 13_000_000_000L));
		recovery.set(Long.toString(recoveryCharge));
		detonationRadius =
				config.getInt(
						"detonationRadius",
						"reactor",
						120,
						1,
						1000,
						"HBM MK3 detonation range. Nuclear waste/vaporization extend beyond this"
							+ " range.");
		nuclearDetonation =
				config.getBoolean(
						"nuclearDetonation",
						"reactor",
						true,
						"True: HBM MK3 nuclear explosion/fallout. False: HBM FLEIJA high-energy"
							+ " explosion.");
		reducedFailureFlashes =
				config.getBoolean(
						"reducedFailureFlashes",
						"client",
						false,
						"Hide rapid catastrophe flashes on this client.");
		for (StfrReactor.RunState state : StfrReactor.RunState.values()) {
			int duration = simulationDuration(state);
			if (duration > 0)
				simulationRemaining.put(
						state,
						config.getInt(
								state.name().toLowerCase(Locale.ROOT),
								"simulation_remaining_ticks",
								0,
								0,
								duration,
								"Remaining state ticks when selected with the creative wand. 0"
									+ " starts the complete scene; past timed events are"
									+ " skipped."));
		}
		if (config.hasChanged()) config.save();
	}

	private static long nonNegativeLong(String raw, long fallback) {
		try {
			return Math.max(0L, Long.parseLong(raw.trim()));
		} catch (NumberFormatException integerFailure) {
			try {
				BigDecimal value = new BigDecimal(raw.trim());
				if (value.signum() < 0) return 0L;
				return value.min(BigDecimal.valueOf(Long.MAX_VALUE)).longValue();
			} catch (NumberFormatException decimalFailure) {
				return fallback;
			}
		}
	}
}

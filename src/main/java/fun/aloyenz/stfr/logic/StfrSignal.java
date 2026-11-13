package fun.aloyenz.stfr.logic;

import java.util.Locale;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Logical channel selected independently for each face of a device. */
public enum StfrSignal {
	NONE,
	KEY,
	SSTB,
	BREAKERS,
	PRIME,
	CONFIRM,
	SHUTDOWN,
	FUEL_FLOW_UP,
	FUEL_FLOW_DOWN,
	TURBINES,
	SEAL_FAILURES,
	STATE,
	STATE_TICKS,
	FUEL_INPUT_LEVEL,
	STATUS,
	SYSTEMS,
	OFFSET_UP,
	OFFSET_DOWN,
	SUPPRESSION_UP,
	SUPPRESSION_DOWN,
	LASERS_UP,
	LASERS_DOWN,
	VENT_UP,
	VENT_DOWN,
	PERMEABILITY_UP,
	PERMEABILITY_DOWN,
	OFFSET,
	SUPPRESSION,
	LASERS,
	VENT,
	ROTOR_RPM,
	CORE_ROTATION,
	CORE_TEMPERATURE,
	CASE_TEMPERATURE,
	CORE_PRESSURE,
	SHIELD,
	REACTION_POWER,
	CORE_FUEL,
	CORE_HELIUM4,
	FUEL_TYPE,
	WATER,
	STEAM,
	LUBRICANT,
	PRODUCTS,
	ENERGY,
	FUEL_REMAINING,
	PERMEABILITY,
	CORE_RPM,
	CASE_PRESSURE,
	CASE_FUEL,
	CASE_HELIUM4,
	SHIELD_STRESS,
	PRESSURE_STRESS,
	SPIN_STRESS,
	RIGIDITY_STRESS,
	MSSR,
	REACTION_RATE,
	HEAT_TRANSFER,
	COOLING_RATE,
	RADIATION,
	CAPACITOR,
	STEAM_BLOCKED,
	GENERATION,
	DIGAMMA_ACTIVE,
	DIGAMMA_COMPRESSION,
	DIGAMMA_CHARGE,
	POLONIUM,
	YARONITE;

	public enum ValueType {
		BOOLEAN,
		NUMBER,
		PERCENT,
		COMMAND,
		ALARM
	}

	public ValueType valueType() {
		switch (this) {
			case KEY:
			case SSTB:
			case BREAKERS:
				return ValueType.BOOLEAN;
			case PRIME:
			case CONFIRM:
			case SHUTDOWN:
			case FUEL_FLOW_UP:
			case FUEL_FLOW_DOWN:
			case OFFSET_UP:
			case OFFSET_DOWN:
			case SUPPRESSION_UP:
			case SUPPRESSION_DOWN:
			case LASERS_UP:
			case LASERS_DOWN:
			case VENT_UP:
			case VENT_DOWN:
			case PERMEABILITY_UP:
			case PERMEABILITY_DOWN:
				return ValueType.COMMAND;
			case FUEL_INPUT_LEVEL:
			case SHIELD:
			case SUPPRESSION:
			case LASERS:
			case VENT:
			case PERMEABILITY:
			case SHIELD_STRESS:
			case PRESSURE_STRESS:
			case SPIN_STRESS:
			case RIGIDITY_STRESS:
			case MSSR:
				return ValueType.PERCENT;
			case SEAL_FAILURES:
				return ValueType.ALARM;
			default:
				return ValueType.NUMBER;
		}
	}

	/** Inputs which are real, player-accessible external controls today. */
	public boolean isCommand() {
		switch (this) {
			case KEY:
			case SSTB:
			case FUEL_FLOW_UP:
			case FUEL_FLOW_DOWN:
			case OFFSET_UP:
			case OFFSET_DOWN:
			case SUPPRESSION_UP:
			case SUPPRESSION_DOWN:
			case LASERS_UP:
			case LASERS_DOWN:
			case VENT_UP:
			case VENT_DOWN:
			case PERMEABILITY_UP:
			case PERMEABILITY_DOWN:
				return true;
			default:
				return false;
		}
	}

	public static StfrSignal[] choicesFor(BlockLogicDevice.Kind kind) {
		if (kind == BlockLogicDevice.Kind.TRANSMITTER) {
			return new StfrSignal[] {
				NONE,
				KEY,
				SSTB,
				FUEL_FLOW_UP,
				FUEL_FLOW_DOWN,
				OFFSET_UP,
				OFFSET_DOWN,
				SUPPRESSION_UP,
				SUPPRESSION_DOWN,
				LASERS_UP,
				LASERS_DOWN,
				VENT_UP,
				VENT_DOWN,
				PERMEABILITY_UP,
				PERMEABILITY_DOWN
			};
		}
		return new StfrSignal[] {
			NONE,
			STATUS,
			SYSTEMS,
			TURBINES,
			SEAL_FAILURES,
			FUEL_INPUT_LEVEL,
			OFFSET,
			ROTOR_RPM,
			CORE_ROTATION,
			CORE_TEMPERATURE,
			CASE_TEMPERATURE,
			CORE_PRESSURE,
			SHIELD,
			REACTION_POWER,
			CORE_FUEL,
			CORE_HELIUM4,
			FUEL_TYPE,
			WATER,
			STEAM,
			LUBRICANT,
			PRODUCTS,
			ENERGY,
			FUEL_REMAINING,
			PERMEABILITY,
			CORE_RPM,
			CASE_PRESSURE,
			CASE_FUEL,
			CASE_HELIUM4,
			SHIELD_STRESS,
			PRESSURE_STRESS,
			SPIN_STRESS,
			RIGIDITY_STRESS,
			MSSR,
			REACTION_RATE,
			HEAT_TRANSFER,
			COOLING_RATE,
			RADIATION,
			CAPACITOR,
			STEAM_BLOCKED,
			GENERATION,
			DIGAMMA_ACTIVE,
			DIGAMMA_COMPRESSION,
			DIGAMMA_CHARGE,
			POLONIUM,
			YARONITE
		};
	}

	public boolean availableFor(BlockLogicDevice.Kind kind) {
		for (StfrSignal candidate : choicesFor(kind)) if (candidate == this) return true;
		return false;
	}

	public String translationKey() {
		return "signal.hbmstfr." + name().toLowerCase(Locale.ROOT);
	}

	public int color(StfrReactor reactor) {
		if (reactor == null) return 0xFF5555;
		switch (this) {
			case SEAL_FAILURES:
				return reactor.sealFailures() == 0 ? 0x77E893 : 0xFF5555;
			case TURBINES:
				return reactor.workingTurbines() == 6
						? 0x77E893
						: reactor.workingTurbines() >= 2 ? 0xFFD76A : 0xFF5555;
			case STATE:
				return reactor.runState() == StfrReactor.RunState.ONLINE
						? 0x77E893
						: reactor.runState() == StfrReactor.RunState.OFFLINE ? 0xA0A0A0 : 0xFFD76A;
			case STATUS:
				return reactor.runState() == StfrReactor.RunState.ONLINE
						? 0x77E893
						: reactor.runState() == StfrReactor.RunState.OFFLINE ? 0xA0A0A0 : 0xFFD76A;
			case SYSTEMS:
				return reactor.keyOn() && reactor.breakersOn() ? 0x77E893 : 0xFFD76A;
			case SHIELD_STRESS:
				return reactor.core().shieldStress() >= 100
						? 0xFF5555
						: reactor.core().shieldStress() >= 85 ? 0xFFD76A : 0x77E893;
			case FUEL_INPUT_LEVEL:
			case FUEL_FLOW_UP:
			case FUEL_FLOW_DOWN:
			case PERMEABILITY:
			case PERMEABILITY_UP:
			case PERMEABILITY_DOWN:
				return 0x7DDCFF;
			case KEY:
				return reactor.keyOn() ? 0x77E893 : 0xA0A0A0;
			case SSTB:
				return reactor.sstbOn() ? 0x77E893 : 0xA0A0A0;
			case BREAKERS:
				return reactor.breakersOn() ? 0x77E893 : 0xA0A0A0;
			default:
				return 0xF4F4F4;
		}
	}

	public String read(StfrReactor reactor) {
		if (reactor == null) return "NO REACTOR";
		switch (this) {
			case KEY:
				return String.valueOf(reactor.keyOn());
			case SSTB:
				return String.valueOf(reactor.sstbOn());
			case BREAKERS:
				return String.valueOf(reactor.breakersOn());
			case FUEL_TYPE:
				return reactor.core().fuelLabel();
			case OFFSET:
				return Integer.toString(reactor.offset());
			case SUPPRESSION:
				return reactor.suppression() + "%";
			case LASERS:
				return reactor.lasers() + "%";
			case VENT:
				return reactor.vent() + "%";
			case PERMEABILITY:
				return reactor.permeability() + "%";
			case ROTOR_RPM:
				return format(reactor.rotorRpm());
			case CORE_ROTATION:
				return format(reactor.telemetry(reactor.core().rotation(), 1));
			case CORE_RPM:
				return format(reactor.telemetry(reactor.core().rotation() / 1000D, 2));
			case CORE_TEMPERATURE:
				return format(reactor.telemetry(reactor.core().coreTemperature(), 3));
			case CASE_TEMPERATURE:
				return format(reactor.telemetry(reactor.core().caseTemperature(), 4));
			case CORE_PRESSURE:
				return format(reactor.telemetry(reactor.core().pressure(), 5));
			case CASE_PRESSURE:
				return format(reactor.telemetry(reactor.core().casePressure(), 6));
			case SHIELD:
				return format(reactor.telemetry(reactor.core().shield(), 7)) + "%";
			case REACTION_POWER:
				return format(reactor.telemetry(reactor.core().reactionPower(), 8));
			case CORE_FUEL:
				return format(reactor.telemetry(reactor.core().coreFuel(), 9));
			case CORE_HELIUM4:
				return format(reactor.telemetry(reactor.core().coreHelium4(), 10));
			case CASE_FUEL:
				return format(reactor.core().caseFuel());
			case CASE_HELIUM4:
				return format(reactor.core().caseHelium4());
			case SHIELD_STRESS:
				return format(reactor.telemetry(reactor.core().shieldStress(), 11)) + "%";
			case PRESSURE_STRESS:
				return format(reactor.core().pressureStress()) + "%";
			case SPIN_STRESS:
				return format(reactor.core().spinStress()) + "%";
			case RIGIDITY_STRESS:
				return format(reactor.core().rigidityStress()) + "%";
			case MSSR:
				return format(reactor.core().mssr()) + "%";
			case REACTION_RATE:
				return format(reactor.telemetry(reactor.core().reactionRate(), 12));
			case HEAT_TRANSFER:
				return format(reactor.core().heatTransferRate());
			case COOLING_RATE:
				return format(reactor.core().coolingRate());
			case RADIATION:
				return format(reactor.core().radiation());
			case CAPACITOR:
				return format(reactor.core().capacitor()) + "%";
			case STEAM_BLOCKED:
				return String.valueOf(reactor.core().steamBlocked());
			case WATER:
				return Long.toString(reactor.resources().water().getFill());
			case STEAM:
				return Long.toString(reactor.resources().steam().getFill());
			case LUBRICANT:
				return Long.toString(reactor.resources().lubricant().getFill());
			case PRODUCTS:
				return Long.toString(reactor.resources().helium4().getFill());
			case ENERGY:
				return Long.toString(reactor.resources().energy());
			case GENERATION:
				return Long.toString(reactor.generatedPower());
			case DIGAMMA_ACTIVE:
				return String.valueOf(reactor.digammaActive());
			case DIGAMMA_COMPRESSION:
				return format(reactor.destabilizerCompression() * 100D) + "%";
			case DIGAMMA_CHARGE:
				return format(reactor.digammaCharge() / 1_000_000D) + " MHE";
			case POLONIUM:
				return format(reactor.poloniumMb()) + " mB";
			case YARONITE:
				return Integer.toString(reactor.resources().yaronite().getFill()) + " mB";
			case FUEL_REMAINING:
				return Long.toString(reactor.fuelRemaining());
			case TURBINES:
				return reactor.workingTurbines() + "/6";
			case SEAL_FAILURES:
				return String.valueOf(reactor.sealFailures());
			case STATE:
				return reactor.runState().name();
			case STATE_TICKS:
				return String.valueOf(reactor.stateTicks());
			case FUEL_INPUT_LEVEL:
				return reactor.fuelInputLevel() + "%";
			case STATUS:
				return reactor.runState().name() + " " + reactor.stateTicks() + "t";
			case SYSTEMS:
				return "K:" + bool(reactor.keyOn()) + " B:" + bool(reactor.breakersOn());
			case FUEL_FLOW_UP:
			case FUEL_FLOW_DOWN:
				return reactor.fuelInputLevel() + "%";
			default:
				return "--";
		}
	}

	public boolean send(StfrReactor reactor, World world) {
		if (reactor == null) return false;
		if (reactor.catastrophic()
				&& this != SSTB && this != KEY && this != BREAKERS && this != SHUTDOWN)
			return false;
		switch (this) {
			case KEY:
				reactor.setKeyOn(true);
				return true;
			case SSTB:
				reactor.setSstbOn(true);
				return true;
			case BREAKERS:
				reactor.setBreakersOn(true);
				return true;
			case PRIME:
				return reactor.prime();
			case CONFIRM:
				return reactor.confirm(world);
			case SHUTDOWN:
				return reactor.shutdown(world);
			case FUEL_FLOW_UP:
				reactor.adjustFuelInputLevel(1);
				return true;
			case FUEL_FLOW_DOWN:
				reactor.adjustFuelInputLevel(-1);
				return true;
			case OFFSET_UP:
				reactor.adjustOffset(1);
				return true;
			case OFFSET_DOWN:
				reactor.adjustOffset(-1);
				return true;
			case SUPPRESSION_UP:
				reactor.adjustSuppression(1);
				return true;
			case SUPPRESSION_DOWN:
				reactor.adjustSuppression(-1);
				return true;
			case LASERS_UP:
				reactor.adjustLasers(1);
				return true;
			case LASERS_DOWN:
				reactor.adjustLasers(-1);
				return true;
			case VENT_UP:
				reactor.adjustVent(1);
				return true;
			case VENT_DOWN:
				reactor.adjustVent(-1);
				return true;
			case PERMEABILITY_UP:
				reactor.adjustPermeability(1);
				return true;
			case PERMEABILITY_DOWN:
				reactor.adjustPermeability(-1);
				return true;
			default:
				return false;
		}
	}

	public void clear(StfrReactor reactor) {
		if (reactor == null) return;
		switch (this) {
			case KEY:
				reactor.setKeyOn(false);
				break;
			case SSTB:
				reactor.setSstbOn(false);
				break;
			case BREAKERS:
				reactor.setBreakersOn(false);
				break;
			default:
				break;
		}
	}

	private static String bool(boolean value) {
		return value ? "ON" : "OFF";
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}

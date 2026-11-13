package fun.aloyenz.stfr.logic;

import java.util.Locale;
import fun.aloyenz.stfr.reactor.StfrCoreSimulation;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Four sign lines with colour escapes and short live reactor values. */
public final class StfrMonitorText {

	public static final int LINES = 4;
	public static final int VISIBLE_LIMIT = 15;
	public static final int RAW_LIMIT = 128;
	public static final String[] DEFAULT = {
		"&aSTFR $STATE", "Ввод $FUEL", "Ядро $CTEMP C", "Пар $STEAM"
	};

	private StfrMonitorText() {}

	public static boolean valid(String line) {
		if (line == null || line.length() > RAW_LIMIT) return false;
		int visible = 0;
		for (int i = 0; i < line.length(); ) {
			int end = escapeEnd(line, i);
			if (end > i) {
				i = end;
				continue;
			}
			if (Character.isISOControl(line.charAt(i)) || line.charAt(i) == '\u00a7') return false;
			visible++;
			i++;
		}
		return visible <= VISIBLE_LIMIT;
	}

	public static String expand(String line, StfrReactor reactor) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < line.length(); ) {
			if (i + 1 < line.length() && line.charAt(i) == '&' && isColor(line.charAt(i + 1))) {
				out.append('\u00a7').append(Character.toLowerCase(line.charAt(i + 1)));
				i += 2;
				continue;
			}
			if (line.charAt(i) == '$') {
				int end = tokenEnd(line, i);
				if (end > i) {
					out.append(value(line.substring(i + 1, end), reactor));
					i = end;
					continue;
				}
			}
			out.append(line.charAt(i++));
		}
		return out.toString();
	}

	private static int escapeEnd(String line, int index) {
		if (index + 1 < line.length()
				&& line.charAt(index) == '&'
				&& isColor(line.charAt(index + 1))) return index + 2;
		return line.charAt(index) == '$' ? tokenEnd(line, index) : index;
	}

	private static int tokenEnd(String line, int index) {
		int end = index + 1;
		while (end < line.length()
				&& (line.charAt(end) >= 'A' && line.charAt(end) <= 'Z'
						|| line.charAt(end) >= '0' && line.charAt(end) <= '9'
						|| line.charAt(end) == '_')) end++;
		return end > index + 1 && known(line.substring(index + 1, end)) ? end : index;
	}

	private static boolean isColor(char value) {
		char c = Character.toLowerCase(value);
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f';
	}

	private static boolean known(String token) {
		switch (token) {
			case "STATE":
			case "TICKS":
			case "FUEL":
			case "TURB":
			case "SEAL":
			case "KEY":
			case "SSTB":
			case "BRK":
			case "WATER":
			case "STEAM":
			case "TEMP":
			case "PRESS":
			case "POWER":
			case "ERR":
			case "FLEFT":
			case "LUBE":
			case "PROD":
			case "FTYPE":
			case "CFUEL":
			case "CHE4":
			case "CTEMP":
			case "CPRESS":
			case "SHIELD":
			case "RXHE":
			case "OFFSET":
			case "RF":
			case "LASER":
			case "VENT":
			case "RPM":
			case "TRPM":
			case "RVEL":
			case "CROT":
			case "LOCK":
			case "REFUEL":
			case "MISS":
			case "AIR":
			case "ENERGY":
			case "CORE":
			case "PERM":
			case "CASEHE4":
			case "CASEFUEL":
			case "SSTATUS":
			case "SSTRESS":
			case "PSTRESS":
			case "RSTRESS":
			case "SPSTRESS":
			case "MSSR":
			case "RR":
			case "HTR":
			case "COOL":
			case "RAD":
			case "CAP":
			case "INTAKE":
			case "MAINT":
			case "CPROD":
			case "UPTIME":
			case "GEN":
			case "REACT":
			case "DEUT":
			case "HE4MB":
			case "PO210":
			case "YARONITE":
				return true;
			default:
				return false;
		}
	}

	private static String value(String token, StfrReactor reactor) {
		if (reactor == null) return "--";
		switch (token) {
			case "STATE":
				return reactor.runState().name();
			case "MAINT":
				return Integer.toString(reactor.maintenance().count());
			case "INTAKE":
				return reactor.coolantIntakeLeaking() ? "LEAK" : "NORMAL";
			case "TICKS":
				return Integer.toString(reactor.stateTicks());
			case "FUEL":
				return reactor.fuelInputLevel() + "%";
			case "FTYPE":
				return reactor.core().fuelLabel();
			case "CORE":
				return reactor.core().exists() ? "ON" : "OFF";
			case "DEUT":
				return physicalMb(
						reactor.core().amount(StfrCoreSimulation.Fuel.DEUTERIUM, false)
								/ StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			case "HE4MB":
				return physicalMb(
						reactor.core().coreHelium4() / StfrCoreSimulation.PLASMA_UNITS_PER_MB);
			case "PO210":
				return physicalMb(reactor.poloniumMb());
			case "YARONITE":
				return physicalMb(reactor.yaroniteMb());
			case "CFUEL":
				return amount(reactor.core().coreFuel());
			case "CHE4":
				return amount(reactor.core().coreHelium4());
			case "CASEHE4":
				return amount(reactor.core().caseHelium4());
			case "CASEFUEL":
				return amount(reactor.core().caseFuel());
			case "CPROD":
				return amount(reactor.core().coreFuel() + reactor.core().coreHelium4());
			case "CTEMP":
				return decimal(reactor.telemetry(reactor.core().coreTemperature(), 3), 0);
			case "CPRESS":
				return decimal(reactor.telemetry(reactor.core().pressure(), 5), 1);
			case "SHIELD":
				return decimal(reactor.telemetry(reactor.core().shield(), 7), 1) + "%";
			case "SSTATUS":
				return reactor.core().shieldStress() >= 100
						? "DEGRADING"
						: reactor.core().shield() <= 75 ? "LOW" : "NORMAL";
			case "SSTRESS":
				return decimal(reactor.telemetry(reactor.core().shieldStress(), 11), 0) + "%";
			case "PSTRESS":
				return decimal(reactor.core().pressureStress(), 0) + "%";
			case "SPSTRESS":
				return decimal(reactor.core().spinStress(), 0) + "%";
			case "RSTRESS":
				return decimal(reactor.core().rigidityStress(), 0) + "%";
			case "MSSR":
				return decimal(reactor.core().mssr(), 0) + "%";
			case "RXHE":
				return decimal(reactor.telemetry(reactor.core().reactionPower(), 8), 0);
			case "RR":
				return decimal(reactor.telemetry(reactor.core().reactionRate(), 12), 0);
			case "HTR":
				return decimal(reactor.core().heatTransferRate(), 0);
			case "COOL":
				return decimal(reactor.core().coolingRate(), 0);
			case "RAD":
				return decimal(reactor.core().radiation(), 0);
			case "CAP":
				return decimal(reactor.core().capacitor(), 1) + "%";
			case "GEN":
				return Long.toString(reactor.generatedPower());
			case "REACT":
				return reactor.core().coreTemperature() < StfrCoreSimulation.CORE_TEMP_SAFE_MIN
						? "BELOW RANGE"
						: reactor.core().coreTemperature() > StfrCoreSimulation.CORE_TEMP_SAFE_MAX
								? "ABOVE RANGE"
								: "STABLE";
			case "UPTIME":
				return uptime(reactor.uptimeTicks());
			case "OFFSET":
				return Integer.toString(reactor.offset());
			case "RF":
				return reactor.suppression() + "%";
			case "LASER":
				return reactor.lasers() + "%";
			case "VENT":
				return reactor.vent() + "%";
			case "PERM":
				return reactor.permeability() + "%";
			case "RPM":
				return decimal(reactor.telemetry(reactor.core().rotation() / 1000D, 2), 1);
			case "TRPM":
				return decimal(reactor.rotorRpm(), 1);
			case "RVEL":
				return Integer.toString(reactor.rotorVelocity());
			case "CROT":
				return decimal(reactor.core().rotation(), 0);
			case "LOCK":
				return reactor.lockState().name();
			case "REFUEL":
				return Integer.toString(reactor.refuelingTurbines());
			case "MISS":
				return Integer.toString(reactor.missingPorts());
			case "AIR":
				return Integer.toString(reactor.blockedChamberAir());
			case "TURB":
				return reactor.workingTurbines() + "/6";
			case "SEAL":
				return Integer.toString(reactor.sealFailures());
			case "KEY":
				return reactor.keyOn() ? "ON" : "OFF";
			case "SSTB":
				return reactor.sstbOn() ? "ON" : "OFF";
			case "BRK":
				return reactor.breakersOn() ? "ON" : "OFF";
			case "WATER":
				return Integer.toString(reactor.resources().water().getFill());
			case "STEAM":
				return Integer.toString(reactor.resources().steam().getFill());
			case "TEMP":
				return String.format(Locale.ROOT, "%.0f", reactor.temperature());
			case "PRESS":
				return decimal(reactor.core().casePressure(), 1);
			case "POWER":
			case "ENERGY":
				return Long.toString(reactor.resources().energy());
			case "FLEFT":
				return Long.toString(reactor.fuelRemaining());
			case "LUBE":
				return Integer.toString(reactor.resources().lubricant().getFill());
			case "PROD":
				return Integer.toString(reactor.resources().helium4().getFill());
			case "ERR":
				return Integer.toString(reactor.wrongBlocks());
			default:
				return "--";
		}
	}

	private static String decimal(double value, int places) {
		return String.format(Locale.ROOT, places == 0 ? "%.0f" : "%.1f", value);
	}

	private static String physicalMb(double value) {
		return Math.abs(value) < 1000D ? decimal(value, 1) : amount(value);
	}

	private static String amount(double value) {
		double absolute = Math.abs(value);
		if (absolute >= 1_000_000_000D) return decimal(value / 1_000_000_000D, 1) + "G";
		if (absolute >= 1_000_000D) return decimal(value / 1_000_000D, 1) + "M";
		if (absolute >= 1_000D) return decimal(value / 1_000D, 1) + "k";
		return decimal(value, 1);
	}

	private static String uptime(long ticks) {
		long seconds = Math.max(0, ticks / 20);
		return String.format(
				Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
	}

	/** Marks a whole line when one of its displayed values crosses the original alarm threshold. */
	public static int alarmColor(String line, StfrReactor reactor) {
		if (reactor != null && reactor.catastrophic()) return 0xFF5555;
		if (reactor == null || !reactor.core().exists()) return 0xF4F4F4;
		double rpm = Math.abs(reactor.core().rotation()) / 1000D;
		boolean alarm =
				has(line, "RPM") && (rpm <= 81_000 || rpm >= 150_000)
						|| has(line, "MAINT") && reactor.maintenance().count() > 0
						|| has(line, "INTAKE") && reactor.coolantIntakeLeaking()
						|| has(line, "CTEMP")
								&& (reactor.core().coreTemperature()
												< StfrCoreSimulation.CORE_TEMP_SAFE_MIN
										|| reactor.core().coreTemperature()
												> StfrCoreSimulation.CORE_TEMP_SAFE_MAX)
						|| has(line, "CPRESS")
								&& (reactor.core().pressure() <= 1_000_000
										|| reactor.core().pressure() >= 5_000_000)
						|| has(line, "TEMP") && reactor.core().caseTemperature() >= 950
						|| has(line, "PRESS") && reactor.core().casePressure() >= 70_000
						|| has(line, "SHIELD") && reactor.core().shield() <= 75
						|| has(line, "SSTRESS") && reactor.core().shieldStress() >= 85
						|| has(line, "PSTRESS") && reactor.core().pressureStress() >= 100
						|| has(line, "SPSTRESS") && reactor.core().spinStress() >= 100
						|| has(line, "RSTRESS") && reactor.core().rigidityStress() >= 100
						|| has(line, "WATER") && reactor.resources().water().getFill() <= 2_500_000
						|| has(line, "STEAM") && reactor.core().steamBlocked()
						|| has(line, "TURB") && reactor.workingTurbines() < 6
						|| has(line, "SEAL") && reactor.sealFailures() > 0
						|| has(line, "FLEFT") && reactor.fuelRemaining() <= 6_000;
		return alarm ? 0xFF5555 : 0xF4F4F4;
	}

	private static boolean has(String line, String token) {
		int from = 0;
		String needle = "$" + token;
		while ((from = line.indexOf(needle, from)) >= 0) {
			int end = from + needle.length();
			if (end == line.length()
					|| !(line.charAt(end) >= 'A' && line.charAt(end) <= 'Z'
							|| line.charAt(end) >= '0' && line.charAt(end) <= '9'
							|| line.charAt(end) == '_')) return true;
			from = end;
		}
		return false;
	}
}

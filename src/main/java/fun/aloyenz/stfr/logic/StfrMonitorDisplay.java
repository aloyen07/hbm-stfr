package fun.aloyenz.stfr.logic;

import java.util.Locale;

public enum StfrMonitorDisplay {
	TEXT,
	REACTION,
	REACTOR,
	CORE;

	public String translationKey() {
		return "gui.hbmstfr.monitor.display." + name().toLowerCase(Locale.ROOT);
	}

	public static StfrMonitorDisplay read(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException ignored) {
			return TEXT;
		}
	}
}

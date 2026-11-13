package fun.aloyenz.stfr.sound;

import java.util.HashMap;
import java.util.Map;

/** Coalesces copies of the same event, without swallowing later distinct stages. */
public final class StfrEventGate {
	private final Map<String, Long> played = new HashMap<>();
	public boolean accept(String key, long tick) {
		played.entrySet().removeIf(e -> tick - e.getValue() > 200 || tick < e.getValue());
		Long previous = played.get(key);
		if (previous != null && tick - previous < 10) return false;
		played.put(key, tick);
		return true;
	}
}

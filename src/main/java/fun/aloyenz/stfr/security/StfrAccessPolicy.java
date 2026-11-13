package fun.aloyenz.stfr.security;

import java.util.*;

/** Card identities are bearer credentials; no player UUIDs are stored. */
public final class StfrAccessPolicy {

	public static final int CONTROL = 1, CONNECT = 2, ADMIN = 4, DETONATE = 8, ALL = 15;
	private final Map<UUID, Integer> cards = new LinkedHashMap<>();
	private boolean enabled;

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		enabled = value;
	}

	public Map<UUID, Integer> cards() {
		return Collections.unmodifiableMap(cards);
	}

	public int rights(UUID card) {
		return card == null ? 0 : cards.getOrDefault(card, 0);
	}

	public boolean allows(UUID card, int right) {
		return (rights(card) & right) == right;
	}

	public boolean initialized() {
		return !cards.isEmpty();
	}

	public boolean grant(UUID card, int rights) {
		if (card == null) return false;
		rights &= ALL;
		// Revocation/edits may never silently remove the final administrator.
		if (allows(card, ADMIN)
				&& (rights & ADMIN) == 0
				&& cards.entrySet().stream()
						.noneMatch(e -> !e.getKey().equals(card) && (e.getValue() & ADMIN) != 0))
			return false;
		if (rights == 0) cards.remove(card);
		else cards.put(card, rights);
		return true;
	}
}

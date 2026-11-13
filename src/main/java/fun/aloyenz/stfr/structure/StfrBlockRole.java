package fun.aloyenz.stfr.structure;

public enum StfrBlockRole {
	UNASSIGNED,
	EXACT,
	ANY_OF,
	CRITICAL,
	OPTIONAL_EFFICIENCY,
	SEAL,
	RADIATION_SHIELD,
	ELECTRODE,
	HATCH,
	PORT,
	FUEL_SLOT,
	MUST_AIR,
	MAYBE_AIR,
	LIGHTNING_ROD,
	HATCH_FIXED,
	HATCH_FREE,
	EMITTER_BLOCK,
	SUPERCRITICAL,
	SOLID_INJECTOR,
	CASE_BREACH;

	public boolean requiresAir() {
		return this == MUST_AIR || this == MAYBE_AIR;
	}

	public boolean preventsStartupWhenOccupied() {
		return requiresAir();
	}

	public OccupiedAirEffect effectWhenRunning() {
		if (this == MUST_AIR) return OccupiedAirEffect.TURBINE_FAILURE;
		if (this == MAYBE_AIR) return OccupiedAirEffect.SMALL_EXPLOSION_AND_DESTROY_BLOCK;
		return OccupiedAirEffect.NONE;
	}

	public enum OccupiedAirEffect {
		NONE,
		TURBINE_FAILURE,
		SMALL_EXPLOSION_AND_DESTROY_BLOCK
	}

	public StfrBlockRole nextEditable() {
		StfrBlockRole[] roles = values();
		return roles[(ordinal() + 1) % roles.length];
	}
}

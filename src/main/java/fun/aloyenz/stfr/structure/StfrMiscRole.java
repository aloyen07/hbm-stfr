package fun.aloyenz.stfr.structure;

/** Independent annotations; primary structural roles remain intact. */
public enum StfrMiscRole {
	UNDEFINED,
	ROTABLE,
	INJECTOR_PLACEABLE;

	public boolean isRotable() {
		return this != UNDEFINED;
	}

	public StfrMiscRole next() {
		return values()[(ordinal() + 1) % values().length];
	}
}

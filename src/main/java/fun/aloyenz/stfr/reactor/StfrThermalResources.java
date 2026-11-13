package fun.aloyenz.stfr.reactor;

/** Storage boundary of the thermal calculation, independent of world/port lifecycle. */
public interface StfrThermalResources {

	int waterAmount();

	int steamRoom();

	int productRoom();

	void consumeWater(int amount);

	int produceSteam(int amount);

	int produceHelium(int amount);

}

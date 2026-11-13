package fun.aloyenz.stfr.api.data;

/** Machine endpoint with an explicit cable attachment policy. */
public interface IDataMachine {
	default boolean acceptsDataCable() { return true; }
}

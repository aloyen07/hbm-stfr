package fun.aloyenz.stfr.api.data;

import com.hbm.uninos.NodeNet;
import java.util.Collections;
import java.util.Set;

public final class DataNetwork extends NodeNet<IDataMachine, IDataMachine, DataNode, DataNetwork> {

	private static final long ENDPOINT_TIMEOUT_MS = 3000L;

	@Override
	public void update() {
		long oldest = System.currentTimeMillis() - ENDPOINT_TIMEOUT_MS;
		receiverEntries
				.object2LongEntrySet()
				.removeIf(
						entry ->
								entry.getLongValue() < oldest || !entry.getKey().acceptsDataCable() || NodeNet.isBadLink(entry.getKey()));
		providerEntries.clear();
	}

	/** Live, read-only view of all machine endpoints currently attached to this network. */
	public Set<IDataMachine> getMachines() {
		return Collections.unmodifiableSet(receiverEntries.keySet());
	}
}

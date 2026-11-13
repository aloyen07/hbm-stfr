package fun.aloyenz.stfr.logic;

import java.util.*;

/** Deterministic whole-device allocation; low-priority devices are shed first. */
public final class StfrNetworkCapacity {

	private StfrNetworkCapacity() {}

	public static final class Device {

		public final long id;
		public final int slots, retentionPriority;

		public Device(long id, int slots, int retentionPriority) {
			this.id = id;
			this.slots = Math.max(0, slots);
			this.retentionPriority = retentionPriority;
		}
	}

	public static final class Allocation {

		public final Set<Long> enabled;
		public final int capacity, demand, used;

		Allocation(Set<Long> enabled, int capacity, int demand, int used) {
			this.enabled = Collections.unmodifiableSet(enabled);
			this.capacity = capacity;
			this.demand = demand;
			this.used = used;
		}
	}

	public static Allocation allocate(int capacity, Collection<Device> devices) {
		List<Device> ordered = new ArrayList<>(devices);
		ordered.sort(
				Comparator.comparingInt((Device d) -> d.retentionPriority)
						.reversed()
						.thenComparingLong(d -> d.id));
		Set<Long> enabled = new HashSet<>();
		int demand = 0, used = 0;
		capacity = Math.max(0, capacity);
		for (Device device : ordered) {
			demand += device.slots;
			if (device.slots <= capacity - used) {
				enabled.add(device.id);
				used += device.slots;
			}
		}
		return new Allocation(enabled, capacity, demand, used);
	}
}

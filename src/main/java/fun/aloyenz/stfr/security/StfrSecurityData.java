package fun.aloyenz.stfr.security;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public final class StfrSecurityData extends WorldSavedData {

	private static final String NAME = "hbmstfr_security";
	private final Map<UUID, StfrAccessPolicy> networks = new LinkedHashMap<>();

	public StfrSecurityData() {
		super(NAME);
	}

	public StfrSecurityData(String name) {
		super(name);
	}

	public static StfrSecurityData get(World w) {
		StfrSecurityData data =
				(StfrSecurityData)
						w.getPerWorldStorage().getOrLoadData(StfrSecurityData.class, NAME);
		if (data == null) {
			data = new StfrSecurityData();
			w.getPerWorldStorage().setData(NAME, data);
		}
		return data;
	}

	public StfrAccessPolicy policy(UUID network) {
		return networks.computeIfAbsent(network, id -> new StfrAccessPolicy());
	}

	public void readFromNBT(NBTTagCompound tag) {
		networks.clear();
		NBTTagList list = tag.getTagList("networks", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound n = list.getCompoundTagAt(i);
			if (!n.hasUniqueId("id")) continue;
			StfrAccessPolicy policy = policy(n.getUniqueId("id"));
			NBTTagList cards = n.getTagList("cards", 10);
			for (int j = 0; j < cards.tagCount(); j++) {
				NBTTagCompound c = cards.getCompoundTagAt(j);
				if (c.hasUniqueId("id")) policy.grant(c.getUniqueId("id"), c.getInteger("rights"));
			}
			policy.setEnabled(n.getBoolean("enabled"));
		}
	}

	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		NBTTagList list = new NBTTagList();
		for (Map.Entry<UUID, StfrAccessPolicy> entry : networks.entrySet()) {
			NBTTagCompound n = new NBTTagCompound();
			n.setUniqueId("id", entry.getKey());
			n.setBoolean("enabled", entry.getValue().enabled());
			NBTTagList cards = new NBTTagList();
			for (Map.Entry<UUID, Integer> card : entry.getValue().cards().entrySet()) {
				NBTTagCompound c = new NBTTagCompound();
				c.setUniqueId("id", card.getKey());
				c.setInteger("rights", card.getValue());
				cards.appendTag(c);
			}
			n.setTag("cards", cards);
			list.appendTag(n);
		}
		tag.setTag("networks", list);
		return tag;
	}
}

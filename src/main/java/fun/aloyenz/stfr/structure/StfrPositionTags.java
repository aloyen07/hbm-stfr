package fun.aloyenz.stfr.structure;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class StfrPositionTags {

	private StfrPositionTags() {}

	public static NBTTagList write(Set<Long> positions) {
		NBTTagList list = new NBTTagList();
		for (long position : positions) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setLong("pos", position);
			list.appendTag(tag);
		}
		return list;
	}

	public static Set<Long> read(NBTTagList list) {
		Set<Long> result = new HashSet<>();
		for (int i = 0; i < list.tagCount(); i++)
			result.add(list.getCompoundTagAt(i).getLong("pos"));
		return result;
	}
}

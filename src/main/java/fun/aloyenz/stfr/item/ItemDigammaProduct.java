package fun.aloyenz.stfr.item;

import net.minecraft.item.Item;
import fun.aloyenz.stfr.block.STFRBlocks;

public final class ItemDigammaProduct extends Item {

	public ItemDigammaProduct(String namespace, String name) {
		setRegistryName(namespace, name);
		setTranslationKey("hbmstfr." + name);
		setCreativeTab(STFRBlocks.TAB);
	}
}

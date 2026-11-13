package fun.aloyenz.stfr.reactor;

import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.item.ItemStack;

/** HBM material quantities are 1/72 ingot, or 2 mB per material quantum. */
public final class StfrPoloniumFeed {

	private StfrPoloniumFeed() {}

	public static int volume(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() == ModItems.scraps) return 0;
		if (stack.getItem() == ModItems.pellet_rtg_polonium) {
			ItemRTGPellet pellet = (ItemRTGPellet) stack.getItem();
			long remaining =
					stack.hasTagCompound() && stack.getTagCompound().hasKey("PELLET_DEPLETION")
							? stack.getTagCompound().getLong("PELLET_DEPLETION")
							: pellet.getMaxLifespan();
			if (remaining <= 0) return 0;
			double fraction =
					Math.pow(
							.5D,
							Math.max(0L, pellet.getMaxLifespan() - remaining)
									/ (double) pellet.getHalfLife());
			return (int) Math.floor(288D * fraction);
		}
		if (stack.getItem() == ModItems.insert_polonium) return 1296;
		if (stack.getItem() == ModItems.billet_po210be) return 48;
		if (stack.getItem() == ModItems.rbmk_fuel_po210be) {
			double maximum = ModItems.rbmk_fuel_po210be.yield;
			double remaining =
					stack.hasTagCompound() && stack.getTagCompound().hasKey("yield")
							? stack.getTagCompound().getDouble("yield")
							: maximum;
			return (int) Math.floor(384D * Math.max(0D, Math.min(1D, remaining / maximum)));
		}
		if (stack.getItem() == ModItems.rbmk_pellet_po210be) {
			// Use HBM's SILEX recovery yield for the pellet's depletion tier.
			int tier = ItemRBMKPellet.rectify(stack.getMetadata()) % 5;
			return (45 - 10 * tier) * 16;
		}
		// Ore dictionary and material entries include generated fragments, powders,
		// billets, ingots, nuggets, blocks, and registered compatible shapes.
		long quanta = 0;
		for (Mats.MaterialStack material : Mats.getMaterialsFromItem(stack))
			if (material.material == Mats.MAT_POLONIUM) quanta += material.amount;
		return quanta > 0 && quanta <= Integer.MAX_VALUE / 2 ? (int) (quanta * 2) : 0;
	}
}

package fun.aloyenz.stfr.reactor;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;

public final class StfrAdvancements {

	private StfrAdvancements() {}

	public static void grant(World world, BlockPos center, String event) {
		if (world.isRemote || world.getMinecraftServer() == null) return;
		for (EntityPlayer player : world.playerEntities)
			if (player instanceof EntityPlayerMP && player.getDistanceSq(center) <= 128D * 128D) {
				grant((EntityPlayerMP) player, "reactor/root");
				grant((EntityPlayerMP) player, "reactor/" + event);
			}
	}

	private static void grant(EntityPlayerMP player, String name) {
		Advancement advancement =
				player.getServer()
						.getAdvancementManager()
						.getAdvancement(new ResourceLocation(Tags.MODID, name));
		if (advancement != null) player.getAdvancements().grantCriterion(advancement, "event");
	}
}

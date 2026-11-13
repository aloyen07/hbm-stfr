package fun.aloyenz.stfr.security;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public final class StfrSecurity {

	private StfrSecurity() {}

	public static UUID cardId(ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() != STFRItems.KEY_CARD) return null;
		NBTTagCompound tag = stack.getOrCreateSubCompound("hbmstfr");
		if (!tag.hasUniqueId("card")) tag.setUniqueId("card", UUID.randomUUID());
		return tag.getUniqueId("card");
	}

	public static UUID networkId(TileEntity tile) {
		NBTTagCompound tag = tile.getTileData();
		if (!tag.hasUniqueId("stfrNetworkIdentity")) {
			if (tile.getWorld().isRemote) return null;
			tag.setUniqueId("stfrNetworkIdentity", UUID.randomUUID());
			tile.markDirty();
		}
		return tag.getUniqueId("stfrNetworkIdentity");
	}

	public static boolean enrolled(TileEntity tile, UUID network) {
		return network != null
				&& tile.getTileData().hasUniqueId("stfrAuthorizedNetwork")
				&& network.equals(tile.getTileData().getUniqueId("stfrAuthorizedNetwork"));
	}

	public static void enroll(TileEntity tile, UUID network) {
		StfrLogicNetwork.invalidate(tile.getWorld());
		tile.getTileData().setUniqueId("stfrAuthorizedNetwork", network);
		tile.markDirty();
	}

	public static boolean allows(EntityPlayer p, World w, BlockPos pos, int right) {
		if (w.isRemote) return true;
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, pos);
		if (net.networkId == null) {
			TileEntity tile = w.getTileEntity(pos);
			if (tile == null || !tile.getTileData().hasUniqueId("stfrAuthorizedNetwork"))
				return true;
			StfrAccessPolicy previous =
					StfrSecurityData.get(w)
							.policy(tile.getTileData().getUniqueId("stfrAuthorizedNetwork"));
			return !previous.enabled() || playerCard(p, previous, right) != null;
		}
		StfrAccessPolicy policy = StfrSecurityData.get(w).policy(net.networkId);
		return !policy.enabled() || playerCard(p, policy, right) != null;
	}

	public static UUID playerCard(EntityPlayer p, StfrAccessPolicy policy, int rights) {
		for (ItemStack stack : new ItemStack[] {p.getHeldItemMainhand(), p.getHeldItemOffhand()}) {
			UUID card = cardId(stack);
			if (policy.allows(card, rights)) return card;
		}
		return null;
	}

	public static boolean useCard(EntityPlayer p, World w, BlockPos pos, ItemStack stack) {
		TileEntity tile = w.getTileEntity(pos);
		if (!(tile instanceof TileEntityLogicDevice)) return false;
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, pos);
		if (net.networkId == null) {
			message(p, "no_network");
			return true;
		}
		UUID card = cardId(stack);
		StfrSecurityData data = StfrSecurityData.get(w);
		StfrAccessPolicy policy = data.policy(net.networkId);
		boolean privileges =
				((TileEntityLogicDevice) tile).kind() == BlockLogicDevice.Kind.PRIVILEGES;
		if (privileges && !policy.initialized()) {
			policy.grant(card, StfrAccessPolicy.ALL);
			enroll(tile, net.networkId);
			if (net.mainframePos != null) enroll(w.getTileEntity(net.mainframePos), net.networkId);
			data.markDirty();
			message(p, "admin_created");
			return true;
		}
		if (!policy.allows(card, privileges ? StfrAccessPolicy.ADMIN : StfrAccessPolicy.CONNECT)) {
			message(p, "denied");
			return true;
		}
		enroll(tile, net.networkId);
		message(p, "connected");
		return true;
	}

	public static void message(EntityPlayer player, String key) {
		player.sendMessage(new TextComponentTranslation("message.hbmstfr.security." + key));
	}
}

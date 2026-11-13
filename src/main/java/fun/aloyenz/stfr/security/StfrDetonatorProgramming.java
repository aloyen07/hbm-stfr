package fun.aloyenz.stfr.security;

import java.util.UUID;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.sound.STFRSounds;

public final class StfrDetonatorProgramming {

	private StfrDetonatorProgramming() {}

	public static void insert(EntityPlayer p, World w, BlockPos pos, EnumHand hand) {
		TileEntity tile = w.getTileEntity(pos);
		if (tile == null) return;
		if (tile.getTileData().hasKey("stfrProgramming")) {
			StfrSecurity.message(p, "busy");
			return;
		}
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, pos);
		if (net.networkId == null || net.reactor == null) {
			StfrSecurity.message(p, "no_network");
			return;
		}
		UUID card =
				StfrSecurity.playerCard(
						p, StfrSecurityData.get(w).policy(net.networkId), StfrAccessPolicy.CONNECT);
		if (card == null) {
			StfrSecurity.message(p, "denied");
			return;
		}
		StfrSecurity.enroll(tile, net.networkId);
		if (!StfrLogicNetwork.inspect(w, pos).ioAvailable) {
			StfrSecurity.message(p, "no_network");
			return;
		}
		ItemStack stack = p.getHeldItem(hand);
		NBTTagCompound firmware = new NBTTagCompound();
		firmware.setUniqueId("network", net.networkId);
		firmware.setUniqueId("reactor", net.reactor.id);
		firmware.setUniqueId("card", card);
		firmware.setInteger("dimension", w.provider.getDimension());
		firmware.setLong("mainframe", net.mainframePos.toLong());
		firmware.setLong("center", net.reactor.center.toLong());
		firmware.setLong("connector", pos.toLong());
		firmware.setInteger("ticks", 40);
		firmware.setTag("item", stack.writeToNBT(new NBTTagCompound()));
		tile.getTileData().setTag("stfrProgramming", firmware);
		tile.markDirty();
		p.setHeldItem(hand, ItemStack.EMPTY);
		StfrSecurity.message(p, "programming");
	}

	public static void tick(TileEntity tile) {
		if (!tile.getTileData().hasKey("stfrProgramming")) return;
		if (!StfrLogicNetwork.inspect(tile.getWorld(), tile.getPos()).ioAvailable) return;
		NBTTagCompound data = tile.getTileData().getCompoundTag("stfrProgramming");
		int remaining = data.getInteger("ticks") - 1;
		data.setInteger("ticks", remaining);
		tile.markDirty();
		if (remaining > 0) return;
		World w = tile.getWorld();
		StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, tile.getPos());
		boolean valid =
				data.hasUniqueId("network")
						&& data.getUniqueId("network").equals(net.networkId)
						&& net.reactor != null
						&& net.reactor.id.equals(data.getUniqueId("reactor"))
						&& StfrSecurityData.get(w)
								.policy(net.networkId)
								.allows(data.getUniqueId("card"), StfrAccessPolicy.CONNECT);
		ItemStack stack = new ItemStack(data.getCompoundTag("item"));
		if (valid) {
			NBTTagCompound tag = stack.getOrCreateSubCompound("hbmstfr");
			tag.setUniqueId("network", net.networkId);
			tag.setUniqueId("reactor", net.reactor.id);
			tag.setUniqueId("device", UUID.randomUUID());
			tag.setInteger("dimension", w.provider.getDimension());
			tag.setLong("mainframe", data.getLong("mainframe"));
			tag.setLong("center", data.getLong("center"));
			tag.setLong("connector", tile.getPos().toLong());
		}
		tile.getTileData().removeTag("stfrProgramming");
		drop(w, tile.getPos(), stack);
		w.playSound(
				null,
				tile.getPos(),
				valid ? STFRSounds.ALARM_BEEP_2 : STFRSounds.ALARM_BEEP,
				SoundCategory.BLOCKS,
				2,
				valid ? 1 : .5F);
	}

	public static void eject(World w, BlockPos pos, TileEntity tile) {
		if (tile == null || !tile.getTileData().hasKey("stfrProgramming")) return;
		drop(
				w,
				pos,
				new ItemStack(
						tile.getTileData()
								.getCompoundTag("stfrProgramming")
								.getCompoundTag("item")));
		tile.getTileData().removeTag("stfrProgramming");
	}

	private static void drop(World w, BlockPos pos, ItemStack stack) {
		if (!stack.isEmpty())
			w.spawnEntity(
					new EntityItem(w, pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, stack));
	}
}

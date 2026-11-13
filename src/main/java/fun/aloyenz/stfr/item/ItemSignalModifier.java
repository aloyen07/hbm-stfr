package fun.aloyenz.stfr.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrLogicSyncPacket;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public final class ItemSignalModifier extends Item {

	public ItemSignalModifier() {
		setRegistryName(Tags.MODID, "signal_modifier");
		setTranslationKey(Tags.MODID + ".signal_modifier");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing face,
			float x,
			float y,
			float z) {
		if (!(world.getTileEntity(pos) instanceof TileEntityLogicDevice))
			return EnumActionResult.PASS;
		TileEntityLogicDevice device = (TileEntityLogicDevice) world.getTileEntity(pos);
		if (device.kind() != BlockLogicDevice.Kind.TRANSMITTER
				&& device.kind() != BlockLogicDevice.Kind.MONITOR) return EnumActionResult.PASS;
		if (!world.isRemote && !StfrSecurity.allows(player, world, pos, StfrAccessPolicy.CONTROL))
			return EnumActionResult.FAIL;
		if (!world.isRemote)
			StfrLogicPacket.CHANNEL.sendTo(
					new StfrLogicSyncPacket(pos, face, device), (EntityPlayerMP) player);
		return EnumActionResult.SUCCESS;
	}
}

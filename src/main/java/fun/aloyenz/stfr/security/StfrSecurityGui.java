package fun.aloyenz.stfr.security;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import fun.aloyenz.stfr.client.gui.GuiOperatorBookMk2;
import fun.aloyenz.stfr.client.gui.GuiSolidInjector;
import fun.aloyenz.stfr.client.gui.GuiStfrSecurity;
import fun.aloyenz.stfr.inventory.ContainerSolidInjector;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;

public final class StfrSecurityGui implements IGuiHandler {

	public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
		if (id == 3) return null;
		if (id == 2) {
			TileEntity tile = w.getTileEntity(new BlockPos(x, y, z));
			if (!(tile instanceof TileEntitySolidInjector)) return null;
			ContainerSolidInjector container =
					new ContainerSolidInjector(p, (TileEntitySolidInjector) tile);
			return container.canInteractWith(p) ? container : null;
		}
		if (id != 0 && id != 1 || id == 1 && (x < 0 || x >= p.inventory.getSizeInventory()))
			return null;
		BlockPos pos = new BlockPos(x, y, z);
		if (id == 0 && !StfrSecurity.allows(p, w, pos, StfrAccessPolicy.ADMIN)) return null;
		ContainerStfrSecurity container = new ContainerStfrSecurity(p, id == 1, pos, x);
		return container.canInteractWith(p) ? container : null;
	}

	public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
		if (id == 3)
			return p.getHeldItem(x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND).getItem()
						== STFRItems.OPERATOR_BOOK_MK2
					? new GuiOperatorBookMk2(true)
					: p.getHeldItem(x == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND).getItem()
						== STFRItems.OPERATOR_BOOK ? new GuiOperatorBookMk2(false) : null;
		if (id == 2) {
			TileEntity tile = w.getTileEntity(new BlockPos(x, y, z));
			return tile instanceof TileEntitySolidInjector
					? new GuiSolidInjector(
							new ContainerSolidInjector(p, (TileEntitySolidInjector) tile))
					: null;
		}
		return new GuiStfrSecurity(new ContainerStfrSecurity(p, id == 1, new BlockPos(x, y, z), x));
	}
}

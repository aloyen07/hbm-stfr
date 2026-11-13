package fun.aloyenz.stfr.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;

public final class RenderFuelSlot extends TileEntitySpecialRenderer<TileEntityFuelSlot> {

	@Override
	public void render(
			TileEntityFuelSlot tile,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		ItemStack capsule = tile.getCapsule();
		if (capsule.isEmpty() || tile.getWorld() == null) return;
		IBlockState slot = tile.getWorld().getBlockState(tile.getPos());
		EnumFacing inward = slot.getValue(BlockFuelSlot.FACING);
		EnumFacing outward = inward.getOpposite();
		float slide = tile.getRenderProgress(partialTicks);

		GlStateManager.pushMatrix();
		GlStateManager.translate(
				x + 0.5D + outward.getXOffset() * slide,
				y + 0.5D + outward.getYOffset() * slide,
				z + 0.5D + outward.getZOffset() * slide);
		if (inward.getAxis().isVertical()) {
			EnumFacing handle = slot.getValue(BlockFuelSlot.HANDLE_FACING);
			float yaw =
					handle == EnumFacing.EAST
							? 90F
							: handle == EnumFacing.SOUTH
									? 180F
									: handle == EnumFacing.WEST ? 270F : 0F;
			GlStateManager.rotate(yaw, 0F, 1F, 0F);
		}
		rotateLocalPositiveZTo(outward);
		// Reverse the capsule along the receptacle axis: TERF's item model has
		// its valve end on local +Z, while the slot expects that end inward.
		GlStateManager.rotate(180F, 0F, 1F, 0F);
		GlStateManager.scale(1F, 1F, 1F);
		Minecraft.getMinecraft()
				.getRenderItem()
				.renderItem(capsule, ItemCameraTransforms.TransformType.NONE);
		GlStateManager.popMatrix();
	}

	private static void rotateLocalPositiveZTo(EnumFacing facing) {
		switch (facing) {
			case NORTH:
				GlStateManager.rotate(180F, 0F, 1F, 0F);
				break;
			case EAST:
				GlStateManager.rotate(90F, 0F, 1F, 0F);
				break;
			case WEST:
				GlStateManager.rotate(-90F, 0F, 1F, 0F);
				break;
			case UP:
				GlStateManager.rotate(-90F, 1F, 0F, 0F);
				break;
			case DOWN:
				GlStateManager.rotate(90F, 1F, 0F, 0F);
				break;
			default:
				break;
		}
	}
}

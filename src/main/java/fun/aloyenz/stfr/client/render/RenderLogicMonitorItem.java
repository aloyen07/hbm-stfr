package fun.aloyenz.stfr.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Item renderer uses the original rectangular entity texture without atlas resampling. */
public final class RenderLogicMonitorItem extends TileEntityItemStackRenderer {

	private static final ResourceLocation TEXTURE =
			new ResourceLocation("hbmstfr", "textures/entity/logic_monitor_sign.png");
	private final ModelSign sign = new ModelSign();

	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.5, 0.5, 0.5);
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		sign.signStick.showModel = true;
		GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
		sign.renderSign();
		GlStateManager.popMatrix();
	}
}

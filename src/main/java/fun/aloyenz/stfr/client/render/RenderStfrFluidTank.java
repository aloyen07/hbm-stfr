package fun.aloyenz.stfr.client.render;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.tileentity.RenderFluidTank;
import com.hbm.tileentity.IPersistentNBT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.StfrFluids;

/**
 * Adds a tinted HBM tank body for yaronite without a texture per fluid. All other fluids retain
 * HBM's renderer and textures. GUI tint stays white because the separately drawn gauge PNG already
 * contains its own colors.
 */
public final class RenderStfrFluidTank extends RenderFluidTank {

	private static final ResourceLocation YARONITE_BODY =
			new ResourceLocation("hbm", "textures/models/tank/tank_STFR_YARONITE.png");
	private static final ResourceLocation NEUTRAL_BODY =
			new ResourceLocation("hbm", "textures/models/tank/tank_NONE.png");

	private static void bind(ResourceLocation texture) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
	}

	private static void tintYaronite() {
		int color = StfrFluids.YARONITE.getColor();
		GlStateManager.color(
				((color >> 16) & 255) / 255F,
				((color >> 8) & 255) / 255F,
				(color & 255) / 255F,
				1F);
	}

	@Override
	protected void bindTexture(ResourceLocation texture) {
		if (YARONITE_BODY.equals(texture)) {
			// HBM already drew its frame. Only the separate Tank part is tinted.
			bind(NEUTRAL_BODY);
			tintYaronite();
		} else {
			super.bindTexture(texture);
		}
	}

	@Override
	public ItemRenderBase getRenderer(Item item) {
		ItemRenderBase original = super.getRenderer(item);
		return new ItemRenderBase() {
			@Override
			public void renderInventory() {
				original.renderInventory();
			}

			@Override
			public void renderCommon(ItemStack stack) {
				FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 0);
				boolean exploded = false;
				if (stack.hasTagCompound()
						&& stack.getTagCompound().hasKey(IPersistentNBT.NBT_PERSISTENT_KEY)) {
					NBTTagCompound data =
							stack.getTagCompound()
									.getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY);
					tank.readFromNBT(data, "tank");
					exploded = data.getBoolean("hasExploded");
				}
				if (tank.getTankType() != StfrFluids.YARONITE) {
					original.renderCommon(stack);
					return;
				}
				GlStateManager.rotate(90, 0, 1, 0);
				GlStateManager.scale(0.75, 0.75, 0.75);
				GlStateManager.shadeModel(GL11.GL_SMOOTH);
				GlStateManager.disableCull();
				GlStateManager.color(1F, 1F, 1F, 1F);
				bind(ResourceManager.tank_tex);
				if (!exploded) {
					ResourceManager.fluidtank.renderPart("Frame");
					bind(NEUTRAL_BODY);
					tintYaronite();
					ResourceManager.fluidtank.renderPart("Tank");
				} else {
					ResourceManager.fluidtank_exploded.renderPart("Frame");
					bind(ResourceManager.tank_inner_tex);
					ResourceManager.fluidtank_exploded.renderPart("TankInner");
					bind(NEUTRAL_BODY);
					tintYaronite();
					ResourceManager.fluidtank_exploded.renderPart("Tank");
				}
				GlStateManager.color(1F, 1F, 1F, 1F);
				GlStateManager.enableCull();
				GlStateManager.shadeModel(GL11.GL_FLAT);
			}
		};
	}
}

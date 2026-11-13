package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

final class StfrScaledIcon implements IDrawable {
	private final IDrawable drawable;
	private final float scale;

	StfrScaledIcon(IDrawable drawable, float scale) {
		this.drawable = drawable;
		this.scale = scale;
	}

	@Override
	public int getWidth() {
		return Math.round(drawable.getWidth() * scale);
	}

	@Override
	public int getHeight() {
		return Math.round(drawable.getHeight() * scale);
	}

	@Override
	public void draw(Minecraft minecraft, int xOffset, int yOffset) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(xOffset, yOffset, 0);
		GlStateManager.scale(scale, scale, 1);
		drawable.draw(minecraft, 0, 0);
		GlStateManager.popMatrix();
	}
}

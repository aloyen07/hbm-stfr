package fun.aloyenz.stfr.client.jei;

import static fun.aloyenz.stfr.block.STFRBlocks.*;
import static fun.aloyenz.stfr.client.jei.StfrJeiPlugin.*;

import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.*;
import mezz.jei.api.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

final class StfrInjectorCategory implements IRecipeCategory<Card> {

	final String uid;
	final IDrawable background, icon;

	StfrInjectorCategory(IGuiHelper helper, String uid, ItemStack icon) {
		this.uid = uid;
		this.background = helper.createBlankDrawable(176, 124);
		this.icon = helper.createDrawableIngredient(icon);
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public String getTitle() {
		return I18n.format("jei." + uid);
	}

	@Override
	public String getModName() {
		return "STFR";
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void drawExtras(Minecraft mc) {
		Gui.drawRect(4, 14, 24, 34, 0xFFE0E0E0);
		Gui.drawRect(132, 14, 152, 34, 0xFFE0E0E0);
		mc.fontRenderer.drawString("->", 100, 20, 0x404040);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, Card card, IIngredients ingredients) {
		IGuiItemStackGroup stacks = layout.getItemStacks();
		for (int i = 0; i < card.inputs.size(); i++) {
			stacks.init(i, true, 6 + 22 * i, 15);
			stacks.set(i, card.inputs.get(i));
		}
		if (!card.output.isEmpty()) {
			stacks.init(card.inputs.size(), false, 133, 15);
			stacks.set(card.inputs.size(), card.output);
		}
		stacks.addTooltipCallback(
				(slot, input, stack, tooltip) -> {
					if (INJECTOR.equals(uid))
						tooltip.add(I18n.format("jei.hbmstfr.injector.depletion"));
				});
	}
}

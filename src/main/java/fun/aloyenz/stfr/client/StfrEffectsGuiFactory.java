package fun.aloyenz.stfr.client;

import java.util.Collections;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import fun.aloyenz.stfr.StfrClientEffectsConfig;
import fun.aloyenz.stfr.Tags;

public final class StfrEffectsGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft minecraft) {}
    public boolean hasConfigGui() { return true; }
    public GuiScreen createConfigGui(GuiScreen parent) { return screen(parent); }
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return Collections.emptySet(); }
    public static GuiScreen screen(GuiScreen parent) {
        return new GuiConfig(parent,new ConfigElement(StfrClientEffectsConfig.configuration().getCategory("effects")).getChildElements(),
            Tags.MODID,false,false,I18n.format("config.hbmstfr.effects.title"));
    }
}

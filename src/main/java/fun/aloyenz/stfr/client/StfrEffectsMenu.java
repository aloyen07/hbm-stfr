package fun.aloyenz.stfr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;

@Mod.EventBusSubscriber(modid=Tags.MODID,value=Side.CLIENT)
public final class StfrEffectsMenu {
    private static final int ID=0x535446;
    @SubscribeEvent public static void init(GuiScreenEvent.InitGuiEvent.Post e) {
        if(e.getGui() instanceof GuiIngameMenu)
            e.getButtonList().add(new GuiButton(ID, e.getGui().width-104,6,98,20,I18n.format("config.hbmstfr.effects.button")));
    }
    @SubscribeEvent public static void click(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if(e.getGui() instanceof GuiIngameMenu && e.getButton().id==ID)
            Minecraft.getMinecraft().displayGuiScreen(StfrEffectsGuiFactory.screen(e.getGui()));
    }
}

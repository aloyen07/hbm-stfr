package fun.aloyenz.stfr;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Local rendering only; never sent to a server or used by reactor physics. */
@Mod.EventBusSubscriber(modid=Tags.MODID)
public final class StfrClientEffectsConfig {
    private static Configuration config;
    public static boolean lighting=true, star=true, exposure=true;
    public static int quality=32, radius=48;
    public static float exposureStrength=.75F, intensity=1;
    public static void load(File directory) {
        config=new Configuration(new File(directory,"hbmstfr-client.cfg"));
        config.load();sync();
    }
    public static Configuration configuration() { return config; }
    public static void sync() {
        if(config==null)return;
        lighting=config.getBoolean("lighting","effects",true,"Illuminate surfaces and cast shadows from an uncontrolled core.","config.hbmstfr.effects.lighting");
        star=config.getBoolean("star","effects",true,"Draw the visible uncontrolled core star.","config.hbmstfr.effects.star");
        exposure=config.getBoolean("exposure","effects",true,"Adapt exposure to visible core light, including illuminated surfaces.","config.hbmstfr.effects.exposure");
        quality=config.getInt("quality","effects",32,16,64,"Shadow resolution per cube face. Rounded to 16, 32 or 64.","config.hbmstfr.effects.quality");
        quality=quality<24?16:quality<48?32:64;
        radius=config.getInt("radius","effects",48,8,96,"Light radius in blocks.","config.hbmstfr.effects.radius");
        exposureStrength=config.getFloat("exposureStrength","effects",.75F,0,1,"Maximum exposure adaptation.","config.hbmstfr.effects.exposureStrength");
        intensity=config.getFloat("intensity","effects",1,.1F,3,"Core light and star intensity.","config.hbmstfr.effects.intensity");
        config.getCategory("effects").setRequiresWorldRestart(false).setRequiresMcRestart(false);
        if(config.hasChanged())config.save();
    }
    @SubscribeEvent public static void changed(ConfigChangedEvent.OnConfigChangedEvent event) {
        if(Tags.MODID.equals(event.getModID()))sync();
    }
}

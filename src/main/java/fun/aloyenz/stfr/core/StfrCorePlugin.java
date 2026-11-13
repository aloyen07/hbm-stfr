package fun.aloyenz.stfr.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("fun.aloyenz.stfr.core")
public final class StfrCorePlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public String[] getASMTransformerClass() { return new String[0]; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {}
    public String getAccessTransformerClass() { return null; }
    public List<String> getMixinConfigs() { return Collections.singletonList("hbmstfr.early.mixin.json"); }
}

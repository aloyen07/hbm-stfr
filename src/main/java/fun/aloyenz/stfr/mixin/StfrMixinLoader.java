package fun.aloyenz.stfr.mixin;

import java.util.Collections;
import java.util.List;
import zone.rong.mixinbooter.ILateMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

/** Loads STFR's HBM block integration after HBM has been discovered. */
@MixinLoader
public final class StfrMixinLoader implements ILateMixinLoader {
	@Override
	public List<String> getMixinConfigs() {
		return Collections.singletonList("hbmstfr.mixin.json");
	}
}

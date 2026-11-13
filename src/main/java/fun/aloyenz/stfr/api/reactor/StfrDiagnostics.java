package fun.aloyenz.stfr.api.reactor;

import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrComponentStatus;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Server-side read API for the reactor's latest component diagnostics. */
public final class StfrDiagnostics {

	private StfrDiagnostics() {}

	@Nullable
	public static StfrComponentStatus get(World world, BlockPos center, EnumFacing turbine) {
		if (world.isRemote) return null;
		StfrReactor reactor = StfrManager.get(world).get(center);
		if (reactor == null) return null;
		BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
		if (world.isAreaLoaded(origin, origin.add(28, 28, 28))) reactor.refresh(world);
		return reactor.componentStatus(turbine);
	}
}

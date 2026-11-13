package fun.aloyenz.stfr.reactor.states;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.*;

final class StfrManualRestabilizationState {

	private StfrManualRestabilizationState() {}

	static void enter(StfrEmergencyController c, World w) {
		for (TileEntity tile : w.loadedTileEntityList)
			if (tile instanceof TileEntityLogicDevice) {
				StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspectRaw(w, tile.getPos());
				if (net.reactor == c.r && net.networkId != null) {
					c.manualNetwork = net.networkId;
					break;
				}
			}
		StfrLogicNetwork.openBreakers(w, c.r);
		c.r.setBreakersOn(false);
		c.music(w, "music.hacknet_ost_carpenter_brut", 16, 1);
		c.repeated(w, "music.hacknet_music_end", 3, 8, 1);
	}

	static void tick(StfrEmergencyController c, World w, int tick) {
		c.r.core().eventShield(Math.max(0, (100000 - tick * 27) / 10000D));
		c.r.core().eventBrake(60000);
		c.r.core()
				.eventHeat(
						c.r.core().coreTemperature() >= 25000 ? -5216.375 : 0,
						c.r.core().caseTemperature() > 134.6743 ? -134.6743 : 0);
		if (tick == 340) c.burst(w);
		if (c.stage == 0 && tick >= 351 && !c.mainframeConnected(w)) c.nextStage(w);
		else if (c.stage == 1 && c.breakersConnected(w)) c.nextStage(w);
		else if (c.stage == 2 && c.mainframeConnected(w)) c.nextStage(w);
		else if (c.stage == 3 && c.lensesInMode(w, false) >= 6) c.nextStage(w);
		if (c.stage == 5 && StfrEmergencyRules.manualRecoveryElapsed(c.recovery) >= 600) {
			c.broadcast(
					w,
					"stfr.manual_restab.restabilize",
					"The Fusion Reactor Core Has Been Restabilized Successfully! Please Shut Down"
						+ " The Fusion Reactor For Immediate Repairs.",
					1);
			c.finish(w, "quick_thinking");
		} else if (tick >= 3710) {
			c.manualFailed = true;
			c.start(w, OVERLOAD);
			c.r.emergencyTick(90, 300);
			c.broadcast(
					w,
					"stfr.manual_restab.fail",
					"Manual Reactor Restabilization Failure! Auxiliary Shield Lost. Total"
						+ " Containment Failure Imminent!",
					3);
		}
	}
}

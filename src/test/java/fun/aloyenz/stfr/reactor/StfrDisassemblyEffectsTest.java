package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.*;
import fun.aloyenz.stfr.sound.StfrEventGate;

final class StfrDisassemblyEffectsTest {
	@Test void radialOrderStartsAtCoreAndTreatsAllSixDirectionsEqually() {
		List<BlockPos> input=new ArrayList<>(Arrays.asList(new BlockPos(0,-14,0),
				new BlockPos(0,1,0),new BlockPos(0,-1,0),new BlockPos(1,0,0),
				new BlockPos(-1,0,0),new BlockPos(0,0,1),new BlockPos(0,0,-1),BlockPos.ORIGIN));
		List<BlockPos> ordered=StfrDisassemblyOrder.centerOut(input,p->p,BlockPos.ORIGIN);
		assertEquals(BlockPos.ORIGIN,ordered.get(0));
		assertEquals(new BlockPos(0,-14,0),ordered.get(7));
		assertEquals(6,new HashSet<>(ordered.subList(1,7)).size());
		for (int i=1;i<ordered.size();i++) assertTrue(ordered.get(i).distanceSq(BlockPos.ORIGIN)
				>=ordered.get(i-1).distanceSq(BlockPos.ORIGIN));
		assertEquals(ordered,StfrDisassemblyOrder.centerOut(input,p->p,BlockPos.ORIGIN));
		assertEquals(input.size(),new HashSet<>(ordered).size());
	}

	@Test void vortexMatchesRotorDirectionAndNeverAcceleratesWithoutLimit() {
		Vec3d position=new Vec3d(4,0,0),velocity=Vec3d.ZERO;
		assertTrue(StfrVortex.velocity(position,velocity,false).z>0);
		for(int tick=0;tick<1200;tick++) {
			velocity=StfrVortex.velocity(position,velocity,tick>=600);
			assertTrue(Double.isFinite(velocity.x)&&Double.isFinite(velocity.z));
			assertTrue(Math.hypot(velocity.x,velocity.z)<=.700001);
			assertTrue(velocity.y>0&&velocity.y<=.28);
			position=position.add(velocity);
		}
	}

	@Test void ionHitsThinWallStopsAndLeavesDelayedFadingHistory() {
		StfrIonTrack track=new StfrIonTrack(Vec3d.ZERO,new Vec3d(1,0,0));
		track.tick(1,(from,to)->new Vec3d(.1,0,0));
		assertFalse(track.flying);
		assertEquals(.1,track.position.x,1e-9);
		assertFalse(track.samples.isEmpty());
		assertEquals(0,StfrIonTrack.alpha(0));
		assertEquals(0,StfrIonTrack.alpha(1.99));
		assertTrue(StfrIonTrack.alpha(5)>StfrIonTrack.alpha(20));
		assertEquals(0,StfrIonTrack.alpha(32));
		for(int tick=2;tick<40;tick++) track.tick(tick,(from,to)->fail("Dead particles must not travel"));
		assertTrue(track.expired());
	}

	@Test void ionFlyingInAirBuildsItsTrailProgressivelyAndHasBoundedLifetime() {
		StfrIonTrack track=new StfrIonTrack(Vec3d.ZERO,new Vec3d(1,.2,0));
		track.tick(1,(a,b)->null);
		assertEquals(3,track.samples.size());
		assertTrue(track.flying);
		for(int tick=2;tick<90;tick++) {
			track.tick(tick,(a,b)->null);
			assertTrue(track.samples.size()<=99);
		}
		assertTrue(track.expired());
	}

	@Test void fiveHundredMainframesStillProduceOneEventPerStageAndReactor() {
		StfrEventGate gate=new StfrEventGate();
		int emitted=0;
		for(int i=0;i<500;i++) if(gate.accept("reactorA:1300",100)) emitted++;
		assertEquals(1,emitted);
		assertTrue(gate.accept("reactorA:1160",240));
		assertTrue(gate.accept("reactorB:1300",240));
		assertFalse(gate.accept("reactorB:1300",241));
		assertTrue(gate.accept("reactorB:1300",400));
	}

	@Test void blastFalloffRespectsStrengthDistanceAndCover() {
		assertEquals(0,StfrBlastDamage.damage(4,8,1));
		assertEquals(0,StfrBlastDamage.damage(4,1,0));
		assertTrue(StfrBlastDamage.damage(4,1,1)>StfrBlastDamage.damage(4,6,1));
		assertTrue(StfrBlastDamage.damage(6,2,1)>StfrBlastDamage.damage(3,2,1));
		assertTrue(StfrBlastDamage.damage(4,2,.2)<StfrBlastDamage.damage(4,2,1));
	}
}

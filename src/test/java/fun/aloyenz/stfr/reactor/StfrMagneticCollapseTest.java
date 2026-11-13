package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;

class StfrMagneticCollapseTest {
	@Test void fragmentsRemainInSwarmAndAreNotAbsorbed() {
		for(Vec3d axis:new Vec3d[]{new Vec3d(1,0,0),new Vec3d(-1,0,0),new Vec3d(0,1,0),
				new Vec3d(0,-1,0),new Vec3d(0,0,1),new Vec3d(0,0,-1),new Vec3d(1,1,1)}) {
			Vec3d position=axis.scale(13),velocity=Vec3d.ZERO;
			int ticks=0;
			for(;ticks<1200;ticks++) {
				velocity=StfrMagneticCollapse.swarm(position,velocity,ticks,71);
				assertTrue(velocity.length()<=.85001);
				position=position.add(velocity);
				if(ticks>=300)assertTrue(position.length()>1.3 && position.length()<2.4,"Fragments must hug the solid core: "+position);
			}
			assertTrue(position.length()>1 && position.length()<10,"Fragment must remain in a bounded swarm: "+position);
		}
	}
	@Test void dischargeGapsVaryWhileSoundAndFlashShareTheSameOnset() {
		Set<Integer> gaps=new HashSet<>();
		int previous=-1,bolts=0;
		for(int tick=1000;tick<3000;tick++) {
			if(StfrMagneticCollapse.dischargeStart(StfrReactor.RunState.UNDERLOAD,tick)!=tick)continue;
			assertEquals(1,StfrMagneticCollapse.discharge(StfrReactor.RunState.UNDERLOAD,tick));
			if(previous>=0)gaps.add(tick-previous);
			previous=tick;bolts+=StfrMagneticCollapse.boltCount(StfrReactor.RunState.UNDERLOAD,tick);
		}
		assertTrue(gaps.size()>5,"No fixed electrical rhythm");
		assertTrue(bolts>300 && bolts<600,"Expected about 3–6 bolts per second, got "+bolts/100D);
	}
	@Test void lightReturnsSmoothlyToBaselineAfterIsolatedDischarge() {
		int pulse=1000;
		while(!(StfrMagneticCollapse.dischargeStart(StfrReactor.RunState.UNDERLOAD,pulse)==pulse
				&& StfrMagneticCollapse.dischargeStart(StfrReactor.RunState.UNDERLOAD,pulse+6)==pulse))pulse++;
		double previous=StfrMagneticCollapse.light(StfrReactor.RunState.UNDERLOAD,pulse);
		for(double t=pulse+.05;t<pulse+6;t+=.05) {
			double value=StfrMagneticCollapse.light(StfrReactor.RunState.UNDERLOAD,t);
			assertTrue(value>=.06 && value<=previous);
			assertTrue(previous-value<.015,"No blackout at end of discharge");previous=value;
		}
	}
	@Test void randomDetachmentHasIncreasedMeanRateAndIrregularGaps() {
		assertEquals(.1875,StfrMagneticCollapse.detachmentChance(1000));
		assertEquals(.375,StfrMagneticCollapse.detachmentChance(2000));
		Random random=new Random(935);
		int count=0,last=-1;Set<Integer> gaps=new HashSet<>();
		for(int tick=0;tick<10000;tick++)if(random.nextDouble()<StfrMagneticCollapse.detachmentChance(2000)) {
			count++;if(last>=0)gaps.add(tick-last);last=tick;
		}
		assertTrue(count>3500 && count<4000);assertTrue(gaps.size()>5);
	}
	@Test void fieldGrowsTowardCoreAndNeverTeleportsPlayers() {
		double near=StfrMagneticCollapse.velocity(new Vec3d(2,0,0),Vec3d.ZERO,1,false).length();
		double far=StfrMagneticCollapse.velocity(new Vec3d(14,0,0),Vec3d.ZERO,1,false).length();
		assertTrue(near>far*10);
		assertEquals(new Vec3d(.1,.2,.3),StfrMagneticCollapse.velocity(new Vec3d(17,0,0),new Vec3d(.1,.2,.3),1,false));
		assertTrue(StfrMagneticCollapse.velocity(new Vec3d(1,0,0),new Vec3d(9,9,9),1,false).length()<=.65001);
		assertTrue(StfrMagneticCollapse.velocity(new Vec3d(0,8,0),Vec3d.ZERO,1,false).y<0,"Above core pulls down");
		assertTrue(StfrMagneticCollapse.velocity(new Vec3d(0,-8,0),Vec3d.ZERO,1,false).y>0,"Below core pulls up");
	}
	@Test void shellNeverSelectsBlocksBeyondTheCurrentRadius() {
		assertEquals(-1,StfrMagneticCollapse.shellBand(5,9));
		assertEquals(0,StfrMagneticCollapse.shellBand(5,20));
		assertEquals(1,StfrMagneticCollapse.shellBand(5,26));
		assertEquals(0,StfrMagneticCollapse.shellBand(6,26));
		assertEquals(-1,StfrMagneticCollapse.shellBand(7,26));
	}
	@Test void coreRepelsDebrisInEveryDirection() {
		for(Vec3d point:new Vec3d[]{new Vec3d(1,0,0),new Vec3d(-1,0,0),new Vec3d(0,1,0),new Vec3d(0,-1,0),new Vec3d(0,0,1)}) {
			Vec3d next=StfrMagneticCollapse.swarm(point,point.scale(-.4),200,42);
			assertTrue(next.dotProduct(point)>0);
		}
	}
	@Test void warningMatchesThirtySecondsAndStrongLightIsReservedForFinalCharge() {
		assertEquals(30*20,StfrMagneticCollapse.LOSS_END-StfrMagneticCollapse.LOSS_WARNING);
		StfrEmergencyTimeline.Event warning=
				StfrEmergencyTimeline.EVENTS.stream()
				.filter(e -> e.state()==StfrReactor.RunState.REACTION_LOSS && e.voice().equals("stfr.reaction_loss.instability"))
				.findFirst().orElseThrow();
		assertEquals(StfrMagneticCollapse.LOSS_WARNING,warning.tick());
		assertTrue(StfrEmergencyTimeline.SOUNDS.stream()
				.anyMatch(e -> e.state()==StfrReactor.RunState.UNDERLOAD && e.tick()==320
						&& e.id().equals("terf:music.remi_galleno_panic_track")));
		assertEquals(0,StfrMagneticCollapse.discharge(StfrReactor.RunState.UNDERLOAD,919));
		long chargeSounds=StfrEmergencyTimeline.SOUNDS.stream()
				.filter(e -> e.state()==StfrReactor.RunState.UNDERLOAD && e.id().contains("mega_chargeup")).count();
		assertEquals(1,chargeSounds,"Do not stack four copies of mega_chargeup");
		for(int t=30;t<3400;t++)
			assertTrue(StfrMagneticCollapse.light(StfrReactor.RunState.UNDERLOAD,t)
					<(StfrMagneticCollapse.exposurePulse(StfrReactor.RunState.UNDERLOAD,t)?1.2:.5),"Early overexposure at "+t);
		assertTrue(StfrMagneticCollapse.light(StfrReactor.RunState.UNDERLOAD,3639)>1.4);
		int pulse=1850;
		while(!(StfrMagneticCollapse.dischargeStart(StfrReactor.RunState.REACTION_LOSS,pulse)==pulse
				&& StfrMagneticCollapse.dischargeStart(StfrReactor.RunState.REACTION_LOSS,pulse+6)==pulse))pulse++;
		assertTrue(StfrMagneticCollapse.light(StfrReactor.RunState.REACTION_LOSS,pulse)
				>StfrMagneticCollapse.light(StfrReactor.RunState.REACTION_LOSS,pulse+6));
		assertEquals(0,StfrMagneticCollapse.vibration(719,9));
		assertTrue(Math.abs(StfrMagneticCollapse.vibration(900,9))<.07);
	}
}

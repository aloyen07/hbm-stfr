package fun.aloyenz.stfr.reactor.states;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import fun.aloyenz.stfr.reactor.StfrAdvancements;
import fun.aloyenz.stfr.sound.STFRSounds;

final class StfrUnderloadState {
	private StfrUnderloadState() {}
	static void enter(StfrEmergencyController c,World w) {
		c.stopMusic(w);
		c.r.core().forceOffline();
		StfrReactionLossEffects.transition(c,w);
		StfrAdvancements.grant(w,c.r.center,"underload");
	}
	static void tick(StfrEmergencyController c,World w,int tick) {
		if(tick%8==0)
			for(int bell=0;bell<2;bell++)
				c.r.mainframeSound(w,SoundEvents.BLOCK_NOTE_BELL,SoundCategory.MASTER,5,.5F);
		if(tick>=1130 && (tick-1130)%240==0)c.sound(w,"alarms.airraidsiren",SoundCategory.MASTER,50,1);
		if(tick>=1130 && tick<=1980 && (w.getTotalWorldTime()+7)%20==0)
			c.r.eventSound(w,STFRSounds.ALARM_3,SoundCategory.AMBIENT,8,.8F);
		if(w.getTotalWorldTime()%24==0)c.sound(w,"alarms.beep3",SoundCategory.AMBIENT,8,0);
		StfrReactionLossEffects.tick(c,w,tick,true);
		c.r.failure().magneticTick(w,tick);
		if(tick==1980)StfrReactionLossEffects.transition(c,w);
		if(tick==1982)c.r.failure().crashMainframes(w,3640-1982);
		if(tick>=3640)c.detonate(w);
	}
}

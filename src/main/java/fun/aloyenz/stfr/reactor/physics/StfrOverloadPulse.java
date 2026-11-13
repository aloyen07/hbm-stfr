package fun.aloyenz.stfr.reactor.physics;

import fun.aloyenz.stfr.reactor.StfrReactor;

/** One independent trial per server tick; recovery never produces a pulse. */
public final class StfrOverloadPulse {
    public static final int FLASH_TICKS=2;
    private StfrOverloadPulse() {}
    public static boolean triggers(StfrReactor.RunState state,boolean recovering,double roll) {
        return state==StfrReactor.RunState.OVERLOAD && !recovering && roll>=0 && roll<.01;
    }
    public static boolean active(long now,long until) { return now<until; }
}

package fun.aloyenz.stfr.sound;

import fun.aloyenz.stfr.reactor.StfrReactor;

/** Accent sounds use music delivery but do not occupy the music queue. */
public final class StfrMusicPolicy {
    private StfrMusicPolicy() {}
    public static boolean allowed(StfrReactor.RunState state) {
        return state!=StfrReactor.RunState.MELTDOWN
            && state!=StfrReactor.RunState.DETONATION;
    }
    public static boolean accent(String sound) { return sound.endsWith(":effects.braam"); }
    public static boolean blocks(String incoming,String playing) { return !accent(incoming) && !accent(playing); }
}

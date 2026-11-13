package fun.aloyenz.stfr.client.lighting;

import fun.aloyenz.stfr.reactor.StfrReactor;

/** Monotonic wall-clock deadline; rendering frequency and world ticks cannot extend it. */
public final class StfrFlashClock {
    private StfrFlashClock() {}
    public static long now(StfrReactor.RunState stage,long nanos,long worldTicks) {
        return stage==StfrReactor.RunState.SELF_DESTRUCT?nanos:worldTicks*50_000_000L;
    }
    public static long deadline(long now,int milliseconds) { return now+Math.max(1,Math.min(1000,milliseconds))*1_000_000L; }
    public static boolean active(long now,long deadline) { return now<deadline; }
}

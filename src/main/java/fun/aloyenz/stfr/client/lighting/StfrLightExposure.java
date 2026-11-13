package fun.aloyenz.stfr.client.lighting;

/** Mean screen-space core radiance includes illuminated surfaces, not the camera/core angle. */
public final class StfrLightExposure {
    private StfrLightExposure() {}
    public static boolean onScreen(float x,float y,float z,float w) {
        return Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z)&&Float.isFinite(w)
            && w>0 && Math.abs(x)<=w && Math.abs(y)<=w && Math.abs(z)<=w;
    }
    public static float target(float energy,float strength) {
        return 1-Math.max(0,Math.min(1,strength))*(1-(float)Math.exp(-Math.max(0,energy)*7));
    }
    public static float approach(float current,float target,double seconds) {
        return current+(target-current)*(1-(float)Math.exp(-Math.max(0,seconds)/(target<current?.25:.35)));
    }
}

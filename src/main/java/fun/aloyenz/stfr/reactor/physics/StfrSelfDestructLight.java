package fun.aloyenz.stfr.reactor.physics;

/** Visual timing only: server charge and all destructive deadlines remain authoritative. */
public final class StfrSelfDestructLight {
    private StfrSelfDestructLight() {}
    public static final int IGNITION_MS=25;
    public static final class Pulse {
        public final float intensity;
        public final int milliseconds;
        Pulse(float intensity,int milliseconds) { this.intensity=intensity;this.milliseconds=milliseconds; }
    }
    public static boolean ignition(boolean offline,int tick) { return offline && tick>=2455 && tick<2485; }
    public static float intensity(double tick,float charge,boolean core) {
        if(!core || tick<2405 || tick>=3740)return 0;
        if(tick<3320)return .5F*Math.max(0,Math.min(1,charge));
        if(tick<3360)return .7F;
        return (float)(.7+.3*Math.min(1,Math.max(0,(tick-3360)/340)));
    }
    public static Pulse pulse(int tick) {
        switch(tick) {
            case 2940:return new Pulse(.4F,75);
            case 3320:return new Pulse(1.5F,150);
            case 3360:return new Pulse(.7F,100);
            case 3525:return new Pulse(.8F,100);
            case 3730:return new Pulse(2F,500);
            default:return tick>=3320 && tick<3730 && tick%30==15?new Pulse(.2F,50):null;
        }
    }
    /** EnumFacing indices: DOWN, UP, NORTH, SOUTH, WEST, EAST. */
    public static int rotorTick(int direction) {
        switch(direction) {
            case 0:return 3570;case 1:return 3566;case 2:return 3618;
            case 3:return 3689;case 4:return 3515;case 5:return 3542;
            default:throw new IllegalArgumentException("Invalid rotor direction");
        }
    }
}

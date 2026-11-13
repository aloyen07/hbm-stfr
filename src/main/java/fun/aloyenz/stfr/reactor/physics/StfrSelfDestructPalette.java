package fun.aloyenz.stfr.reactor.physics;

/** One temperature palette for the core mesh, star, halo and emitted surface light. */
public final class StfrSelfDestructPalette {
    private StfrSelfDestructPalette() {}
    public static final class Color {
        public final float red,green,blue;
        Color(float red,float green,float blue) { this.red=red;this.green=green;this.blue=blue; }
        public Color brighter() { return new Color(1,green+(1-green)*.15F,blue+(1-blue)*.15F); }
    }
    public static Color color(double tick,float charge) {
        float q=Math.max(0,Math.min(1,charge));
        if(tick>=3320)q=Math.max(q,(float)Math.min(1,(tick-3320)/380));
        return new Color(1,.12F+.88F*Math.min(1,q/.6F),.025F+.975F*Math.max(0,(q-.6F)/.4F));
    }
}

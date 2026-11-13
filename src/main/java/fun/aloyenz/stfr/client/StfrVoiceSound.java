package fun.aloyenz.stfr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Gain and centered/directional mode are immutable for the entire recording. */
final class StfrVoiceSound extends PositionedSound implements ITickableSound {
    private final boolean centered;
    private final World world;
    private boolean done;
    StfrVoiceSound(ResourceLocation sound,BlockPos source,float gain,boolean centered) {
        super(sound,SoundCategory.VOICE);
        this.world=Minecraft.getMinecraft().world;this.centered=centered;this.volume=gain;
        // Keep world panning without the engine's additional distance attenuation.
        this.attenuationType=AttenuationType.NONE;
        xPosF=source.getX()+.5F;yPosF=source.getY()+.5F;zPosF=source.getZ()+.5F;
        update();
    }
    public boolean isDonePlaying() { return done; }
    public void update() {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world!=world) { done=true;return; }
        // A centered voice remains at the listener; this does not change mode or gain.
        if(centered) {
            Entity camera=mc.getRenderViewEntity();
            if(camera!=null) {
                Vec3d eye=camera.getPositionEyes(1);
                xPosF=(float)eye.x;yPosF=(float)eye.y;zPosF=(float)eye.z;
            }
        }
    }
}

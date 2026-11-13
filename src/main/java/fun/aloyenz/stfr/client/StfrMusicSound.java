package fun.aloyenz.stfr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

final class StfrMusicSound extends PositionedSound implements ITickableSound {
	private final World world;
	StfrMusicSound(String sound,float pitch) {
		super(new ResourceLocation(sound),SoundCategory.RECORDS);
		world=Minecraft.getMinecraft().world;this.pitch=Math.max(.01F,pitch);
		followListener();
		attenuationType=AttenuationType.NONE;volume=StfrClientVoice.speaking()?.32F:1F;
	}
	public boolean isDonePlaying() { return Minecraft.getMinecraft().world!=world; }
	private void followListener() {
		Entity listener=Minecraft.getMinecraft().getRenderViewEntity();
		if(listener!=null) {xPosF=(float)listener.posX;yPosF=(float)(listener.posY+listener.getEyeHeight());zPosF=(float)listener.posZ;}
	}
	public void update() {
		followListener();
		float target=StfrClientVoice.speaking()?.32F:1F;
		volume+=(target-volume)*(target<volume?.45F:.08F);
	}
}

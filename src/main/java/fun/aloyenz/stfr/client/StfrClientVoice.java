package fun.aloyenz.stfr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.sound.StfrVoiceCatalog;
import fun.aloyenz.stfr.sound.StfrVoiceRange;

/** The newest reactor announcement cuts off the previous spoken announcement. */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class StfrClientVoice {
	private static ISound current;
	private static BlockPos currentCenter;
	private static World world;

	private StfrClientVoice() {}
	public static boolean speaking() { return current!=null; }

	public static void play(BlockPos center, BlockPos source, String key) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null || !StfrVoiceCatalog.contains(key)) return;
		Entity listener=mc.getRenderViewEntity();
		if(listener==null)return;
		double distance=listener.getPositionEyes(1).distanceTo(new Vec3d(source).add(.5,.5,.5));
		float gain=StfrVoiceRange.gain(distance);
		if(gain<=0)return;
		if (current != null) mc.getSoundHandler().stopSound(current);
		world = mc.world;
		currentCenter = center;
		String language = StfrVoiceCatalog.languageFor(
				key, mc.getLanguageManager().getCurrentLanguage().getLanguageCode());
		String name = key.substring("message.hbmstfr.".length());
		current = new StfrVoiceSound(
				new ResourceLocation(Tags.MODID, "voice." + language + "." + name),
				source,gain,StfrVoiceRange.centered(distance));
		mc.getSoundHandler().playSound(current);
	}

	public static void stop(BlockPos center) {
		if (current == null || !center.equals(currentCenter)) return;
		Minecraft.getMinecraft().getSoundHandler().stopSound(current);
		current = null;
		currentCenter = null;
	}

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (world != mc.world) {
			if (current != null) mc.getSoundHandler().stopSound(current);
			current = null;
			currentCenter = null;
			world = mc.world;
		} else if (current != null && !mc.getSoundHandler().isSoundPlaying(current)) {
			current = null;
			currentCenter = null;
		}
	}
}

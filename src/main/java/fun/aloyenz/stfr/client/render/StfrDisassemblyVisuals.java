package fun.aloyenz.stfr.client.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.reactor.physics.StfrIonTrack;

/** Client-only tracks. They hit terrain, pass through entities and never change the world. */
@Mod.EventBusSubscriber(modid=Tags.MODID,value=Side.CLIENT)
public final class StfrDisassemblyVisuals {
	private static World currentWorld;
	private static final List<StfrIonTrack> TRACKS=new ArrayList<>();
	private static final List<StfrAccidentSmoke> SMOKE=new ArrayList<>();
	private StfrDisassemblyVisuals() {}
	static void tick(World world) {
		if (world!=currentWorld) { TRACKS.clear(); SMOKE.clear(); currentWorld=world; }
		if (world==null) return;
		SMOKE.removeIf(p -> !p.isAlive());
		for (Iterator<StfrIonTrack> iterator=TRACKS.iterator();iterator.hasNext();) {
			StfrIonTrack track=iterator.next();
			track.tick(world.getTotalWorldTime(),(from,to) -> {
				RayTraceResult hit=world.rayTraceBlocks(from,to,false,true,false);
				return hit==null ? null : hit.hitVec;
			});
			if (track.expired()) iterator.remove();
		}
	}
	static void emit(World world,BlockPos center,boolean charging) {
		Minecraft mc=Minecraft.getMinecraft();
		if (mc.player==null || mc.player.getDistanceSq(center)>128D*128D) return;
		int quality=mc.gameSettings.particleSetting;
		emitTracks(world,center);
		if (SMOKE.size()<(quality==2 ? 20 : quality==1 ? 80 : 180)
				&& world.getTotalWorldTime()%(quality==2 ? 6 : 2)==0) {
			double angle=world.rand.nextDouble()*Math.PI*2, radius=world.rand.nextDouble()*(charging ? 8 : 5);
			StfrAccidentSmoke cloud=new StfrAccidentSmoke(world,center.getX()+.5+Math.cos(angle)*radius,
					center.getY()+world.rand.nextDouble()*4, center.getZ()+.5+Math.sin(angle)*radius, charging ? 5 : 3);
			SMOKE.add(cloud); mc.effectRenderer.addEffect(cloud);
		}
	}

	static void emitTracks(World world,BlockPos center) {
		Minecraft mc=Minecraft.getMinecraft();
		if(mc.player==null || mc.player.getDistanceSq(center)>128D*128D)return;
		int quality=mc.gameSettings.particleSetting;
		if (TRACKS.size()<(quality==2 ? 24 : quality==1 ? 80 : 160)
				&& world.getTotalWorldTime()%(quality==2 ? 4 : 1)==0) {
			double y=world.rand.nextDouble()*2-1, angle=world.rand.nextDouble()*Math.PI*2;
			double radial=Math.sqrt(1-y*y);
			TRACKS.add(new StfrIonTrack(new Vec3d(center).add(.5,.5,.5),
					new Vec3d(Math.cos(angle)*radial,y,Math.sin(angle)*radial)));
		}
	}

	@SubscribeEvent public static void render(RenderWorldLastEvent event) {
		Minecraft mc=Minecraft.getMinecraft();
		Entity camera=mc.getRenderViewEntity();
		if (mc.world==null || mc.world!=currentWorld || camera==null || TRACKS.isEmpty()) return;
		float partial=event.getPartialTicks();
		Vec3d view=new Vec3d(camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial,
				camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial,
				camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial);
		double time=mc.world.getTotalWorldTime()+partial;
		float oldX=OpenGlHelper.lastBrightnessX,oldY=OpenGlHelper.lastBrightnessY;
		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(-view.x,-view.y,-view.z);
			GlStateManager.disableTexture2D(); GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.disableAlpha();
			GlStateManager.enableDepth(); GlStateManager.depthMask(false); GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
			StfrGlowMotes.prepare();
			BufferBuilder buffer=Tessellator.getInstance().getBuffer();
			buffer.begin(GL11.GL_TRIANGLES,DefaultVertexFormats.POSITION_COLOR);
			for (StfrIonTrack track:TRACKS) {
				StfrIonTrack.Sample previous=null;
				for (StfrIonTrack.Sample sample:track.samples) {
					if (previous!=null) {
						float a=StfrIonTrack.alpha(time-previous.time)*.7F,b=StfrIonTrack.alpha(time-sample.time)*.7F;
						if (a>0 || b>0) {
							Vec3d side=sample.pos.subtract(previous.pos).crossProduct(view.subtract(sample.pos)).normalize().scale(.014);
							Vec3d p=previous.pos.add(side),q=previous.pos.subtract(side),r=sample.pos.subtract(side),s=sample.pos.add(side);
							vertex(buffer,p,a);vertex(buffer,q,a);vertex(buffer,r,b);
							vertex(buffer,p,a);vertex(buffer,r,b);vertex(buffer,s,b);
						}
					}
					previous=sample;
				}
				if (track.flying) StfrGlowMotes.append(buffer,
						track.previous.add(track.position.subtract(track.previous).scale(partial)),.07,.8F,.9F,1F,.95F);
			}
			Tessellator.getInstance().draw();
		} finally {
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,oldX,oldY);
			GlStateManager.depthMask(true);GlStateManager.enableTexture2D();GlStateManager.enableLighting();GlStateManager.enableAlpha();
			GlStateManager.enableCull();GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
			GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
		}
	}
	private static void vertex(BufferBuilder buffer,Vec3d p,float a) { StfrGlowMotes.vertex(buffer,p,.55F,.75F,1F,a); }
}

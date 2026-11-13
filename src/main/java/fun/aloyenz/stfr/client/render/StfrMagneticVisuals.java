package fun.aloyenz.stfr.client.render;

import java.util.Random;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;

/** Short geometry-bound electrical arcs and pressure fronts, with ordinary depth testing. */
final class StfrMagneticVisuals {
	private StfrMagneticVisuals() {}
	static void render(World world,BlockPos center,double cx,double cy,double cz,RunState state,double tick) {
		double arc=StfrMagneticCollapse.discharge(state,tick);
		if(arc<=0)return;
		GlStateManager.pushMatrix();
		GlStateManager.translate(-cx,-cy,-cz);
		GlStateManager.disableTexture2D();GlStateManager.disableLighting();
		GlStateManager.enableDepth();GlStateManager.depthMask(false);
		GlStateManager.disableCull();GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
		Vec3d start=new Vec3d(center).add(.5,.5,.5),camera=new Vec3d(cx,cy,cz);
		BufferBuilder b=Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
		for(int bolt=0;bolt<StfrMagneticCollapse.boltCount(state,tick);bolt++) {
			long cycle=StfrMagneticCollapse.dischargeStart(state,tick);
			Random random=new Random(center.toLong()^cycle*0x632BE59BD9B4E019L ^ bolt*0x9E3779B97F4A7C15L);
			EnumFacing side=EnumFacing.VALUES[random.nextInt(6)];
			Vec3d direction=new Vec3d(side.getDirectionVec()).add(
					(random.nextDouble()-.5)*1.5,(random.nextDouble()-.5)*1.5,(random.nextDouble()-.5)*1.5).normalize();
			Vec3d end=StfrEmergencyLight.trace(world,start,direction,12);
			Vec3d delta=end.subtract(start),previous=start;
			for(int i=1;i<=12;i++) {
				Vec3d next=i==12?end:start.add(delta.scale(i/12D)).add(
						(random.nextDouble()-.5)*.5,(random.nextDouble()-.5)*.5,(random.nextDouble()-.5)*.5);
				beam(b,previous,next,camera,.09,(float)(arc*.24));
				beam(b,previous,next,camera,.018,(float)(arc*.85));
				if(i%3==0 && i<12)beam(b,next,next.add(new Vec3d(random.nextDouble()-.5,random.nextDouble()-.5,random.nextDouble()-.5).scale(2)),camera,.012,(float)(arc*.55));
				previous=next;
			}
		}
		Tessellator.getInstance().draw();
		GlStateManager.depthMask(true);GlStateManager.disableBlend();GlStateManager.enableCull();
		GlStateManager.enableTexture2D();GlStateManager.enableLighting();GlStateManager.color(1,1,1,1);
		GlStateManager.popMatrix();
	}
	private static void beam(BufferBuilder b,Vec3d a,Vec3d z,Vec3d camera,double width,float alpha) {
		Vec3d side=z.subtract(a).crossProduct(camera.subtract(a));
		if(side.lengthSquared()<1E-10)return;
		side=side.normalize().scale(width);
		vertex(b,a.subtract(side),alpha);vertex(b,a.add(side),alpha);
		vertex(b,z.add(side),alpha);vertex(b,z.subtract(side),alpha);
	}
	private static void vertex(BufferBuilder b,Vec3d p,float alpha) {
		b.pos(p.x,p.y,p.z).color(.62F,.84F,1F,alpha).endVertex();
	}
}

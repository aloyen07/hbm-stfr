package fun.aloyenz.stfr.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Camera-facing radial gradients; callers retain depth testing and do not write depth. */
final class StfrGlowMotes {
	private static Vec3d right = new Vec3d(1,0,0), up = new Vec3d(0,1,0);
	private StfrGlowMotes() {}
	static void prepare() {
		Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
		if (camera == null) return;
		Vec3d look = camera.getLook(1);
		right = look.crossProduct(new Vec3d(0,1,0)).normalize();
		if (right.lengthSquared() < .01) right = new Vec3d(1,0,0);
		up = right.crossProduct(look).normalize();
	}

	static void append(BufferBuilder b, Vec3d center, double radius, float r, float g, float blue, float alpha) {
		for (int i=0;i<12;i++) {
			double a=i*Math.PI/6,next=(i+1)*Math.PI/6;
			double x=Math.cos(a)*radius,y=Math.sin(a)*radius;
			double nx=Math.cos(next)*radius,ny=Math.sin(next)*radius;
			point(b,center,0,0,r,g,blue,alpha);
			point(b,center,x*.22,y*.22,r,g,blue,alpha*.85F);
			point(b,center,nx*.22,ny*.22,r,g,blue,alpha*.85F);
			point(b,center,x*.22,y*.22,r,g,blue,alpha*.85F);
			point(b,center,x,y,r,g,blue,0);
			point(b,center,nx,ny,r,g,blue,0);
			point(b,center,x*.22,y*.22,r,g,blue,alpha*.85F);
			point(b,center,nx,ny,r,g,blue,0);
			point(b,center,nx*.22,ny*.22,r,g,blue,alpha*.85F);
		}
	}
	private static void point(BufferBuilder b, Vec3d c,double x,double y,float r,float g,float blue,float alpha) {
		b.pos(c.x+right.x*x+up.x*y,c.y+right.y*x+up.y*y,c.z+right.z*x+up.z*y)
				.color(r,g,blue,alpha).endVertex();
	}
	static void vertex(BufferBuilder b, Vec3d p, float r, float g, float blue, float alpha) {
		b.pos(p.x,p.y,p.z).color(r,g,blue,alpha).endVertex();
	}
}

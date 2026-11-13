package fun.aloyenz.stfr.client.render;

import java.util.Random;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Continuous violet packets travel outwards; white sweeping rays use the Meltdown renderer. */
final class StfrSelfDestructRays {
    static void render(World world,BlockPos center,double cx,double cy,double cz,double time) {
        Vec3d core=new Vec3d(center).add(.5,.5,.5),camera=new Vec3d(cx,cy,cz);
        boolean oldMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-cx,-cy,-cz);
            GlStateManager.disableTexture2D();GlStateManager.disableLighting();GlStateManager.disableCull();
            GlStateManager.disableAlpha();GlStateManager.enableDepth();GlStateManager.depthMask(false);
            GlStateManager.enableBlend();GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            BufferBuilder b=Tessellator.getInstance().getBuffer();
            b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
            for(int slot=0;slot<12;slot++) {
                double clock=time+slot*2,age=clock-Math.floor(clock/24)*24;
                if(age>=16)continue;
                Random random=new Random(center.toLong()^(long)Math.floor(clock/24)*0x9E3779B97F4A7C15L^slot*0x632BE59BD9B4E019L);
                double y=random.nextDouble()*2-1,angle=random.nextDouble()*Math.PI*2,r=Math.sqrt(1-y*y);
                Vec3d direction=new Vec3d(Math.cos(angle)*r,y,Math.sin(angle)*r);
                double limit=StfrEmergencyLight.trace(world,core,direction,34).distanceTo(core);
                double head=Math.min(1.2+age*2,limit),tail=Math.max(1,1.2+age*2-4);
                if(head<=tail)continue;
                Vec3d from=core.add(direction.scale(tail)),to=core.add(direction.scale(head));
                Vec3d side=direction.crossProduct(camera.subtract(from)).normalize();
                float strength=(float)(Math.min(1,age/2)*Math.min(1,(16-age)/3));
                for(int layer=0;layer<2;layer++) {
                    Vec3d width=side.scale(layer==0?.10:.025);
                    float a=strength*(layer==0?.18F:.8F);
                    vertex(b,from.subtract(width),a*.1F);vertex(b,from.add(width),a*.1F);
                    vertex(b,to.add(width),a);vertex(b,to.subtract(width),a);
                }
            }
            Tessellator.getInstance().draw();
        } finally {
            GlStateManager.depthMask(oldMask);GlStateManager.disableBlend();GlStateManager.enableCull();
            if(alpha)GlStateManager.enableAlpha();GlStateManager.enableTexture2D();GlStateManager.enableLighting();
            GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
        }
    }
    private static void vertex(BufferBuilder b,Vec3d p,float alpha) {
        b.pos(p.x,p.y,p.z).color(.78F,.12F,1F,alpha).endVertex();
    }
}

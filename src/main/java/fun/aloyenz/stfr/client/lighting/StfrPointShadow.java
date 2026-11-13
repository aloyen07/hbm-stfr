package fun.aloyenz.stfr.client.lighting;

import java.nio.ByteBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import fun.aloyenz.stfr.client.render.StfrEmergencyLight;

/** Incrementally ray-traced radial depth cubemap. Glass transmits; collision shapes cast shadows. */
final class StfrPointShadow {
    final int size, radius;
    private final ByteBuffer[] faces=new ByteBuffer[6];
    private int cursor,texture;
    private boolean ready;
    StfrPointShadow(int size,int radius) {
        this.size=size;this.radius=radius;
        for(int i=0;i<6;i++)faces[i]=BufferUtils.createByteBuffer(size*size*3);
    }
    static Vec3d direction(int face,double u,double v) {
        switch(face) {
            case 0:return new Vec3d(1,-v,-u).normalize();
            case 1:return new Vec3d(-1,-v,u).normalize();
            case 2:return new Vec3d(u,1,v).normalize();
            case 3:return new Vec3d(u,-1,-v).normalize();
            case 4:return new Vec3d(u,-v,1).normalize();
            default:return new Vec3d(-u,-v,-1).normalize();
        }
    }
    void update(World world,BlockPos center,int budget,boolean preview) {
        Vec3d start=new Vec3d(center).add(.5,.5,.5);
        for(int i=0;i<budget;i++) {
            int face=cursor/(size*size),pixel=cursor%(size*size);
            Vec3d direction=direction(face,2D*((pixel%size)+.5)/size-1,2D*((pixel/size)+.5)/size-1);
            double distance=StfrEmergencyLight.trace(world,start,direction,radius,preview?center:null).distanceTo(start);
            int packed=(int)Math.round(Math.min(1,distance/radius)*65535);
            faces[face].put(pixel*3,(byte)(packed>>8));faces[face].put(pixel*3+1,(byte)packed);faces[face].put(pixel*3+2,(byte)0);
            cursor++;
            if(cursor==6*size*size) { upload();cursor=0;ready=true;break; }
        }
    }
    private void upload() {
        int old=GL11.glGetInteger(GL13.GL_TEXTURE_BINDING_CUBE_MAP);
        if(texture==0)texture=GL11.glGenTextures();
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,texture);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL12.GL_TEXTURE_WRAP_R,GL12.GL_CLAMP_TO_EDGE);
        for(int face=0;face<6;face++) {
            faces[face].position(0);
            GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X+face,0,GL11.GL_RGB8,size,size,0,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,faces[face]);
        }
        GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,old);
    }
    int texture() { return ready?texture:0; }
    void dispose() { if(texture!=0)GL11.glDeleteTextures(texture);texture=0; }
}

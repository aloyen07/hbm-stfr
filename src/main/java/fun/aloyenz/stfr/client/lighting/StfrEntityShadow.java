package fun.aloyenz.stfr.client.lighting;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

/** Dynamic radial depth from actual animated living-entity models, including hidden casters. */
final class StfrEntityShadow {
    final int size;
    private int texture,program;
    private Framebuffer framebuffer;
    StfrEntityShadow(int size) { this.size=size; }
    void render(Vec3d center,float radius,float partial) throws Exception {
        Minecraft mc=Minecraft.getMinecraft();
        List<EntityLivingBase> casters=new ArrayList<>();
        for(Entity entity:mc.world.loadedEntityList)
            if(entity instanceof EntityLivingBase && !entity.isDead && !entity.isInvisible()
                    && entity.getDistanceSq(center.x,center.y,center.z)<radius*radius)casters.add((EntityLivingBase)entity);
        casters.sort(Comparator.comparingDouble(e -> e.getDistanceSq(center.x,center.y,center.z)));
        if(casters.size()>48)casters=new ArrayList<>(casters.subList(0,48));
        int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),oldMode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),lighting=GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        FloatBuffer clear=clearColorBuffer();
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,clear);
        float brightnessX=OpenGlHelper.lastBrightnessX,brightnessY=OpenGlHelper.lastBrightnessY;
        RenderManager manager=mc.getRenderManager();boolean shadows=manager.isRenderShadow();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();
        try {
            init(mc);
            manager.setRenderShadow(false);
            framebuffer.bindFramebuffer(true);
            GL20.glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"skin"),0);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"radius"),radius);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();
            GL11.glFrustum(-.05,.05,-.05,.05,.05,radius);
            for(int face=0;face<6;face++) {
                OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_COLOR_ATTACHMENT0,
                    GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X+face,texture,0);
                GlStateManager.clearColor(1,1,0,1);GlStateManager.clearDepth(1);
                GlStateManager.depthMask(true);GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();
                Vec3d dir=StfrPointShadow.direction(face,0,0);
                float upY=face==2 || face==3?0:-1,upZ=face==2?1:face==3?-1:0;
                GLU.gluLookAt(0,0,0,(float)dir.x,(float)dir.y,(float)dir.z,0,upY,upZ);
                for(EntityLivingBase entity:casters) {
                    double ex=entity.lastTickPosX+(entity.posX-entity.lastTickPosX)*partial-center.x;
                    double ey=entity.lastTickPosY+(entity.posY-entity.lastTickPosY)*partial-center.y+entity.height*.5;
                    double ez=entity.lastTickPosZ+(entity.posZ-entity.lastTickPosZ)*partial-center.z;
                    double forward=ex*dir.x+ey*dir.y+ez*dir.z,bound=(entity.width+entity.height)*.5;
                    if(forward < -bound || Math.max(Math.abs(ex),Math.max(Math.abs(ey),Math.abs(ez)))>forward+bound*2)continue;
                    GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                    GlStateManager.enableDepth();GlStateManager.depthFunc(GL11.GL_LEQUAL);GlStateManager.depthMask(true);
                    GlStateManager.disableBlend();GlStateManager.enableAlpha();GlStateManager.disableLighting();
                    GL20.glUseProgram(program);
                    double x=entity.lastTickPosX+(entity.posX-entity.lastTickPosX)*partial-center.x;
                    double y=entity.lastTickPosY+(entity.posY-entity.lastTickPosY)*partial-center.y;
                    double z=entity.lastTickPosZ+(entity.posZ-entity.lastTickPosZ)*partial-center.z;
                    manager.renderEntity(entity,x,y,z,entity.prevRotationYaw+(entity.rotationYaw-entity.prevRotationYaw)*partial,partial,false);
                }
            }
        } finally {
            manager.setRenderShadow(shadows);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();GL11.glMatrixMode(oldMode);
            mc.getFramebuffer().bindFramebuffer(true);GL20.glUseProgram(oldProgram);
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
            if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();
            if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
            GlStateManager.depthMask(mask);GlStateManager.depthFunc(depthFunc);
            GlStateManager.clearColor(clear.get(0),clear.get(1),clear.get(2),clear.get(3));GlStateManager.color(1,1,1,1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,brightnessX,brightnessY);
        }
    }
    // LWJGL 2 requires room for 16 values even when the queried state has only four.
    static FloatBuffer clearColorBuffer() { return ByteBuffer.allocateDirect(16*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer(); }
    private void init(Minecraft mc) throws Exception {
        if(program==0) {
            int v=StfrAccidentLighting.compile(mc,"entity_shadow.vert",GL20.GL_VERTEX_SHADER);
            int f=StfrAccidentLighting.compile(mc,"entity_shadow.frag",GL20.GL_FRAGMENT_SHADER);
            program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
            GL20.glDeleteShader(v);GL20.glDeleteShader(f);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
        }
        if(texture==0) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+3);
            int old=GL11.glGetInteger(GL13.GL_TEXTURE_BINDING_CUBE_MAP);
            texture=GL11.glGenTextures();GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,texture);
            for(int face=0;face<6;face++)GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X+face,0,GL11.GL_RGB8,size,size,0,GL11.GL_RGB,GL11.GL_UNSIGNED_BYTE,(ByteBuffer)null);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP,GL12.GL_TEXTURE_WRAP_R,GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,old);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            framebuffer=new Framebuffer(size,size,true);
        }
    }
    int texture() { return texture; }
    void dispose() {
        if(framebuffer!=null)framebuffer.deleteFramebuffer();framebuffer=null;
        if(texture!=0)GL11.glDeleteTextures(texture);texture=0;
        if(program!=0)GL20.glDeleteProgram(program);program=0;
    }
}

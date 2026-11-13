package fun.aloyenz.stfr.client.lighting;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;
import fun.aloyenz.stfr.StfrClientEffectsConfig;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.api.client.StfrCoreLight;
import fun.aloyenz.stfr.api.client.StfrCoreLightRenderEvent;
import fun.aloyenz.stfr.client.lighting.StfrLightExposure;
import fun.aloyenz.stfr.client.render.StfrClientVisuals;
import fun.aloyenz.stfr.client.render.StfrEmergencyLight;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Independent depth-based point lighting; no OptiFine classes or shader-pack assumptions. */
@Mod.EventBusSubscriber(modid=Tags.MODID,value=Side.CLIENT)
public final class StfrAccidentLighting {
    private static final Map<Long,Source> SOURCES=new HashMap<>();
    private static World world;
    private static int program,colorTexture,depthTexture,width,height,frames;
    private static boolean failed,registered;
    private static Framebuffer meter;
    private static float adaptation=1;
    private static long lastFrame;
    private static final FloatBuffer MATRIX=BufferUtils.createFloatBuffer(16);
    private static final class Source {
        StfrCoreLight light;
        long lastSeen;
        float energy;
        StfrPointShadow shadow;
        StfrEntityShadow entities;
        Source(StfrCoreLight light,long now) { this.light=light;lastSeen=now; }
        void dispose() { if(shadow!=null)shadow.dispose();if(entities!=null)entities.dispose();entities=null; }
    }
    private StfrAccidentLighting() {}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void render(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(!registered) {
            registered=true;
            ((IReloadableResourceManager)mc.getResourceManager()).registerReloadListener(manager -> reset());
        }
        if(world!=mc.world) { reset();world=mc.world; }
        if(world==null || mc.getRenderViewEntity()==null)return;
        long now=System.nanoTime();
        double dt=lastFrame==0?.016:Math.min(.1,(now-lastFrame)*1E-9);lastFrame=now;
        Map<Long,StfrCoreLight> incoming=new HashMap<>();
        for(StfrCoreLight light:StfrClientVisuals.coreLights(event.getPartialTicks())) {
            long key=light.center.toLong();
            StfrCoreLight previous=incoming.get(key);
            if(previous==null)incoming.put(key,light);
            else {
                StfrCoreLight base=previous.flash&&!light.flash?light:previous;
                incoming.put(key,new StfrCoreLight(base.dimension,base.center,base.stage,base.time,
                    Math.min(2.5F*StfrClientEffectsConfig.intensity,previous.intensity+light.intensity),
                    Math.max(previous.radius,light.radius),base.preview,previous.flash&&light.flash,base.red,base.green,base.blue));
            }
        }
        for(StfrCoreLight light:incoming.values()) {
            Source s=SOURCES.computeIfAbsent(light.center.toLong(),key -> new Source(light,now));
            s.light=light;s.lastSeen=now;
        }
        List<StfrCoreLight> lights=new ArrayList<>();
        Iterator<Source> iterator=SOURCES.values().iterator();
        while(iterator.hasNext()) {
            Source s=iterator.next();
            if((s.light.flash || s.light.stage==StfrReactor.RunState.SELF_DESTRUCT)
                    && !incoming.containsKey(s.light.center.toLong())) {
                // Keep the shadow resources briefly between rapid pulses, without drawing a fade.
                if(now-s.lastSeen>2_000_000_000L) { s.dispose();iterator.remove(); }
                continue;
            }
            float fade=(float)Math.max(0,1-(now-s.lastSeen)*1E-9/.6);
            if(fade<=0) { s.dispose();iterator.remove();continue; }
            StfrCoreLight l=s.light;
            lights.add(new StfrCoreLight(l.dimension,l.center,l.stage,l.time,l.intensity*fade,l.radius,l.preview,l.flash,l.red,l.green,l.blue));
        }
        StfrCoreLightRenderEvent hook=new StfrCoreLightRenderEvent(lights,event.getPartialTicks());
        MinecraftForge.EVENT_BUS.post(hook);
        boolean lighting=hook.renderLighting && StfrClientEffectsConfig.lighting;
        boolean star=hook.renderStar && StfrClientEffectsConfig.star;
        boolean exposure=hook.renderExposure && StfrClientEffectsConfig.exposure;
        if(lights.isEmpty())adaptation=1;
        if(failed || lights.isEmpty() || (!lighting && !star && !exposure)
                || !OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled())return;
        Entity camera=mc.getRenderViewEntity();
        Vec3d origin=new Vec3d(camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*event.getPartialTicks(),
            camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*event.getPartialTicks(),
            camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*event.getPartialTicks());
        lights.removeIf(l -> new Vec3d(l.center).add(.5,.5,.5).distanceTo(origin)>192);
        lights.sort(Comparator.comparingDouble(l -> new Vec3d(l.center).squareDistanceTo(origin)));
        // Bound GPU and ray-tracing work; API still supplies every source.
        if(lights.size()>4)lights=new ArrayList<>(lights.subList(0,4));
        if(lights.isEmpty()) { adaptation=1;return; }
        int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int oldActive=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] bindings=new int[4];
        int[] cubeBindings=new int[4];
        for(int i=0;i<4;i++) { GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+i);bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);cubeBindings[i]=GL11.glGetInteger(GL13.GL_TEXTURE_BINDING_CUBE_MAP); }
        int[] viewport=new int[4];IntBuffer vp=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_VIEWPORT,vp);vp.get(viewport);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        FloatBuffer clearColor=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,clearColor);
        ByteBuffer colorMask=BufferUtils.createByteBuffer(16);GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,colorMask);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GlStateManager.colorMask(true,true,true,true);
            init(mc);
            mc.getFramebuffer().bindFramebuffer(false);
            GlStateManager.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            Matrix4f projection=matrix(GL11.GL_PROJECTION_MATRIX),view=matrix(GL11.GL_MODELVIEW_MATRIX);
            Matrix4f invProjection=Matrix4f.invert(projection,null),invView=Matrix4f.invert(view,null);
            if(invProjection==null || invView==null)return;
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+1);
            GlStateManager.bindTexture(depthTexture);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D,0,GL14.GL_DEPTH_COMPONENT24,0,0,width,height,0);
            GL20.glUseProgram(program);
            uniform("scene",0);uniform("depth",1);uniform("shadow",2);uniform("entityShadow",3);
            matrixUniform("inverseProjection",invProjection);matrixUniform("inverseView",invView);
            GL20.glUniform2f(location("screenSize"),width,height);
            GlStateManager.disableBlend();GlStateManager.disableDepth();GlStateManager.disableAlpha();GlStateManager.depthMask(false);
            float totalEnergy=0;
            boolean firstLight=true;
            boolean adapt=exposure;
            for(StfrCoreLight light:lights) {
                Source s=SOURCES.get(light.center.toLong());
                boolean pulse=light.flash;
                // A continuous source must keep its shadow map across flash boundaries.
                boolean continuousCold=light.stage==StfrReactor.RunState.REACTION_LOSS
                    || light.stage==StfrReactor.RunState.UNDERLOAD;
                int shadowQuality=pulse && !continuousCold?16:StfrClientEffectsConfig.quality;
                int shadowRadius=Math.max(1,Math.round(light.radius));
                if(s.shadow==null || s.shadow.size!=shadowQuality || s.shadow.radius!=shadowRadius) {
                    s.dispose();s.shadow=new StfrPointShadow(shadowQuality,shadowRadius);
                }
                if(lighting) {
                    int entitySize=Math.max(128,StfrClientEffectsConfig.quality*4);
                    if(s.entities==null)s.entities=new StfrEntityShadow(entitySize);
                    s.entities.render(new Vec3d(light.center).add(.5,.5,.5),light.radius,event.getPartialTicks());
                    GlStateManager.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
                    GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+1);GlStateManager.bindTexture(depthTexture);
                    GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+3);
                    GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,s.entities.texture());
                }
                uniform("entityReady",lighting && s.entities!=null?1:0);
                scalar("shadowSize",s.shadow.size);scalar("entitySize",s.entities!=null?s.entities.size:128);
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+2);
                if(lighting && (pulse || frames%2==0))s.shadow.update(world,light.center,pulse?6*shadowQuality*shadowQuality:512/Math.max(1,lights.size()),light.preview);
                GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,s.shadow.texture());
                Vec3d center=new Vec3d(light.center).add(.5,.5,.5),relative=center.subtract(origin);
                GL20.glUniform3f(location("lightPosition"),(float)relative.x,(float)relative.y,(float)relative.z);
                GL20.glUniform3f(location("lightColor"),light.red,light.green,light.blue);
                boolean self=light.stage==StfrReactor.RunState.SELF_DESTRUCT;
                GL20.glUniform3f(location("starColor"),self?light.red+(1-light.red)*.15F:1,
                    self?light.green+(1-light.green)*.15F:1,self?light.blue+(1-light.blue)*.15F:1);
                scalar("starPower",self?3.5F:8F);
                scalar("radius",light.radius);scalar("intensity",light.intensity);
                uniform("shadowReady",s.shadow.texture()!=0?1:0);
                Vector4f clip=Matrix4f.transform(projection,Matrix4f.transform(view,new Vector4f((float)relative.x,(float)relative.y,(float)relative.z,1),null),null);
                Vec3d eye=origin.add(Matrix4f.transform(invView,new Vector4f(0,0,0,1),null).x,
                    Matrix4f.transform(invView,new Vector4f(0,0,0,1),null).y,Matrix4f.transform(invView,new Vector4f(0,0,0,1),null).z);
                boolean onScreen=StfrLightExposure.onScreen(clip.x,clip.y,clip.z,clip.w);
                float visible=onScreen?visibility(eye,center,light.preview):0;
                GL20.glUniform2f(location("starPosition"),onScreen?clip.x/clip.w*.5F+.5F:-10,onScreen?clip.y/clip.w*.5F+.5F:-10);
                scalar("starVisibility",visible);
                scalar("starSize",(float)Math.min(.15, .65/Math.max(2,center.distanceTo(eye))));
                uniform("drawLight",lighting?1:0);uniform("drawStar",star?1:0);
                scalar("exposure",1);
                boolean cold=light.stage==StfrReactor.RunState.REACTION_LOSS
                    || light.stage==StfrReactor.RunState.UNDERLOAD;
                if(exposure) {
                    meter.bindFramebuffer(true);
                    GlStateManager.clearColor(0,0,0,1);GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                    uniform("measure",1);quad();
                    ByteBuffer pixels=BufferUtils.createByteBuffer(16*9*4);
                    GL11.glReadPixels(0,0,16,9,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
                    double sum=0,peak=0;
                    for(int i=0;i<16*9;i++) { double value=(pixels.get(i*4)&255)/255D;sum+=value;peak=Math.max(peak,value); }
                    s.energy=(float)(sum/(16*9)*.75+peak*.25);
                    mc.getFramebuffer().bindFramebuffer(false);
                    GlStateManager.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
                }
                totalEnergy+=cold && !light.flash ? Math.min(.012F,s.energy) : s.energy;
                uniform("measure",0);
                scalar("exposure",firstLight && adapt?adaptation:1);
                scalar("lightExposure",adapt?adaptation:1);
                firstLight=false;
                // Read the previous pass so several independent reactors add light.
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.bindTexture(colorTexture);
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGB8,0,0,width,height,0);
                quad();
            }
            float target=exposure ? StfrLightExposure.target(totalEnergy,StfrClientEffectsConfig.exposureStrength) : 1;
            adaptation=StfrLightExposure.approach(adaptation,target,dt);
            frames++;
        } catch(Exception error) {
            failed=true;
            LogManager.getLogger("HBM-STFR").error("Core lighting disabled until resource reload; adapter API remains available",error);
        } finally {
            mc.getFramebuffer().bindFramebuffer(false);
            GL20.glUseProgram(oldProgram);
            for(int i=0;i<4;i++) { GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit+i);GlStateManager.bindTexture(bindings[i]);GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP,cubeBindings[i]); }
            GlStateManager.setActiveTexture(oldActive);
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();
            if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
            GlStateManager.depthMask(mask);
            GlStateManager.clearColor(clearColor.get(0),clearColor.get(1),clearColor.get(2),clearColor.get(3));
            GlStateManager.colorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
        }
    }
    private static float visibility(Vec3d eye,Vec3d center,boolean preview) {
        int visible=0;
        for(int i=0;i<5;i++) {
            Vec3d target=center.add(i==1?.4:i==2?-.4:0,i==3?.4:i==4?-.4:0,0);
            Vec3d delta=target.subtract(eye);double distance=delta.length();
            if(distance<.1 || StfrEmergencyLight.trace(world,eye,delta.normalize(),distance,preview?new BlockPos(center):null).distanceTo(eye)>=distance-.1)visible++;
        }
        return visible/5F;
    }
    private static Matrix4f matrix(int name) { MATRIX.clear();GL11.glGetFloat(name,MATRIX);return (Matrix4f)new Matrix4f().load(MATRIX); }
    private static int location(String name) { return GL20.glGetUniformLocation(program,name); }
    private static void uniform(String name,int value) { GL20.glUniform1i(location(name),value); }
    private static void scalar(String name,float value) { GL20.glUniform1f(location(name),value); }
    private static void matrixUniform(String name,Matrix4f value) { MATRIX.clear();value.store(MATRIX);MATRIX.flip();GL20.glUniformMatrix4(location(name),false,MATRIX); }
    private static void quad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0,0);GL11.glVertex2f(-1,-1);
        GL11.glTexCoord2f(1,0);GL11.glVertex2f(1,-1);
        GL11.glTexCoord2f(1,1);GL11.glVertex2f(1,1);
        GL11.glTexCoord2f(0,1);GL11.glVertex2f(-1,1);
        GL11.glEnd();
    }
    private static void init(Minecraft mc) throws Exception {
        if(program==0) {
            int vertex=compile(mc,"core_light.vert",GL20.GL_VERTEX_SHADER),fragment=compile(mc,"core_light.frag",GL20.GL_FRAGMENT_SHADER);
            program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
            GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
        }
        if(colorTexture==0)colorTexture=GL11.glGenTextures();
        if(depthTexture==0)depthTexture=GL11.glGenTextures();
        width=mc.displayWidth;height=mc.displayHeight;
        for(int texture:new int[]{colorTexture,depthTexture}) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);GlStateManager.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        }
        if(meter==null)meter=new Framebuffer(16,9,false);
    }
    static int compile(Minecraft mc,String file,int type) throws Exception {
        String source;
        try(InputStream in=mc.getResourceManager().getResource(new ResourceLocation("hbmstfr","shaders/"+file)).getInputStream()) {
            source=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0) {
            String log=GL20.glGetShaderInfoLog(shader,4096);GL20.glDeleteShader(shader);throw new IllegalStateException(log);
        }
        return shader;
    }
    private static void reset() {
        for(Source source:SOURCES.values())source.dispose();SOURCES.clear();
        if(program!=0)GL20.glDeleteProgram(program);program=0;
        if(colorTexture!=0)GlStateManager.deleteTexture(colorTexture);colorTexture=0;
        if(depthTexture!=0)GlStateManager.deleteTexture(depthTexture);depthTexture=0;
        if(meter!=null)meter.deleteFramebuffer();meter=null;
        failed=false;adaptation=1;lastFrame=0;frames=0;
    }
}

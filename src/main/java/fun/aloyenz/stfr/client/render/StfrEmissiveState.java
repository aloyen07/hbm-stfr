package fun.aloyenz.stfr.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** Untextured core emission must not inherit the HUD/hand lightmap state (F1). */
final class StfrEmissiveState implements AutoCloseable {
    private final int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final boolean baseTexture,lightmap,lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),fog=GL11.glIsEnabled(GL11.GL_FOG);
    StfrEmissiveState() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        lightmap=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        baseTexture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();GlStateManager.disableFog();
    }
    public void close() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        if(lightmap)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if(baseTexture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
        if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
        if(fog)GlStateManager.enableFog();else GlStateManager.disableFog();
        GlStateManager.setActiveTexture(active);
    }
}

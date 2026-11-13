package fun.aloyenz.stfr.client.render;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.StfrClientEffectsConfig;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.api.client.StfrCoreLight;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Render-only preview sources, deliberately outside real reactor snapshots. */
@Mod.EventBusSubscriber(modid=Tags.MODID,value=Side.CLIENT)
public final class StfrFakeCoreVisuals {
    private static World world;
    private static final Set<BlockPos> POSITIONS=new LinkedHashSet<>();
    private static boolean validWorld() {
        World current=Minecraft.getMinecraft().world;
        if(current!=world) { POSITIONS.clear();world=current; }
        return world!=null;
    }
    public static void receive(int dimension,Collection<BlockPos> positions) {
        if(!validWorld() || world.provider.getDimension()!=dimension)return;
        POSITIONS.clear();POSITIONS.addAll(positions);
    }
    public static List<StfrCoreLight> lights(float partial) {
        List<StfrCoreLight> lights=new ArrayList<>();
        if(!validWorld())return lights;
        for(BlockPos pos:POSITIONS)lights.add(new StfrCoreLight(world.provider.getDimension(),pos,
            StfrReactor.RunState.MELTDOWN,world.getTotalWorldTime()+partial,
            StfrClientEffectsConfig.intensity,StfrClientEffectsConfig.radius,true));
        return lights;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !validWorld() || Minecraft.getMinecraft().isGamePaused())return;
        for(BlockPos pos:POSITIONS)if(world.isBlockLoaded(pos))StfrDisassemblyVisuals.emit(world,pos,false);
    }
    @SubscribeEvent public static void render(RenderWorldLastEvent event) {
        try (StfrEmissiveState emission = new StfrEmissiveState()) {
        Minecraft mc=Minecraft.getMinecraft();
        if(!validWorld())return;
        Entity camera=mc.getRenderViewEntity();if(camera==null)return;
        float partial=event.getPartialTicks();
        double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial;
        double y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial;
        double z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial;
        double time=world.getTotalWorldTime()+partial;
        for(BlockPos pos:POSITIONS) {
            if(pos.distanceSq(x,y,z)>192D*192D || !world.isBlockLoaded(pos))continue;
            StfrEmergencyLight.render(world,pos,x,y,z,time,true,true,true);
            RenderReactorCore.render(pos.getX()+.5-x,pos.getY()+.5-y,pos.getZ()+.5-z,
                StfrReactor.RunState.MELTDOWN.ordinal(),time,time,0,Collections.emptyList(),true,false,0);
        }

        }
}
}

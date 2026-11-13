package fun.aloyenz.stfr.debug;

import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import fun.aloyenz.stfr.Tags;

/** One transient registry per server lifetime, shared by all players and dimensions. */
@Mod.EventBusSubscriber(modid=Tags.MODID)
public final class StfrFakeCores {
    private static final Map<MinecraftServer,StfrFakeCoreRegistry> SERVERS=new WeakHashMap<>();
    private static StfrFakeCoreRegistry get(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server,key -> new StfrFakeCoreRegistry());
    }
    public static void add(EntityPlayerMP player,BlockPos pos) {
        get(player.getServer()).add(player.dimension,pos);sync(player.getServer());
    }
    public static int clear(MinecraftServer server) {
        int count=get(server).clear();sync(server);return count;
    }
    private static void send(EntityPlayerMP player) {
        StfrFakeCorePacket.CHANNEL.sendTo(new StfrFakeCorePacket(player.dimension,
            get(player.getServer()).positions(player.dimension)),player);
    }
    private static void sync(MinecraftServer server) {
        for(EntityPlayerMP player:server.getPlayerList().getPlayers())send(player);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        for(MinecraftServer server:new ArrayList<>(SERVERS.keySet()))
            if(server.getTickCounter()%20==0)sync(server);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.player instanceof EntityPlayerMP)send((EntityPlayerMP)event.player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.player instanceof EntityPlayerMP)send((EntityPlayerMP)event.player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.player instanceof EntityPlayerMP)send((EntityPlayerMP)event.player);
    }
}

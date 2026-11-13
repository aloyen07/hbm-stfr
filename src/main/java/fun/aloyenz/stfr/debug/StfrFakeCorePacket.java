package fun.aloyenz.stfr.debug;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.client.render.StfrFakeCoreVisuals;

/** Full dimension snapshot, client-bound only. Never accepts positions from a client. */
public final class StfrFakeCorePacket implements IMessage {
    public static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID+"_fake_core");
    private int dimension;
    private final Set<BlockPos> positions=new LinkedHashSet<>();
    public StfrFakeCorePacket() {}
    public StfrFakeCorePacket(int dimension,Collection<BlockPos> positions) {
        this.dimension=dimension;this.positions.addAll(positions);
    }
    public static void register() {
        if(FMLCommonHandler.instance().getSide().isClient())
            CHANNEL.registerMessage(Client.class,StfrFakeCorePacket.class,0,Side.CLIENT);
        else CHANNEL.registerMessage(Stub.class,StfrFakeCorePacket.class,0,Side.CLIENT);
    }
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);buf.writeInt(positions.size());
        for(BlockPos pos:positions)buf.writeLong(pos.toLong());
    }
    public void fromBytes(ByteBuf buf) {
        dimension=buf.readInt();int count=buf.readInt();positions.clear();
        if(count<0 || count>buf.readableBytes()/8)throw new IllegalArgumentException("Invalid fake core snapshot size");
        for(int i=0;i<count;i++)positions.add(BlockPos.fromLong(buf.readLong()));
    }
    public static final class Stub implements IMessageHandler<StfrFakeCorePacket,IMessage> {
        public IMessage onMessage(StfrFakeCorePacket packet,MessageContext context) { return null; }
    }
    @SideOnly(Side.CLIENT)
    public static final class Client implements IMessageHandler<StfrFakeCorePacket,IMessage> {
        public IMessage onMessage(StfrFakeCorePacket packet,MessageContext context) {
            Minecraft mc=Minecraft.getMinecraft();
            mc.addScheduledTask(() -> StfrFakeCoreVisuals.receive(packet.dimension,packet.positions));
            return null;
        }
    }
}

package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import fun.aloyenz.stfr.client.render.StfrCoreFlashes;
import fun.aloyenz.stfr.reactor.StfrReactor;

/** Client-bound visual event only. Damage is independently applied by the server. */
public final class StfrCoreFlashPacket implements IMessage {
    private int dimension;
    private BlockPos center;
    private float intensity;
    private int milliseconds,radius,stage;
    public StfrCoreFlashPacket() {}
    public StfrCoreFlashPacket(int dimension,BlockPos center) {
        this(dimension,center,1.6F,100,0,StfrReactor.RunState.OVERLOAD);
    }
    public StfrCoreFlashPacket(int dimension,BlockPos center,float intensity,int milliseconds,int radius,StfrReactor.RunState stage) {
        this.dimension=dimension;this.center=center;this.intensity=intensity;this.milliseconds=milliseconds;this.radius=radius;this.stage=stage.ordinal();
    }
    public static void selfDestruct(World world,BlockPos center,float intensity,int milliseconds,int radius) {
        if(world.isRemote)return;
        StfrCoreFlashPacket packet=new StfrCoreFlashPacket(world.provider.getDimension(),center,intensity,milliseconds,radius,
            StfrReactor.RunState.SELF_DESTRUCT);
        for(EntityPlayer player:world.playerEntities)
            if(player instanceof EntityPlayerMP && player.getDistanceSq(center)<=192D*192D)
                StfrLogicPacket.CHANNEL.sendTo(packet,(EntityPlayerMP)player);
    }
    public void toBytes(ByteBuf buffer) { buffer.writeInt(dimension);buffer.writeLong(center.toLong());buffer.writeFloat(intensity);buffer.writeInt(milliseconds);buffer.writeInt(radius);buffer.writeByte(stage); }
    public void fromBytes(ByteBuf buffer) { dimension=buffer.readInt();center=BlockPos.fromLong(buffer.readLong());intensity=buffer.readFloat();milliseconds=buffer.readInt();radius=buffer.readInt();stage=buffer.readUnsignedByte(); }
    public static final class Client implements IMessageHandler<StfrCoreFlashPacket,IMessage> {
        public IMessage onMessage(StfrCoreFlashPacket packet,MessageContext context) {
            Minecraft mc=Minecraft.getMinecraft();
            mc.addScheduledTask(() -> StfrCoreFlashes.receive(packet.dimension,packet.center,packet.intensity,packet.milliseconds,packet.radius,packet.stage));
            return null;
        }
    }
}

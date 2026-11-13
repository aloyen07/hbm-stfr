package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.logic.StfrCoreFlashPacket;
import fun.aloyenz.stfr.logic.StfrRotorSyncPacket;

class StfrSelfDestructLightPacketTest {
    @Test void halfTickFlashAndLocalRotorRadiusSurviveNetworkEncoding() {
        ByteBuf a=Unpooled.buffer(),b=Unpooled.buffer();
        try {
            new StfrCoreFlashPacket(-1,new BlockPos(4,50,8),.25F,25,16,StfrReactor.RunState.SELF_DESTRUCT).toBytes(a);
            StfrCoreFlashPacket restored=new StfrCoreFlashPacket();restored.fromBytes(a.duplicate());restored.toBytes(b);
            assertEquals(a,b);
        } finally { a.release();b.release(); }
    }
    @Test void actualChargeFractionSurvivesVisualSnapshotEncoding() {
        ByteBuf a=Unpooled.buffer(),b=Unpooled.buffer(),input=Unpooled.buffer();
        try {
            new StfrRotorSyncPacket(BlockPos.ORIGIN,12,200,StfrReactor.RunState.SELF_DESTRUCT.ordinal(),3000,
                0,false,true,1,false,0,0,.375F).toBytes(a);
            input.writeBytes(a,a.readerIndex(),a.readableBytes());
            StfrRotorSyncPacket restored=new StfrRotorSyncPacket();restored.fromBytes(input);restored.toBytes(b);
            assertEquals(a,b);
            assertEquals(.375F,b.getFloat(b.writerIndex()-4));
        } finally { a.release();b.release();input.release(); }
    }
}

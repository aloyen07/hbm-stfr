package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.entity.EntityStfrDebris;

class StfrSealDebrisSpawnTest {
    @Test void ballisticShellKeepsMaterialSpinUpwardVelocityAndTilePayloadAcrossSpawn() {
        Bootstrap.register();
        EntityStfrDebris server=new EntityStfrDebris(null,new BlockPos(4,70,3),Blocks.PLANKS.getStateFromMeta(3),BlockPos.ORIGIN,70,600,200);
        server.tileEntityData=new NBTTagCompound();server.tileEntityData.setString("CustomName","Shell test");
        server.launch(new Vec3d(.8,.9,-.2));
        assertTrue(server.released());assertEquals(.9,server.motionY);
        ByteBuf data=Unpooled.buffer();
        try {
            server.writeSpawnData(data);
            EntityStfrDebris client=new EntityStfrDebris(null);
            // Forge supplies velocity in its standard spawn packet, before additional NBT.
            client.motionX=server.motionX;client.motionY=server.motionY;client.motionZ=server.motionZ;
            client.readSpawnData(data);
            assertTrue(client.released());assertTrue(client.canBeCollidedWith());
            assertEquals(server.getBlock(),client.getBlock());
            assertEquals(server.motionX,client.motionX);
            assertEquals(server.motionY,client.motionY);assertEquals(server.motionZ,client.motionZ);
            assertEquals(server.spin(.5F),client.spin(.5F));
            assertEquals("Shell test",client.tileEntityData.getString("CustomName"));
            assertFalse(client.isDead);
        } finally { data.release(); }
    }
}

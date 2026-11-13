package fun.aloyenz.stfr.meme;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.imageio.ImageIO;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

class StfrCatResourcesTest {
    @Test void finalApprovedAssetsAndSoundArePackaged() throws Exception {
        Path root=Paths.get("src/main/resources/assets/hbmstfr");
        for(int i=1;i<=3;i++) {
            var image=ImageIO.read(root.resolve("textures/meme/cat_"+i+".png").toFile());
            assertEquals(128,image.getWidth());assertEquals(96,image.getHeight());
        }
        byte[] sound=Files.readAllBytes(root.resolve("sounds/meme/cat_buffer_stall.ogg"));
        assertEquals("OggS",new String(sound,0,4,StandardCharsets.US_ASCII));
        JsonObject sounds=new JsonParser().parse(Files.readString(root.resolve("sounds.json"))).getAsJsonObject();
        assertTrue(sounds.has("meme.cat_buffer_stall"));
        for(String lang:new String[]{"ru_ru","en_us"}) {
            String text=Files.readString(root.resolve("lang/"+lang+".lang"));
            assertTrue(text.contains("gui.hbmstfr.cat.warning="));
            assertTrue(text.contains("gui.hbmstfr.cat.threat="));
        }
    }
    @Test void sharedMonitorGroupSurvivesSaveAndReload() {
        NBTTagCompound input=new NBTTagCompound(), group=new NBTTagCompound();
        group.setLong("center",123);group.setInteger("stage",2);group.setLong("lastClick",50);
        NBTTagList targets=new NBTTagList();
        for(long pos:new long[]{100,200,300}) { NBTTagCompound tag=new NBTTagCompound();tag.setLong("pos",pos);targets.appendTag(tag); }
        group.setTag("monitors",targets);NBTTagList groups=new NBTTagList();groups.appendTag(group);input.setTag("groups",groups);
        StfrCatEvents data=new StfrCatEvents();data.readFromNBT(input);
        NBTTagList result=data.writeToNBT(new NBTTagCompound()).getTagList("groups",10);
        assertEquals(1,result.tagCount());
        NBTTagCompound saved=result.getCompoundTagAt(0);
        assertEquals(2,saved.getInteger("stage"));assertEquals(123,saved.getLong("center"));
        assertEquals(3,saved.getTagList("monitors",10).tagCount());
        assertEquals(50,saved.getLong("lastClick"));
    }
}

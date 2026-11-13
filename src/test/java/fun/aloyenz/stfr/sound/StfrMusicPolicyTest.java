package fun.aloyenz.stfr.sound;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import net.minecraft.util.SoundEvent;

class StfrMusicPolicyTest {
    @Test void braamCanPlayOverMusicAndDoesNotBlockMusic() {
        String braam="hbmstfr:effects.braam",music="hbmstfr:music.hacknet_ost_panic_track";
        assertTrue(StfrMusicPolicy.accent(braam));
        assertFalse(StfrMusicPolicy.blocks(braam,music));
        assertFalse(StfrMusicPolicy.blocks(music,braam));
        assertTrue(StfrMusicPolicy.blocks(music,music));
        assertTrue(StfrMusicPolicy.accent("123:"+braam));
    }

    @Test void sceneRegistryMatchesTheResourceCatalog() throws Exception {
        try(InputStream stream=getClass().getResourceAsStream("/assets/hbmstfr/sounds.json")) {
            assertNotNull(stream);
            JsonObject sounds=new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            for(SoundEvent event:StfrSceneSounds.values()) {
                String path=event.getRegistryName().getPath();
                assertTrue(sounds.has(path),path);
                assertTrue(path.startsWith("music.") || path.startsWith("alarms.")
                    || path.startsWith("effects.") || path.startsWith("scene_voice."),path);
            }
        }
    }

    @Test void machineryDoesNotCompeteWithMusicForStreamingChannels() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/assets/hbmstfr/sounds.json")) {
            assertNotNull(stream);
            JsonObject sounds = new JsonParser().parse(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            // system_noise lasts ~20 seconds and starts every 8 seconds. Three copies
            // plus startup music and speech would exceed Paulscode's four stream channels.
            for (String name : new String[] {"reactor.system_noise", "reactor.humming", "reactor.humming_high"}) {
                for (JsonElement entry : sounds.getAsJsonObject(name).getAsJsonArray("sounds")) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject sound = entry.getAsJsonObject();
                    assertFalse(sound.has("stream") && sound.get("stream").getAsBoolean(), name);
                }
            }
            assertTrue(sounds.getAsJsonObject("reactor.startup_music").getAsJsonArray("sounds")
                    .get(0).getAsJsonObject().get("stream").getAsBoolean(), "Long music remains streamed");
        }
    }
}

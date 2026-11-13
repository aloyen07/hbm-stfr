package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.jupiter.api.Test;

final class StfrEmitterMigrationTest {
	@Test void onlyLegacyVariantsAreReplacedAndReplacementIsIdempotent() throws Exception {
		Bootstrap.register();
		// Register minimal lens fixtures; loading all HBM blocks requires LaunchWrapper
		// to strip optional mod interfaces and is exercised by the server smoke check.
		Block normal = new Block(Material.IRON);
		Block auxiliary = new Block(Material.IRON);
		Block frozen = new Block(Material.IRON);
		ForgeRegistries.BLOCKS.register(normal.setRegistryName(new ResourceLocation("hbmstfr:stfr_plasma_lens")));
		ForgeRegistries.BLOCKS.register(auxiliary.setRegistryName(new ResourceLocation("hbmstfr:stfr_auxiliary_quartz_lens")));
		ForgeRegistries.BLOCKS.register(frozen.setRegistryName(new ResourceLocation("hbmstfr:stfr_frozen_plasma_lens")));
		assertSame(normal.getDefaultState(),
				StfrEmitterMigration.replacement(Blocks.GLOWSTONE.getDefaultState()));
		assertSame(auxiliary.getDefaultState(),
				StfrEmitterMigration.replacement(Blocks.QUARTZ_BLOCK.getDefaultState()));
		assertSame(frozen.getDefaultState(),
				StfrEmitterMigration.replacement(Blocks.PACKED_ICE.getDefaultState()));
		for (Block block : new Block[] {
				Blocks.AIR, Blocks.STONE, normal,
				auxiliary, frozen})
			assertSame(block.getDefaultState(), StfrEmitterMigration.replacement(block.getDefaultState()));
	}

	@Test void shippedTemplateUsesSixNewNormalLenses() throws Exception {
		try (InputStream stream = getClass().getResourceAsStream("/assets/hbmstfr/structures/stfr.json")) {
			assertNotNull(stream);
			JsonObject root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			int count = 0;
			for (JsonElement element : root.getAsJsonArray("cells")) {
				JsonObject cell = element.getAsJsonObject();
				if (!"EMITTER_BLOCK".equals(cell.get("role").getAsString())) continue;
				assertEquals("hbmstfr:stfr_plasma_lens", cell.get("block").getAsString());
				count++;
			}
			assertEquals(6, count);
		}
	}
}

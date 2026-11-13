package fun.aloyenz.stfr.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.tile.TileEntityDataCable;

final class StfrDataCableBlockTest {

	@Test
	void coveredCablesCarryTheSameNetworkAndHaveCompleteAssets() throws Exception {
		Bootstrap.register();
		for (String name : new String[] {
			"covered_logic_cable", "radiation_shielded_logic_cable"
		}) {
			BlockCoveredDataCable block = new BlockCoveredDataCable(name,
					name.startsWith("radiation"));
			assertTrue(BlockDataCable.isDataCable(block));
			assertTrue(block.isFullCube(block.getDefaultState()));
			assertTrue(block.isOpaqueCube(block.getDefaultState()));
			assertTrue(block.createNewTileEntity(null, 0) instanceof TileEntityDataCable);
			for (String path : new String[] {
				"blockstates/" + name + ".json",
				"models/block/" + name + ".json",
				"models/item/" + name + ".json"
			}) assertNotNull(getClass().getResource("/assets/hbmstfr/" + path), path);
			try (InputStream texture = getClass().getResourceAsStream(
					"/assets/hbmstfr/textures/blocks/" + name + ".png")) {
				assertNotNull(texture, name);
				assertTrue(ImageIO.read(texture).getWidth() == 16, name);
			}
		}
		assertFalse(BlockDataCable.isDataCable(Blocks.STONE));
	}
}

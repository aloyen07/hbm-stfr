package fun.aloyenz.stfr.tile;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Export of editable structure roles and rotor annotations. */
final class StfrAnalyzerExport {
	private StfrAnalyzerExport() {}

	static File saveRoles(TileEntityStructureAnalyzer c) {
		File directory = new File(c.getWorld().getSaveHandler().getWorldDirectory(), "hbmstfr");
		if (!directory.isDirectory() && !directory.mkdirs())
			throw new IllegalStateException("Cannot create " + directory);
		File output =
				new File(
						directory,
						"stfr_roles_"
								+ c.getWorld().provider.getDimension()
								+ "_"
								+ c.getPos().getX()
								+ "_"
								+ c.getPos().getY()
								+ "_"
								+ c.getPos().getZ()
								+ ".json");
		JsonObject root = new JsonObject();
		root.addProperty("formatVersion", 1);
		root.addProperty("heightOffset", c.heightOffset);
		if (c.liftBoxFirst != null) root.add("liftBoxFirst", jsonPos(c.liftBoxFirst));
		if (c.liftBoxSecond != null) root.add("liftBoxSecond", jsonPos(c.liftBoxSecond));
		if (c.liftReleaseY != Integer.MIN_VALUE) root.addProperty("liftReleaseY", c.liftReleaseY);
		JsonArray cells = new JsonArray();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			JsonObject json = new JsonObject();
			JsonArray xyz = new JsonArray();
			xyz.add(cell.localPos.getX());
			xyz.add(cell.localPos.getY());
			xyz.add(cell.localPos.getZ());
			json.add("pos", xyz);
			json.addProperty("block", cell.expected.getBlock().getRegistryName().toString());
			json.addProperty("meta", cell.expected.getBlock().getMetaFromState(cell.expected));
			json.addProperty("role", c.getRole(cell.localPos).name());
			json.addProperty("miscRole", c.getMiscRole(cell.localPos).name());
			IBlockState injector = c.injectorState(cell.localPos);
			if (injector != null) {
				JsonObject reference = new JsonObject();
				reference.addProperty("block", injector.getBlock().getRegistryName().toString());
				reference.addProperty("meta", injector.getBlock().getMetaFromState(injector));
				json.add("injectorState", reference);
			}
			json.addProperty("caseBreach", c.isCaseBreach(cell.localPos));
			json.addProperty("destructible", c.isDestructible(cell.localPos));
			cells.add(json);
		}
		for (Map.Entry<BlockPos, StfrBlockRole> entry : c.airRoles().entrySet()) {
			BlockPos local = entry.getKey();
			JsonObject json = new JsonObject();
			JsonArray xyz = new JsonArray();
			xyz.add(local.getX());
			xyz.add(local.getY());
			xyz.add(local.getZ());
			json.add("pos", xyz);
			json.addProperty("block", "minecraft:air");
			json.addProperty("meta", 0);
			json.addProperty("role", entry.getValue().name());
			cells.add(json);
		}
		root.add("cells", cells);
		JsonArray rotorEntries = new JsonArray();
		for (Long key : c.rotors) {
			BlockPos local = BlockPos.fromLong(key);
			JsonObject json = new JsonObject();
			JsonArray xyz = new JsonArray();
			xyz.add(local.getX());
			xyz.add(local.getY());
			xyz.add(local.getZ());
			json.add("pos", xyz);
			json.addProperty("role", "ROTOR");
			rotorEntries.add(json);
		}
		for (Map.Entry<Long, EnumFacing> entry : c.axes.entrySet()) {
			BlockPos local = BlockPos.fromLong(entry.getKey());
			JsonObject json = new JsonObject();
			JsonArray xyz = new JsonArray();
			xyz.add(local.getX());
			xyz.add(local.getY());
			xyz.add(local.getZ());
			json.add("pos", xyz);
			json.addProperty("role", "AXIS");
			json.addProperty("face", entry.getValue().getName());
			rotorEntries.add(json);
		}
		for (Map.Entry<Long, Integer> entry : c.particleFaces.entrySet())
			for (EnumFacing face : EnumFacing.VALUES) {
				if ((entry.getValue() & 1 << face.getIndex()) == 0) continue;
				BlockPos local = BlockPos.fromLong(entry.getKey());
				JsonObject json = new JsonObject();
				JsonArray xyz = new JsonArray();
				xyz.add(local.getX());
				xyz.add(local.getY());
				xyz.add(local.getZ());
				json.add("pos", xyz);
				json.addProperty("role", "PARTICLE");
				json.addProperty("face", face.getName());
				rotorEntries.add(json);
			}
		root.add("rotorAnnotations", rotorEntries);
		try (FileWriter writer = new FileWriter(output)) {
			new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot save structure roles", exception);
		}
		return output;
		}
	static JsonArray jsonPos(BlockPos local) {
		JsonArray result = new JsonArray();
		result.add(local.getX());
		result.add(local.getY());
		result.add(local.getZ());
		return result;
	}
}

package fun.aloyenz.stfr.structure;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.block.STFRBlocks;

/** Immutable reference generated from a vanilla structure-block export. */
public final class StfrStructureTemplate {

	public static final int SIZE = 29;
	public static final BlockPos CENTER = new BlockPos(14, 14, 14);
	private static volatile List<Cell> cells;
	private static Map<BlockPos, Cell> byPosition;
	private static Map<BlockPos, StfrBlockRole> defaultAirRoles;

	private StfrStructureTemplate() {}

	public static List<Cell> cells() {
		List<Cell> result = cells;
		if (result != null) return result;
		synchronized (StfrStructureTemplate.class) {
			if (cells == null) {
				List<Cell> loaded = load();
				Map<BlockPos, Cell> index = new HashMap<>();
				for (Cell cell : loaded) index.put(cell.localPos, cell);
				byPosition = Collections.unmodifiableMap(index);
				cells = Collections.unmodifiableList(loaded);
			}
			return cells;
		}
	}

	public static Cell cellAt(BlockPos local) {
		cells();
		return byPosition.get(local);
	}

	public static Map<BlockPos, StfrBlockRole> airRoles() {
		cells();
		return defaultAirRoles;
	}

	private static List<Cell> load() {
		try (InputStream stream =
				StfrStructureTemplate.class.getResourceAsStream(
						"/assets/hbmstfr/structures/stfr.json")) {
			if (stream == null)
				throw new IllegalStateException("Missing generated STFR structure JSON");
			JsonObject root =
					new JsonParser()
							.parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
							.getAsJsonObject();
			// The analyzer's role export has the same cells but omits immutable geometry
			// and writes block metadata. Accept it as a compatible authoring format.
			if (!root.has("formatVersion")
					|| root.get("formatVersion").getAsInt() != 1
					|| root.has("size")
							&& !matchesPosition(root.getAsJsonArray("size"), SIZE, SIZE, SIZE)
					|| root.has("center")
							&& !matchesPosition(
									root.getAsJsonArray("center"),
									CENTER.getX(),
									CENTER.getY(),
									CENTER.getZ())) {
				throw new IllegalStateException(
						"Unsupported STFR structure size, center, or JSON version");
			}
			List<Cell> result = new ArrayList<>();
			Map<BlockPos, StfrBlockRole> air = new HashMap<>();
			Set<BlockPos> seen = new HashSet<>();
			for (JsonElement element : root.getAsJsonArray("cells")) {
				JsonObject entry = element.getAsJsonObject();
				JsonArray xyz = entry.getAsJsonArray("pos");
				if (xyz.size() != 3) throw new IllegalStateException("Invalid STFR cell position");
				BlockPos local =
						new BlockPos(
								xyz.get(0).getAsInt(),
								xyz.get(1).getAsInt(),
								xyz.get(2).getAsInt());
				if (local.getX() < 0
						|| local.getX() >= SIZE
						|| local.getY() < 0
						|| local.getY() >= SIZE
						|| local.getZ() < 0
						|| local.getZ() >= SIZE
						|| !seen.add(local)) {
					throw new IllegalStateException("Invalid or duplicate STFR cell " + local);
				}
				// Authoring exports may contain unrelated pipes/cables without a primary role.
				if (!entry.has("role")
						|| entry.get("role").getAsString().equals("UNDEFINED")
						|| entry.get("role").getAsString().equals("UNASSIGNED")) continue;
				IBlockState state = decodeState(entry);
				StfrBlockRole role =
						entry.has("role")
								? StfrBlockRole.valueOf(entry.get("role").getAsString())
								: StfrBlockRole.UNASSIGNED;
				if (state.getBlock() == Blocks.AIR) {
					if (!role.requiresAir())
						throw new IllegalStateException("Invalid STFR air role at " + local);
					air.put(local, role);
					continue;
				}
				result.add(
						new Cell(
								local,
								state,
								role,
								entry.has("caseBreach") && entry.get("caseBreach").getAsBoolean(),
								entry.has("destructible")
										&& entry.get("destructible").getAsBoolean(),
								entry.has("miscRole")
										? StfrMiscRole.valueOf(entry.get("miscRole").getAsString())
										: StfrMiscRole.UNDEFINED,
								entry.has("injectorState")
										? decodeState(entry.getAsJsonObject("injectorState"))
										: null));
			}
			if (result.isEmpty())
				throw new IllegalStateException("STFR structure JSON has no cells");
			defaultAirRoles = Collections.unmodifiableMap(air);
			return result;
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot read generated STFR structure JSON", exception);
		}
	}

	private static boolean matchesPosition(JsonArray array, int x, int y, int z) {
		return array != null
				&& array.size() == 3
				&& array.get(0).getAsInt() == x
				&& array.get(1).getAsInt() == y
				&& array.get(2).getAsInt() == z;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static IBlockState decodeState(JsonObject encoded) {
		ResourceLocation name = new ResourceLocation(encoded.get("block").getAsString());
		if (name.equals(new ResourceLocation("minecraft", "air")))
			return Blocks.AIR.getDefaultState();
		Block block = Block.REGISTRY.getObject(name);
		if (block == null || block == Blocks.AIR)
			throw new IllegalStateException("Unknown STFR block " + name);
		IBlockState state =
				encoded.has("meta")
						? block.getStateFromMeta(encoded.get("meta").getAsInt())
						: block.getDefaultState();
		JsonObject properties =
				encoded.has("properties")
						? encoded.getAsJsonObject("properties")
						: new JsonObject();
		for (Map.Entry<String, JsonElement> propertyEntry : properties.entrySet()) {
			IProperty property = block.getBlockState().getProperty(propertyEntry.getKey());
			if (property == null)
				throw new IllegalStateException(
						"Unknown property " + propertyEntry.getKey() + " on " + name);
			Optional value = property.parseValue(propertyEntry.getValue().getAsString());
			if (!value.isPresent())
				throw new IllegalStateException(
						"Invalid value for " + propertyEntry.getKey() + " on " + name);
			state = state.withProperty(property, (Comparable) value.get());
		}
		return state;
	}

	public static final class Cell {

		public final BlockPos localPos;
		public final IBlockState expected;
		public final StfrBlockRole defaultRole;
		public final boolean caseBreach;
		public final boolean destructible;
		public final StfrMiscRole miscRole;
		public final IBlockState injectorState;

		private Cell(
				BlockPos localPos,
				IBlockState expected,
				StfrBlockRole defaultRole,
				boolean caseBreach,
				boolean destructible,
				StfrMiscRole miscRole,
				IBlockState injectorState) {
			this.localPos = localPos;
			this.expected = expected;
			this.defaultRole = defaultRole;
			this.caseBreach = caseBreach;
			this.destructible = destructible;
			this.miscRole = miscRole;
			if (injectorState != null && injectorState.getBlock() != STFRBlocks.SOLID_INJECTOR)
				throw new IllegalStateException("Invalid injector reference at " + localPos);
			if (miscRole == StfrMiscRole.INJECTOR_PLACEABLE && injectorState == null)
				throw new IllegalStateException("Missing injector reference at " + localPos);
			this.injectorState = injectorState;
		}
	}
}

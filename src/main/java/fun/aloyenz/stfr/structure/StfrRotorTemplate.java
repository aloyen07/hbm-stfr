package fun.aloyenz.stfr.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** The validated rotor authoring export packaged with the reactor structure. */
public final class StfrRotorTemplate {

	private static final Set<Long> ROTORS = new HashSet<>();
	private static final Map<Long, EnumFacing> AXES = new HashMap<>();
	private static final Map<Long, Integer> PARTICLES = new HashMap<>();

	static {
		try (InputStream stream =
				StfrRotorTemplate.class.getResourceAsStream(
						"/assets/hbmstfr/structures/stfr_rotors.json")) {
			if (stream == null) throw new IllegalStateException("Missing STFR rotor map");
			JsonObject root =
					new JsonParser()
							.parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
							.getAsJsonObject();
			if (root.get("formatVersion").getAsInt() != 1)
				throw new IllegalStateException("Unknown rotor map version");
			for (JsonElement element : root.getAsJsonArray("rotorAnnotations")) {
				JsonObject marker = element.getAsJsonObject();
				JsonArray xyz = marker.getAsJsonArray("pos");
				if (xyz == null || xyz.size() != 3)
					throw new IllegalStateException("Invalid rotor coordinate");
				BlockPos local =
						new BlockPos(
								xyz.get(0).getAsInt(),
								xyz.get(1).getAsInt(),
								xyz.get(2).getAsInt());
				if (StfrStructureTemplate.cellAt(local) == null)
					throw new IllegalStateException(
							"Rotor marker outside STFR structure: " + local);
				long key = local.toLong();
				switch (marker.get("role").getAsString()) {
					case "ROTOR":
						ROTORS.add(key);
						break;
					case "AXIS":
						AXES.put(key, face(marker));
						break;
					case "PARTICLE":
						EnumFacing side = face(marker);
						PARTICLES.put(key, PARTICLES.getOrDefault(key, 0) | 1 << side.getIndex());
						break;
					default:
						throw new IllegalStateException("Unknown STFR rotor marker: " + marker);
				}
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Cannot load STFR rotor map", exception);
		}
	}

	private StfrRotorTemplate() {}

	private static EnumFacing face(JsonObject marker) {
		EnumFacing facing = EnumFacing.byName(marker.get("face").getAsString());
		if (facing == null) throw new IllegalStateException("Invalid STFR rotor face: " + marker);
		return facing;
	}

	public static void populate(
			Set<Long> rotors, Map<Long, EnumFacing> axes, Map<Long, Integer> particles) {
		rotors.addAll(ROTORS);
		axes.putAll(AXES);
		particles.putAll(PARTICLES);
	}
}

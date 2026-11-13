package fun.aloyenz.stfr.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;

/** Recorded announcement IDs and the Minecraft locales supported by each language. */
public final class StfrVoiceCatalog {
	private static final Map<String, Set<String>> RECORDINGS = loadRecordings();
	private static final LanguageSettings LANGUAGES = loadLanguages();

	private StfrVoiceCatalog() {}

	public static boolean contains(String key) {
		return RECORDINGS.containsKey(key);
	}

	public static String languageFor(String key, String minecraftLocale) {
		Set<String> available = RECORDINGS.get(key);
		if (available == null || available.isEmpty()) return LANGUAGES.defaultLanguage;
		String locale = minecraftLocale.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Set<String>> entry : LANGUAGES.locales.entrySet())
			if (available.contains(entry.getKey()) && entry.getValue().contains(locale))
				return entry.getKey();
		if (available.contains(LANGUAGES.defaultLanguage)) return LANGUAGES.defaultLanguage;
		return available.iterator().next();
	}

	private static Map<String, Set<String>> loadRecordings() {
		try (InputStream stream = StfrVoiceCatalog.class.getResourceAsStream(
				"/assets/hbmstfr/voice_lines.json")) {
			if (stream == null) return Collections.emptyMap();
			JsonObject data = new JsonParser()
					.parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, Set<String>> recordings = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
				Set<String> languages = new LinkedHashSet<>();
				for (Map.Entry<String, JsonElement> language : entry.getValue().getAsJsonObject().entrySet())
					languages.add(language.getKey());
				if (!languages.isEmpty()) recordings.put(entry.getKey(), Collections.unmodifiableSet(languages));
			}
			return Collections.unmodifiableMap(recordings);
		} catch (Exception exception) {
			LogManager.getLogger("STFR").error("Cannot load STFR voice catalog", exception);
			return Collections.emptyMap();
		}
	}

	private static LanguageSettings loadLanguages() {
		try (InputStream stream = StfrVoiceCatalog.class.getResourceAsStream(
				"/assets/hbmstfr/voice_languages.json")) {
			if (stream == null) return new LanguageSettings("en", Collections.emptyMap());
			JsonObject data = new JsonParser()
					.parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			Map<String, Set<String>> locales = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("languages").entrySet()) {
				Set<String> names = new LinkedHashSet<>();
				for (JsonElement name : entry.getValue().getAsJsonArray())
					names.add(name.getAsString().toLowerCase(Locale.ROOT));
				locales.put(entry.getKey(), Collections.unmodifiableSet(names));
			}
			return new LanguageSettings(data.get("default_language").getAsString(),
					Collections.unmodifiableMap(locales));
		} catch (Exception exception) {
			LogManager.getLogger("STFR").error("Cannot load STFR voice languages", exception);
			return new LanguageSettings("en", Collections.emptyMap());
		}
	}

	private static final class LanguageSettings {
		final String defaultLanguage;
		final Map<String, Set<String>> locales;

		LanguageSettings(String defaultLanguage, Map<String, Set<String>> locales) {
			this.defaultLanguage = defaultLanguage;
			this.locales = locales;
		}
	}
}

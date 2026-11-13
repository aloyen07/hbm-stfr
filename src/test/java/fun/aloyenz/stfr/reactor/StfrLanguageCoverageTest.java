package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.text.TextComponentTranslation;
import org.junit.jupiter.api.Test;

public final class StfrLanguageCoverageTest {
	private static final class FormatProbe extends TextComponentTranslation {
		FormatProbe() { super("test"); }
		void validate(String value) { initializeFromFormat(value); }
	}

	@Test
	void monitorAndShutdownTranslationsCanActuallyBeFormatted() throws Exception {
		for (String language : new String[] {"ru_ru", "en_us"}) {
			Map<String, String> values = new HashMap<>();
			try (InputStream stream = getClass().getClassLoader().getResourceAsStream(
					"assets/hbmstfr/lang/" + language + ".lang")) {
				assertNotNull(stream);
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
					reader.lines().filter(line -> line.contains("=")).forEach(line -> {
						int separator = line.indexOf('=');
						values.put(line.substring(0, separator), line.substring(separator + 1));
					});
				}
			}
			for (String key : new String[] {
				"message.hbmstfr.reactor.shutdown.shield_unsafe",
				"gui.hbmstfr.mainframe.feedback.shutdown_unsafe",
				"gui.hbmstfr.mainframe.feedback.unsafe_shutdown"
			}) {
				String value = values.get(key);
				assertNotNull(value, language + ": " + key);
				assertDoesNotThrow(() -> new FormatProbe().validate(value), language + ": " + key);
				String formatted = String.format(Locale.ROOT, value);
				assertTrue(formatted.contains("60%"));
				assertTrue(formatted.contains("20%"));
				assertFalse(formatted.contains("%%"));
			}
			assertEquals(language.equals("ru_ru") ? "НЕТ" : "NO",
					values.get("gui.hbmstfr.monitor.no_signal.first"));
			assertEquals(language.equals("ru_ru") ? "СИГНАЛА" : "SIGNAL",
					values.get("gui.hbmstfr.monitor.no_signal.second"));
		}
	}
	@Test
	void unsafeShutdownAnnouncementHasClientTranslations() throws Exception {
		String key = "message.hbmstfr.reactor.shutdown.shield_unsafe=";
		for (String language : new String[] {"ru_ru", "en_us"}) {
			String path = "assets/hbmstfr/lang/" + language + ".lang";
			InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
			assertNotNull(stream, path);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				assertEquals(1, reader.lines().filter(line -> line.startsWith(key)
						&& line.length() > key.length()).count(), path);
			}
		}
	}
}

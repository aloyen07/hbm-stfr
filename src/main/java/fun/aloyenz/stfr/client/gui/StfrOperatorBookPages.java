package fun.aloyenz.stfr.client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Markdown source is copied into the jar unchanged, one section per physical page. */
final class StfrOperatorBookPages {

	private static final Map<String, List<Page>> CACHED = new HashMap<>();

	static final class Page {
		final String title;
		final String body;

		Page(String title, String body) {
			this.title = title;
			this.body = body;
		}
	}

	private StfrOperatorBookPages() {}

	static List<Page> all(boolean secondEdition, boolean russian) {
		String name = "operator_" + (secondEdition ? "mk2_" : "")
				+ (russian ? "ru" : "en") + ".md";
		return CACHED.computeIfAbsent(name,
				key -> Collections.unmodifiableList(read(key, russian)));
	}

	private static List<Page> read(String name, boolean russian) {
		InputStream stream = StfrOperatorBookPages.class.getResourceAsStream(
				"/assets/hbmstfr/books/" + name);
		if (stream == null) return Collections.singletonList(
				new Page("STFR", russian ? "Текст руководства не найден." : "Manual text is missing."));
		StringBuilder source = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) source.append(line).append('\n');
		} catch (IOException ex) {
			return Collections.singletonList(new Page("STFR", russian ? "Не удалось прочитать руководство." : "Could not read the manual."));
		}
		List<Page> pages = new ArrayList<>();
		for (String section : source.toString().split("(?m)^---[ \\t]*$")) {
			String text = section.trim();
			if (text.isEmpty()) continue;
			String[] lines = text.split("\\R");
			String title = lines[0].replaceFirst("^#+\\s*", "").trim();
			StringBuilder body = new StringBuilder();
			for (int i = 1; i < lines.length; i++) {
				if (body.length() > 0) body.append('\n');
				body.append(lines[i].replaceFirst("^#+\\s*", ""));
			}
			pages.add(new Page(title, body.toString().trim()));
		}
		return pages.isEmpty()
				? Collections.singletonList(new Page("STFR", russian ? "Руководство пусто." : "Manual is empty."))
				: pages;
	}
}

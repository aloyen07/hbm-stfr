package fun.aloyenz.stfr.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

final class StfrOperatorBookPagesTest {
	@Test
	void bothEditionsLoadPackagedPagesInBothLanguages() {
		for (boolean secondEdition : new boolean[] {false, true}) {
			for (boolean russian : new boolean[] {false, true}) {
				List<StfrOperatorBookPages.Page> pages = StfrOperatorBookPages.all(secondEdition, russian);
				assertTrue(pages.size() > 4, "Manual must contain its actual pages, not a missing-text fallback");
				assertEquals("STFR", pages.get(0).title);
				assertTrue(pages.get(0).body.contains(russian ? "Руководство оператора" : "Operator's Manual"));
				assertTrue(pages.get(0).body.contains(russian
						? secondEdition ? "Второе издание" : "Первое издание"
						: secondEdition ? "Second Edition" : "First Edition"));
			}
		}
	}
}

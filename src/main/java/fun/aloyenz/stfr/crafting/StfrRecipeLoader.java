package fun.aloyenz.stfr.crafting;

import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.main.MainRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;

/** Load each addon table after HBM; join HBM's normal recipe sync/reload thereafter. */
public final class StfrRecipeLoader {

	private StfrRecipeLoader() {}

	public static void initialize(SerializableRecipe handler) {
		if (!SerializableRecipe.recipeHandlers.contains(handler))
			SerializableRecipe.recipeHandlers.add(handler);
		handler.deleteRecipes();
		File folder = new File(MainRegistry.configDir, "hbmRecipes");
		File file = new File(folder, handler.getFileName());
		InputStream synced = SerializableRecipe.recipeSyncHandlers.get(handler.getFileName());
		if (synced != null) {
			try {
				synced.reset();
				handler.readRecipeStream(new InputStreamReader(synced, StandardCharsets.UTF_8));
			} catch (IOException error) {
				throw new IllegalStateException("Cannot read STFR synced recipes", error);
			}
			handler.modified = true;
		} else if (file.isFile()) {
			handler.readRecipeFile(file);
			handler.modified = true;
		} else {
			handler.registerDefaults();
			// The underscore file is a user-editable template.  Rewriting it at every
			// startup silently discarded recipe changes before it could be renamed.
			File template = new File(folder, "_" + handler.getFileName());
			if (!template.isFile()) handler.writeTemplateFile(template);
			handler.modified = false;
		}
		handler.registerPost();
		LogManager.getLogger("STFR")
				.info(
						"STFR recipe table {}: {}",
						handler.getFileName(),
						handler.modified ? "custom JSON" : "defaults");
	}
}

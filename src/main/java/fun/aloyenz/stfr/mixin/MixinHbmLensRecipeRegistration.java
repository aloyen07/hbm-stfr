package fun.aloyenz.stfr.mixin;

import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aloyenz.stfr.crafting.StfrLensArcRecipes;

/** Include addon recipes after defaults, custom JSON, and recipe synchronization. */
@Mixin(value = SerializableRecipe.class, remap = false)
public abstract class MixinHbmLensRecipeRegistration {
	@Inject(method = "registerPost", at = @At("HEAD"))
	private void stfr$registerLenses(CallbackInfo ci) {
		if ((Object) this instanceof ArcFurnaceRecipes) StfrLensArcRecipes.register();
	}
}

package fun.aloyenz.stfr.mixin;

import com.hbm.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import fun.aloyenz.stfr.crafting.StfrLensArcRecipes;

@Mixin(value = TileEntityMachineArcFurnaceLarge.class, remap = false)
public abstract class MixinHbmArcFurnaceLenses {
	@Redirect(method = "process", at = @At(value = "INVOKE", target =
			"Lcom/hbm/inventory/recipes/ArcFurnaceRecipes;getOutput(Lnet/minecraft/item/ItemStack;Z)Lcom/hbm/inventory/recipes/ArcFurnaceRecipes$ArcFurnaceRecipe;"))
	private ArcFurnaceRecipe stfr$completeLensPass(ItemStack input, boolean liquid) {
		return StfrLensArcRecipes.completedPass(input, liquid,
				((TileEntityMachineArcFurnaceLarge) (Object) this).getWorld().rand);
	}
}

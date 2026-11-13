package fun.aloyenz.stfr.mixin.early;

import com.hbm.items.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.aloyenz.stfr.meme.StfrCatEvents;

/** Trigger only when fire actually destroys the dropped item, not on contact. */
@Mixin(EntityItem.class)
public abstract class MixinEntityItemBurn extends Entity {
    @Unique private boolean stfr$burnCandidate;
    protected MixinEntityItemBurn(World world) { super(world); }
    @Inject(method={"attackEntityFrom", "func_70097_a"}, at=@At("HEAD"), remap=false)
    private void stfr$beforeFire(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        EntityItem item = (EntityItem)(Object)this;
        stfr$burnCandidate = !world.isRemote && !isDead && source.isFireDamage()
                && !item.getItem().isEmpty() && item.getItem().getItem() == ModItems.polaroid;
    }
    @Inject(method={"attackEntityFrom", "func_70097_a"}, at=@At("RETURN"), remap=false)
    private void stfr$afterFire(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (stfr$burnCandidate && isDead) StfrCatEvents.burned(world, getPosition());
        stfr$burnCandidate = false;
    }
}

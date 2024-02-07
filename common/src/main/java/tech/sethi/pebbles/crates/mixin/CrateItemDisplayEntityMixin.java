package tech.sethi.pebbles.crates.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tech.sethi.pebbles.crates.impl.ItemDisplayEntityImpl;

@Mixin(DisplayEntity.ItemDisplayEntity.class)
public abstract class CrateItemDisplayEntityMixin implements ItemDisplayEntityImpl {

    @Shadow
    abstract void setItemStack(ItemStack stack);

    @Override
    public void pebbles_items$publicSetStack(ItemStack stack) {
        setItemStack(stack);
    }
}
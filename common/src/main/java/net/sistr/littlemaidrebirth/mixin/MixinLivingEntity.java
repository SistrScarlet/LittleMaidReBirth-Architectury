package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements LivingAccessor {

    @Shadow
    protected abstract void tickActiveItemStack();

    @Shadow
    public abstract boolean blockedByShield(DamageSource source);

    @Shadow
    protected abstract void sendEquipmentChanges();

    @Shadow
    public abstract void equipStack(EquipmentSlot var1, ItemStack var2);

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot var1);

    @Override
    public void applyEquipmentAttributes_LM() {
        sendEquipmentChanges();
    }

    @Override
    public void tickActiveItemStack_LM() {
        tickActiveItemStack();
    }

    @Override
    public boolean blockedByShield_LM(DamageSource source) {
        return blockedByShield(source);
    }

    //Lithium導入下でのバグ修正
    @Inject(method = "checkHandStackSwap",
            at = @At(value = "HEAD"))
    private void onCheckHandStackSwap(Map<EquipmentSlot, ItemStack> equipmentChanges, CallbackInfo ci) {
        if ((Object) this instanceof LittleMaidEntity) {
            equipStack(EquipmentSlot.HEAD, getEquippedStack(EquipmentSlot.HEAD));
        }
    }
}

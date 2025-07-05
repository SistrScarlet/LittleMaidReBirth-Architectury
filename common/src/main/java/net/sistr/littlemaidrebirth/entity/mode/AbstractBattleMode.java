package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;

import java.util.Optional;

public abstract class AbstractBattleMode<T> extends Mode {
    protected final MobEntity mob;
    protected LivingEntity target;
    protected ItemStack weaponStack;
    protected T weapon;

    protected AbstractBattleMode(MobEntity mob, ModeType<? extends Mode> modeType, String name) {
        super(modeType, name);
        this.mob = mob;
    }

    public boolean shouldExecute() {
        if (this.mob.getTarget() == null
                || !this.mob.getTarget().isAlive()) {
            return false;
        }
        this.target = this.mob.getTarget();

        var main = this.mob.getMainHandStack();
        if (isWeapon(main)) {
            this.weaponStack = main;
            this.weapon = getWeaponInstance(main).orElseThrow();
            return true;
        }

        return false;
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public boolean isBattleMode() {
        return true;
    }

    protected boolean isWeapon(ItemStack stack) {
        return getWeaponInstance(stack).isPresent();
    }

    protected abstract Optional<T> getWeaponInstance(ItemStack stack);
}

package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public class MixinCakeBlock {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUseInjection(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (world instanceof ServerWorld serverWorld) {
            var worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState(serverWorld);
            var maidSouls = worldMaidSoulState.get(player.getUuid());
            if (maidSouls.isEmpty()) {
                return;
            }
            for (WorldMaidSoulState.MaidSoul maidSoul : maidSouls) {
                var maid = Registration.LITTLE_MAID_MOB.get().create(serverWorld);
                if (maid != null) {
                    maid.installMaidSoul(maidSoul);
                    maid.refreshPositionAfterTeleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    serverWorld.spawnEntity(maid);
                }
            }
            worldMaidSoulState.remove(player.getUuid());
            worldMaidSoulState.markDirty();
            world.removeBlock(pos, false);
            world.playSound(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1.0f, 2.0f);
            world.playSound(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 2.0f);
            //todo 演出強化
            ((ServerWorld) world).spawnParticles(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0, 0, 0, 0);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}

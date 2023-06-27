package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CandleCakeBlock.class)
public abstract class MixinCandleCakeBlock {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUseInjection(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                                BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStack = player.getStackInHand(hand);
        //着火するときを取得できなさそうだったので、手動で判定
        //クライアントでは動かない
        if ((itemStack.isOf(Items.FLINT_AND_STEEL)
                || itemStack.isOf(Items.FIRE_CHARGE)
                || itemStack.isIn(ItemTags.CREEPER_IGNITERS))
                && CandleCakeBlock.canBeLit(state)
                && getAroundAlterComponentBlocks(world, pos) >= 4
                && world instanceof ServerWorld serverWorld) {
            var worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState(serverWorld);
            var maidSouls = worldMaidSoulState.get(player.getUuid());
            if (maidSouls.isEmpty()) {
                //todo なんか報酬
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
            float size = 0.5f;
            int count = 10;
            double delta = 1.5;
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.65f, 0.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 1.0f, 0.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 1.0f, 0.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 1.0f, 1.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 0.0f, 1.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    new DustParticleEffect(new Vector3f(0.5f, 0.0f, 1.0f), size),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            ((ServerWorld) world).spawnParticles(
                    ParticleTypes.HEART,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    count, delta, delta, delta, 0);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    private static int getAroundAlterComponentBlocks(World world, BlockPos center) {
        int num = 0;
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }
            var blockState = world.getBlockState(center.add((i % 3) - 1, 0, (i / 3) - 1));
            if (blockState.isIn(LMTags.Blocks.MAID_ALTER_COMPONENT_BLOCKS)) {
                num++;
            }
        }
        return num;
    }
}

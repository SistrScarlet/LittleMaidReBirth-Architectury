package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.MaidResurrection;
import net.sistr.littlemaidrebirth.tags.LMTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CandleCakeBlock.class)
public abstract class MixinCandleCakeBlock {

    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void onUseInjection(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<ItemActionResult> cir) {
        // 着火するときを取得できなさそうだったので、手動で判定
        // クライアントでは動かない
        if ((stack.getItem() instanceof FlintAndSteelItem
                        || stack.getItem() instanceof FireChargeItem
                        || stack.isIn(ItemTags.CREEPER_IGNITERS))
                && CandleCakeBlock.canBeLit(state)
                && LMRB$getAroundAlterComponentBlocks(world, pos) >= 4
                && world instanceof ServerWorld serverWorld) {
            if (MaidResurrection.resurrect(serverWorld, pos, player)) {
                cir.setReturnValue(ItemActionResult.SUCCESS);
            }
        }
    }

    @Unique
    private static int LMRB$getAroundAlterComponentBlocks(World world, BlockPos center) {
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

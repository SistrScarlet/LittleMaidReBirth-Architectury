package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.tags.LMTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
                && LMRB$getAroundAlterComponentBlocks(world, pos) >= 4
                && world instanceof ServerWorld serverWorld) {
            LittleMaidEntity.resurrectionMaid(serverWorld, pos, player);
            cir.setReturnValue(ActionResult.SUCCESS);
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

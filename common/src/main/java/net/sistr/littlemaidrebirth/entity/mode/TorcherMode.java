package net.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.TorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.util.BlockFinderPD;
import org.jetbrains.annotations.Nullable;

//暗所発見->移動->設置
//置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final float distance;
    protected BlockPos placePos;
    protected int recalcPathTimer;
    protected int failPlaceTimer;
    protected int count;
    @Nullable
    protected BlockFinderPD blockFinder;

    public TorcherMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float distance) {
        super(modeType, name);
        this.mob = mob;
        this.distance = distance;
    }

    @Override
    public boolean shouldExecute() {
        //手に持っているものがブロックでないといけない
        Item item = mob.getMainHandStack().getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        //todo blockFinderを使いまわす
        if (blockFinder == null || blockFinder.isEnd() || count++ > 100) {
            this.count = 0;
            BlockPos basePos;
            if (this.mob.getMovingMode() == MovingMode.ESCORT) {
                Entity owner = mob.getTameOwner().orElse(null);
                if (owner == null) {
                    return false;
                }
                basePos = owner.getBlockPos();
            } else {
                basePos = mob.getBlockPos();
            }
            blockFinder = new BlockFinderPD(ImmutableList.of(basePos),
                    pos -> isDark(pos) && isPlaceable(pos),
                    pos -> Math.abs(basePos.getY() - pos.getY()) < 3
                            && (isPlaceable(pos) || isPlaceable(pos.down()))
                            && pos.isWithinDistance(basePos, distance),
                    MathHelper.floor(distance * distance * 7));
            //探索済みブロック数の実測値に合わせてexpectedを指定
            //半径12 seed数874
        }
        //毎tick nブロック探索
        blockFinder.tick(10);
        placePos = blockFinder.getResult().orElse(null);
        return placePos != null;
    }

    public boolean isDark(BlockPos pos) {
        return mob.getWorld().getLightLevel(pos) <= LMRBMod.getConfig().getTorcherLightLevelThreshold();
    }

    public boolean isPlaceable(BlockPos pos) {
        return mob.getWorld().isAir(pos)
                && TorchBlock.sideCoversSmallSquare(this.mob.getWorld(), pos.down(), Direction.UP);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return placePos != null
                && mob.getMainHandStack().getItem() instanceof BlockItem;
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigation().stop();
        ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_D);
        this.mob.setSprinting(true);
    }

    @Override
    public void tick() {
        //なぜかnullの場合があるので必須
        if (placePos == null) {
            return;
        }
        //一定時間経過しても置けない、または明るい地点を無視
        if (60 < ++this.failPlaceTimer
                || LMRBMod.getConfig().getTorcherLightLevelThreshold()
                < mob.getWorld().getLightLevel(placePos)) {
            this.placePos = null;
            this.failPlaceTimer = 0;
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
        //距離が遠すぎる場合は無視
        if (this.distance * this.distance * 1.5f * 1.5f < distanceSq) {
            this.placePos = null;
            return;
        }
        //手の届く範囲でない場合、近づく
        if (3 * 3 < distanceSq) {
            if (--recalcPathTimer < 0) {
                recalcPathTimer = 20;
                Path path = this.mob.getNavigation().findPathTo(placePos.getX(), placePos.getY(), placePos.getZ(), 2);
                if (path == null || path.getEnd() == null
                        || !path.getEnd().getBlockPos().isWithinDistance(placePos, 3)) {
                    placePos = null;
                    return;
                }
                this.mob.getNavigation().startMovingAlong(path, 1.0);
            }
            return;
        }

        //shouldContinueExecutingでチェック済みなので、必ずitemはブロック
        ItemStack itemStack = mob.getMainHandStack();
        Item item = itemStack.getItem();
        assert item instanceof BlockItem;
        if (mob.getWorld().isAir(placePos)) {
            try {
                ((BlockItem) item).place(new AutomaticItemPlacementContext(mob.getWorld(), placePos, Direction.UP, itemStack, Direction.UP));
            } catch (Exception e) {
                LMRBMod.LOGGER.warn("Torcherでのブロック設置時に例外が発生しました。");
                e.printStackTrace();
            }
            mob.swingHand(Hand.MAIN_HAND);
            ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.count = 0;
        this.failPlaceTimer = 0;
        this.recalcPathTimer = 0;
        this.mob.setSprinting(false);
        this.mob.getNavigation().stop();
    }

}

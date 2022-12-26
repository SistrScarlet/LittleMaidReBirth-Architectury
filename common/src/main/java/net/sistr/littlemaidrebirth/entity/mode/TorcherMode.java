package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.block.TorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.FakePlayer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.util.BlockFinder;

import java.util.Arrays;
import java.util.Optional;

//暗所発見->移動->設置
//置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final float distance;
    protected BlockPos placePos;
    protected BlockPos basePos;
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public TorcherMode(ModeType<? extends Mode> modeType, String name, LittleMaidEntity mob, float distance) {
        super(modeType, name);
        this.mob = mob;
        this.distance = distance;
    }

    @Override
    public boolean shouldExecute() {
        if (0 < --cool) {
            return false;
        }
        cool = 10;
        //手に持っているものがブロックでないといけない
        Item item = mob.getMainHandStack().getItem();
        if (!(item instanceof BlockItem)) {
            return false;
        }
        if (this.mob.getMovingMode() == MovingMode.ESCORT) {
            Entity owner = mob.getTameOwner().orElse(null);
            if (owner == null) {
                return false;
            }
            basePos = owner.getBlockPos();
        } else {
            basePos = mob.getBlockPos();
        }
        placePos = findSpawnablePoint(basePos.up())
                .orElse(null);
        return placePos != null;
    }

    //湧けるブロックを探索
    public Optional<BlockPos> findSpawnablePoint(BlockPos base) {
        return BlockFinder.searchTargetBlock(base,
                pos -> isDark(pos)
                        && isPlaceable(pos),
                pos -> Math.abs(base.getY() - pos.getY()) < 3
                        && mob.world.isAir(pos)
                        && pos.isWithinDistance(base, distance),
                Arrays.asList(Direction.values()), 128);
    }

    public boolean isDark(BlockPos pos) {
        return mob.world.getLightLevel(pos) <= 8;
    }

    public boolean isPlaceable(BlockPos pos) {
        return mob.world.isAir(pos)
                && TorchBlock.sideCoversSmallSquare(this.mob.world, pos.down(), Direction.UP);
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
    }

    @Override
    public void tick() {
        //3秒経過しても置けない、または明るい地点を無視
        if (60 < ++this.timeToIgnore || 8 < mob.world.getLightLevel(placePos)) {
            this.placePos = null;
            this.timeToIgnore = 0;
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
        //距離が遠すぎる場合は無視
        if (this.distance * this.distance * 4f < distanceSq) {
            this.placePos = null;
            return;
        }
        //手の届く範囲でない場合、近づく
        if (3 * 3 < distanceSq) {
            if (--timeToRecalcPath < 0) {
                timeToRecalcPath = 20;
                Path path = this.mob.getNavigation().findPathTo(placePos.getX(), placePos.getY(), placePos.getZ(), 3);
                if (path == null) {
                    placePos = null;
                    return;
                }
                this.mob.getNavigation().startMovingAlong(path, 1.2);
            }
            return;
        }

        //プレイヤーと同じ処理で設置する

        Vec3d start = mob.getCameraPosVec(1F);
        //終端はブロックの下面
        Vec3d end = new Vec3d(
                placePos.getX() + 0.5D, placePos.getY() - 0.1, placePos.getZ() + 0.5D);
        BlockHitResult result = mob.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.mob));
        FakePlayer fakePlayer = mob.getFakePlayer();
        //shouldContinueExecutingでチェック済みなので、必ずitemはブロック
        Item item = mob.getMainHandStack().getItem();
        assert item instanceof BlockItem;
        if (result.getType() != HitResult.Type.MISS
                && ((BlockItem) item).place(new ItemPlacementContext(
                new ItemUsageContext(fakePlayer, Hand.MAIN_HAND, result))).shouldSwingHand()) {
            mob.swingHand(Hand.MAIN_HAND);
            ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.cool = 2;
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
    }

}

package net.sistr.lmrb.entity.mode;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmrb.api.mode.Mode;
import net.sistr.lmrb.api.mode.ModeManager;
import net.sistr.lmrb.entity.FakePlayer;
import net.sistr.lmrb.entity.FakePlayerSupplier;
import net.sistr.lmrb.entity.Tameable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

//暗所発見->移動->設置
//置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode<T extends PathAwareEntity & FakePlayerSupplier & Tameable> implements Mode {
    protected final T mob;
    protected final float distance;
    protected BlockPos placePos;
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public TorcherMode(T mob, float distance) {
        this.mob = mob;
        this.distance = distance;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (0 < --cool) {
            return false;
        }
        cool = 20;
        BlockPos base;
        if (mob.getMovingState() == Tameable.MovingState.ESCORT) {
            Entity owner = mob.getTameOwner().orElse(null);
            if (owner == null) {
                return false;
            }
            base = owner.getBlockPos();
        } else {
            base = mob.getBlockPos();
        }
        placePos = findSpawnablePoint(base, this.distance)
                .orElse(null);
        return placePos != null;
    }

    //湧けるブロックを探索
    public Optional<BlockPos> findSpawnablePoint(BlockPos base, float distance) {
        BlockPos start = base.add(-distance, -1, -distance);
        BlockPos end = base.add(distance, 1, distance);
        List<BlockPos> points = new ArrayList<>();
        BlockPos.stream(start, end).forEach(pos -> points.add(pos.toImmutable()));
        return points.stream()
                .sorted(Comparator.comparingDouble(base::getManhattanDistance))
                .filter(this::isSpawnable)
                .filter(this::isReachable)
                .findFirst();
    }

    public boolean isSpawnable(BlockPos pos) {
        BlockPos posUp = pos.up();
        return mob.world.getBlockState(pos).isFullCube(mob.world, pos) && mob.world.isAir(posUp)
                && mob.world.getLightLevel(posUp) <= 8;
    }

    public boolean isReachable(BlockPos pos) {
        Path path = mob.getNavigation().findPathTo(pos, 4);
        return path != null && path.reachesTarget();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return placePos != null;
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigation().stop();
        Path path = this.mob.getNavigation().findPathTo(placePos.getX(), placePos.getY(), placePos.getZ(), 3);
        this.mob.getNavigation().startMovingAlong(path, 1);
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_D);
        }
    }

    @Override
    public void tick() {
        //5秒経過しても置けない、または明るい地点を無視
        if (100 < ++this.timeToIgnore || 8 < mob.world.getLightLevel(placePos.up())) {
            this.placePos = null;
            this.timeToIgnore = 0;
            return;
        }
        //距離が遠すぎる場合は無視
        if (distance * 2F < placePos.getManhattanDistance(mob.getBlockPos())) {
            this.placePos = null;
            return;
        }
        Item item = mob.getMainHandStack().getItem();
        if (!(item instanceof BlockItem)) {
            return;
        }
        if (3 * 3 < this.mob.squaredDistanceTo(placePos.getX(), placePos.getY(), placePos.getZ())) {
            if (--timeToRecalcPath < 0) {
                timeToRecalcPath = 20;
                Path path = this.mob.getNavigation().findPathTo(placePos.getX(), placePos.getY(), placePos.getZ(), 3);
                this.mob.getNavigation().startMovingAlong(path, 1);
            }
            return;
        }
        Vec3d start = mob.getCameraPosVec(1F);
        //終端はブロックの上面
        Vec3d end = new Vec3d(
                placePos.getX() + 0.5D, placePos.getY() + 1D, placePos.getZ() + 0.5D);
        BlockHitResult result = mob.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, this.mob));
        FakePlayer fakePlayer = mob.getFakePlayer();
        if (((BlockItem) item).place(new ItemPlacementContext(
                new ItemUsageContext(fakePlayer, Hand.MAIN_HAND, result))).shouldSwingHand()) {
            mob.swingHand(Hand.MAIN_HAND);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
            }
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.cool = 20;
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundTag tag) {

    }

    @Override
    public void readModeData(CompoundTag tag) {

    }

    @Override
    public String getName() {
        return "Torcher";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(new TorcherModeItem());
        ModeManager.INSTANCE.register(TorcherMode.class, items);
    }

    public static class TorcherModeItem implements ModeManager.CheckModeItem {

        @Override
        public boolean checkModeItem(ItemStack stack) {
            Item item = stack.getItem();
            if (!(item instanceof BlockItem)) {
                return false;
            }
            Block block = ((BlockItem) item).getBlock();
            int lightValue = block.getDefaultState().getLuminance();
            return 13 <= lightValue;
        }

    }

}

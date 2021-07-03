package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
import net.sistr.littlemaidrebirth.entity.Tameable;
import net.sistr.littlemaidrebirth.util.BlockFinder;

import java.util.Arrays;
import java.util.Optional;

//暗所発見->移動->設置
//置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final float distance;
    protected BlockPos placePos;
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
        placePos = findSpawnablePoint(base)
                .orElse(null);
        return placePos != null;
    }

    //湧けるブロックを探索
    public Optional<BlockPos> findSpawnablePoint(BlockPos base) {
        return BlockFinder.searchTargetBlock(base, pos -> this.isSpawnable(pos) && this.isReachable(pos),
                pos -> true,
                Arrays.asList(Direction.values()), 4000);

        /*BlockPos start = base.add(-distance, -1, -distance);
        BlockPos end = base.add(distance, 1, distance);
        List<BlockPos> points = new ArrayList<>();
        BlockPos.stream(start, end).forEach(pos -> points.add(pos.toImmutable()));
        return points.stream()
                .sorted(Comparator.comparingDouble(base::getManhattanDistance))
                .filter(this::isSpawnable)
                .filter(this::isReachable)
                .findFirst();*/
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
        ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_D);
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
            ((SoundPlayable) mob).play(LMSounds.INSTALLATION);
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.cool = 20;
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
    }

}

package net.sistr.littlemaidrebirth.util;

import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * 視界に関するユーティリティ
 */
public class SightUtil {

    public static List<Entity> getInSightEntities(World world, Entity entity, Vec3d viewPos, Vec3d lookFor,
                                                  float distance, float yawFov, float pitchFov, float targetExpand,
                                                  Predicate<Entity> predicate) {
        Vec3d lookTo = lookFor.normalize().multiply(distance);
        var bb = new Box(
                viewPos.getX(),
                viewPos.getY(),
                viewPos.getZ(),
                viewPos.getX() + lookTo.getX(),
                viewPos.getY() + lookTo.getY(),
                viewPos.getZ() + lookTo.getZ())
                .expand(1);
        var sightChecker = getSightChecker(viewPos, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov);
        return world.getOtherEntities(entity, bb, inBB -> {
            if (sightChecker.check(inBB.getBoundingBox().expand(targetExpand)) == SightState.HIDE) {
                return false;
            }
            return predicate.test(inBB);
        });
    }

    //rollなしの視錐台を作成し、それとチェックする
    //z軸方向はチェックしない
    public static SightChecker getSightChecker(Vec3d viewPos, Vec3d lookFor, float yaw, float yawFov, float pitchFov) {
        lookFor = lookFor.normalize();
        //x軸をy軸に回して視線方向Pitch軸
        //視線方向と視線方向Pitch軸の外積から視線方向Yaw軸が得られる
        //視線方向から視線方向Pitch軸にΘ - 90度回すと上面の法線(内向き)が得られる
        var lookForPitchAxis = rotate(new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), -yaw);
        var lookForYawAxis = lookFor.crossProduct(lookForPitchAxis);
        var upNorm = rotate(lookFor, lookForPitchAxis, pitchFov - 90f);
        var downNorm = rotate(lookFor, lookForPitchAxis, -pitchFov + 90f);
        var rightNorm = rotate(lookFor, lookForYawAxis, yawFov - 90f);
        var leftNorm = rotate(lookFor, lookForYawAxis, -yawFov + 90f);
        return new SightChecker() {
            @Override
            public boolean check(Vec3d targetPos) {
                Vec3d targetFor = targetPos.subtract(viewPos).normalize();
                //内積がマイナス=成す角が鈍角だったらダメ
                return 0 < upNorm.dotProduct(targetFor)
                        && 0 < downNorm.dotProduct(targetFor)
                        && 0 < rightNorm.dotProduct(targetFor)
                        && 0 < leftNorm.dotProduct(targetFor);
            }

            @Override
            public SightState check(Box box) {
                boolean upP = 0 < upNorm.dotProduct(positive(box, upNorm));
                boolean upN = 0 < upNorm.dotProduct(negative(box, upNorm));
                boolean downP = 0 < downNorm.dotProduct(positive(box, downNorm));
                boolean downN = 0 < downNorm.dotProduct(negative(box, downNorm));
                boolean rightP = 0 < rightNorm.dotProduct(positive(box, rightNorm));
                boolean rightN = 0 < rightNorm.dotProduct(negative(box, rightNorm));
                boolean leftP = 0 < leftNorm.dotProduct(positive(box, leftNorm));
                boolean leftN = 0 < leftNorm.dotProduct(negative(box, leftNorm));
                if (upP && upN && downP && downN && rightP && rightN && leftP && leftN) {
                    return SightState.ALL;
                }
                if ((upP || upN)
                        && (downP || downN)
                        && (rightP || rightN)
                        && (leftP || leftN)) {
                    return SightState.PARTIAL;
                }
                return SightState.HIDE;
            }

            private Vec3d positive(Box box, Vec3d norm) {
                double x = box.minX;
                double y = box.minY;
                double z = box.minZ;
                if (0 < norm.getX()) {
                    x = box.maxX;
                }
                if (0 < norm.getY()) {
                    y = box.maxY;
                }
                if (0 < norm.getZ()) {
                    z = box.maxZ;
                }
                return new Vec3d(x, y, z);
            }

            private Vec3d negative(Box box, Vec3d norm) {
                double x = box.minX;
                double y = box.minY;
                double z = box.minZ;
                if (norm.getX() < 0) {
                    x = box.maxX;
                }
                if (norm.getY() < 0) {
                    y = box.maxY;
                }
                if (norm.getZ() < 0) {
                    z = box.maxZ;
                }
                return new Vec3d(x, y, z);
            }
        };
    }

    public static SightState check(Entity viewer, Entity target, float yawFov, float pitchFov) {
        Vec3d view = viewer.getCameraPosVec(1F);
        Vec3d lookFor = viewer.getRotationVector();
        return getSightChecker(view, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov)
                .check(target.getBoundingBox());
    }

    public static boolean isInFrustum(Entity viewer, Entity target, float yawFov, float pitchFov) {
        Vec3d view = viewer.getCameraPosVec(1F);
        Vec3d lookFor = viewer.getRotationVector();
        return isInFrustum(view, target.getCameraPosVec(1F), lookFor, yawFov, pitchFov);
    }

    public static boolean isInFrustum(Vec3d viewPos, Vec3d targetPos, Vec3d lookFor, float yawFov, float pitchFov) {
        return getSightChecker(viewPos, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov)
                .check(targetPos);
    }

    public static Vec3d rotate(Vec3d vec, Vec3d axis, float angle) {
        var point = new Quaternionf((float) vec.x, (float) vec.y, (float) vec.z, 0);
        var rotate = RotationAxis.of(new Vector3f((float) axis.x, (float) axis.y, (float) axis.z)).rotationDegrees(angle);
        var rotateBar = new Quaternionf(rotate);
        rotateBar.conjugate();
        rotate.mul(point);
        rotate.mul(rotateBar);
        return new Vec3d(rotate.x(), rotate.y(), rotate.z());
    }

    //getYawPitch->getVecの場合、こちらのyawとpitchの値をマイナスにすること
    public static Vec3d getVec(float yaw, float pitch) {
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = -yaw * ((float) Math.PI / 180F);
        float yawCos = MathHelper.cos(yawRad);
        float yawSin = MathHelper.sin(yawRad);
        float pitchCos = MathHelper.cos(pitchRad);
        float pitchSin = MathHelper.sin(pitchRad);
        return new Vec3d(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
    }

    public static float[] getYawPitch(Vec3d vec) {
        double lookAtHorizontal = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float lookAtYaw = (float) (-MathHelper.atan2(vec.x, vec.z) * (180D / (float) Math.PI));
        float lookAtPitch = (float) (-MathHelper.atan2(vec.y, lookAtHorizontal) * (180D / (float) Math.PI));
        return new float[]{lookAtYaw, lookAtPitch};
    }

    /**
     * 透明ブロックを貫通して、点から点が見えるかどうか
     */
    public static boolean canSee(World world, @Nullable Entity entity, Vec3d view, Vec3d point) {
        Vec3d toEnd = point.subtract(view).normalize();
        for (int i = 0; i < 8; i++) {
            BlockHitResult result = world.raycast(
                    new RaycastContext(view, point,
                            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
            if (world.getBlockState(result.getBlockPos()).getMaterial() == Material.GLASS) {
                view = result.getPos().add(toEnd);
                continue;
            }
            return false;
        }
        return false;
    }

    public static void faceTo(Entity owner, Vec3d to, float maxYawIncrease, float maxPitchIncrease) {
        double x = to.getX() - owner.getX();
        double z = to.getZ() - owner.getZ();
        double y = to.getY() - owner.getEyeY();
        double horizon = Math.sqrt(x * x + z * z);
        float pitch = (float) (-(MathHelper.atan2(y, horizon) * (180D / Math.PI)));
        float yaw = (float) (MathHelper.atan2(z, x) * (180D / Math.PI)) - 90.0F;
        owner.setPitch(updateRotation(owner.getPitch(), pitch, maxPitchIncrease));
        owner.setYaw(updateRotation(owner.getYaw(), yaw, maxYawIncrease));
    }

    private static float updateRotation(float angle, float targetAngle, float maxIncrease) {
        float f = MathHelper.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }

    public static Vec3d[] getEight(Box box) {
        return new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
    }

    public interface SightChecker {
        boolean check(Vec3d targetPos);

        //デフォルト実装では8点全部が外になるくらいデカいBoxはダメ
        default SightState check(Box box) {
            var eight = getEight(box);
            var bbb = check(eight[0]);
            var bbt = check(eight[1]);
            var btb = check(eight[2]);
            var btt = check(eight[3]);
            var tbb = check(eight[4]);
            var tbt = check(eight[5]);
            var ttb = check(eight[6]);
            var ttt = check(eight[7]);
            if (bbb && bbt && btb && btt && tbb && tbt && ttb && ttt) {
                return SightState.ALL;
            }
            if (bbb || bbt || btb || btt || tbb || tbt || ttb || ttt) {
                return SightState.PARTIAL;
            }
            return SightState.HIDE;
        }
    }

    public enum SightState {
        ALL(true),
        PARTIAL(true),
        HIDE(false);
        private final boolean canSee;

        SightState(boolean canSee) {
            this.canSee = canSee;
        }

        public boolean isCanSee() {
            return canSee;
        }
    }

}

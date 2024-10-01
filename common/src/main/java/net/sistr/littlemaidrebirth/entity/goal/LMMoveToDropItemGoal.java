package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class LMMoveToDropItemGoal extends MoveToDropItemGoal {
    protected final LittleMaidEntity maid;

    public LMMoveToDropItemGoal(LittleMaidEntity maid, int range, int frequency, double speed) {
        super(maid, range, frequency, speed);
        this.maid = maid;
    }

    @Override
    public boolean isInventoryFull() {
        var inv = this.maid.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    //todo コンフィグで設定可能にする
    public boolean isOwnerRange(Entity entity, Entity owner) {
        Vec3d ownerPos = owner.getPos();
        Vec3d entityPos = entity.getPos().subtract(ownerPos);
        Vec3d ownerRot = owner.getRotationVec(1F).multiply(4);
        double dot = entityPos.dotProduct(ownerRot);
        double range = 4;
        //プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        //かつ内積の大きさが4m以下
        return 0 < dot && dot < range * range;
    }
}

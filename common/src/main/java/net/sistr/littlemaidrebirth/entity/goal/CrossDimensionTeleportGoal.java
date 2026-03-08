package net.sistr.littlemaidrebirth.entity.goal;

import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

/** ESCORT モードのメイドさんが、別ディメンションに居る主人の元へテレポートする Goal。 */
public class CrossDimensionTeleportGoal extends Goal {
  private final LittleMaidEntity maid;
  @Nullable private ServerPlayerEntity owner;

  public CrossDimensionTeleportGoal(LittleMaidEntity maid) {
    this.maid = maid;
  }

  @Override
  public boolean canStart() {
    if (maid.getMovingMode() != MovingMode.ESCORT) {
      return false;
    }
    if (TameableUtil.isWait(maid)) {
      return false;
    }
    if (!(maid.getWorld() instanceof ServerWorld serverWorld)) {
      return false;
    }
    UUID ownerUuid = maid.getOwnerUuid();
    if (ownerUuid == null) {
      return false;
    }
    ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
    if (player == null) {
      return false;
    }
    if (player.getWorld() == maid.getWorld()) {
      return false;
    }
    this.owner = player;
    return true;
  }

  @Override
  public void start() {
    if (owner == null) {
      return;
    }
    ServerWorld destWorld = owner.getServerWorld();
    maid.teleport(
        destWorld, owner.getX(), owner.getY(), owner.getZ(), Set.of(), owner.getYaw(), 0F);
  }

  @Override
  public boolean shouldContinue() {
    return false;
  }

  @Override
  public void stop() {
    this.owner = null;
  }
}

package net.sistr.littlemaidrebirth.entity;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.setup.Registration;
import org.joml.Vector3f;

public final class MaidResurrection {

  private MaidResurrection() {}

  public static boolean resurrect(ServerWorld world, BlockPos pos, PlayerEntity player) {
    var maidSouls = ((MaidManager) player).getMaidSouls();
    if (maidSouls.isEmpty()) {
      return false;
    }
    for (MaidSoul maidSoul : maidSouls) {
      var maid = Registration.LITTLE_MAID_MOB.get().create(world);
      if (maid != null) {
        maid.installMaidSoul(maidSoul);
        maid.refreshPositionAfterTeleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        maid.setMovingMode(MovingMode.ESCORT);
        TameableUtil.setWait(maid, true);
        maid.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, player.getEyePos());
        maid.getLookControl().lookAt(player);

        maid.extinguish();
        maid.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 10));

        world.spawnEntity(maid);

        // TODO: NeoForge の RegisterEvent タイミングで Criteria 登録対応後に復活
        // LMRBCriteria.RESURRECT_MAID.trigger((ServerPlayerEntity) player, maid);
      }
    }
    ((MaidManager) player).clearMaidSouls();

    world.removeBlock(pos, false);
    playResurrectionEffects(world, pos);

    return true;
  }

  // todo 演出強化
  private static void playResurrectionEffects(ServerWorld world, BlockPos pos) {
    double cx = pos.getX() + 0.5;
    double cy = pos.getY() + 0.5;
    double cz = pos.getZ() + 0.5;

    world.playSound(
        null,
        cx,
        pos.getY(),
        cz,
        SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE,
        SoundCategory.PLAYERS,
        1.0f,
        2.0f);
    world.playSound(
        null,
        cx,
        pos.getY(),
        cz,
        SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
        SoundCategory.PLAYERS,
        1.0f,
        2.0f);

    world.spawnParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0, 0, 0, 0);

    float size = 0.5f;
    int count = 10;
    double delta = 1.5;
    float[][] colors = {
      {1.0f, 0.0f, 0.0f},
      {1.0f, 0.65f, 0.0f},
      {1.0f, 1.0f, 0.0f},
      {0.0f, 1.0f, 0.0f},
      {0.0f, 1.0f, 1.0f},
      {0.0f, 0.0f, 1.0f},
      {0.5f, 0.0f, 1.0f},
    };
    for (float[] color : colors) {
      world.spawnParticles(
          new DustParticleEffect(new Vector3f(color[0], color[1], color[2]), size),
          cx,
          cy,
          cz,
          count,
          delta,
          delta,
          delta,
          0);
    }
    world.spawnParticles(ParticleTypes.HEART, cx, cy, cz, count, delta, delta, delta, 0);
  }
}

package net.sistr.littlemaidrebirth.gametest.fabric;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class GameTestHelperImpl {

  public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
    return FakePlayer.get(world, new GameProfile(UUID.randomUUID(), name));
  }
}

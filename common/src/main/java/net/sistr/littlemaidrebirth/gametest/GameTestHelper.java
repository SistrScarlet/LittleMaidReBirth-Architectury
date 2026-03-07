package net.sistr.littlemaidrebirth.gametest;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class GameTestHelper {

  @ExpectPlatform
  public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
    throw new AssertionError();
  }
}

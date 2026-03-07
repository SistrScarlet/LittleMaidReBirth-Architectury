package net.sistr.littlemaidrebirth.gametest;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class GameTestHelper {

  @ExpectPlatform
  public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
    throw new AssertionError();
  }

  /** FakePlayer をワールドの players リストに登録する。getTameOwner() 等で検索可能になる。 */
  public static void registerPlayerInWorld(ServerWorld world, ServerPlayerEntity player) {
    world.onPlayerConnected(player);
  }
}

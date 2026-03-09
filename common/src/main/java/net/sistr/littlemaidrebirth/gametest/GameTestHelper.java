package net.sistr.littlemaidrebirth.gametest;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.Entity;
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

    /** FakePlayer をワールドから削除する。entity manager 経由で players リストからも除去される。 */
    public static void removePlayerFromWorld(ServerPlayerEntity player) {
        player.remove(Entity.RemovalReason.DISCARDED);
    }
}

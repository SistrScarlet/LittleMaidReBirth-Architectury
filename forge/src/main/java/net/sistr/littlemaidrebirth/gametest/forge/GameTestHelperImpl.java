package net.sistr.littlemaidrebirth.gametest.forge;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;

public class GameTestHelperImpl {

  public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
    return FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), name));
  }
}

package net.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 偽のPlayerクラス
 * サーバーオンリー
 * */
public class FakePlayer extends ServerPlayerEntity {

    public FakePlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
    }
}

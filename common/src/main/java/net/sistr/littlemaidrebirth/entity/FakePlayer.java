package net.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 偽のPlayerクラス
 * サーバーオンリー
 * */
public class FakePlayer extends ServerPlayerEntity {

    public FakePlayer(ServerWorld world, GameProfile profile) {
        super(world.getServer(), world, profile);
    }

}

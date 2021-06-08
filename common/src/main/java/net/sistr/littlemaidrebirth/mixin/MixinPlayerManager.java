package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.sistr.littlemaidrebirth.entity.FakePlayerWrapperEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "getAdvancementTracker", at = @At("HEAD"), cancellable = true)
    public void onGetAdvancementTracker(ServerPlayerEntity player, CallbackInfoReturnable<PlayerAdvancementTracker> cir) {
        if (!(player instanceof FakePlayerWrapperEntity)) return;
        cir.setReturnValue(FakePlayerWrapperEntity.getFPWEAdvancementTracker()
                .orElseGet(() -> {
                    File file = this.server.getSavePath(WorldSavePath.ADVANCEMENTS).toFile();
                    File file2 = new File(file, FakePlayerWrapperEntity.getFPWEUuid() + ".json");
                    return FakePlayerWrapperEntity.initFPWEAdvancementTracker(this.server.getDataFixer(),
                            (PlayerManager) (Object) this, this.server.getAdvancementLoader(), file2, player);
                }));
    }

}

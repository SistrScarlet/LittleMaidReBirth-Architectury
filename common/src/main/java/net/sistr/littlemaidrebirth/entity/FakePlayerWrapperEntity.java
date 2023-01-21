package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.sistr.littlemaidrebirth.util.PlayerEntityInventoryAccessor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.UUID;

//アイテムの使用/アイテム回収/その他
/**
 * エンティティをプレイヤーにラップするクラス
 * サーバーオンリー
 * */
public abstract class FakePlayerWrapperEntity<T extends LivingEntity> extends FakePlayer {
    private static final UUID FPWE_UUID = UUID.fromString("8eabd891-5b4a-44f5-8ea4-89b04100baf6");
    private static final GameProfile FPWE_PROFILE = new GameProfile(FPWE_UUID, "fake_player_name");

    public FakePlayerWrapperEntity(T origin) {
        super((ServerWorld) origin.world, FPWE_PROFILE);
        networkHandler = new FakePlayNetworkHandler(getServer(), this);
    }

    public static UUID getFPWEUuid() {
        return FPWE_UUID;
    }

    public PlayerAdvancementTracker initFPWEAdvancementTracker(DataFixer dataFixer, PlayerManager playerManager,
                                                               ServerAdvancementLoader serverAdvancementLoader,
                                                               File file, ServerPlayerEntity serverPlayerEntity) {
        return new PlayerAdvancementTracker(dataFixer, playerManager, serverAdvancementLoader, file, serverPlayerEntity);
    }

    public abstract T getOrigin();

    @Override
    public void tick() {
        //Fencer
        ++lastAttackedTicks;
        ((LivingAccessor) this).applyEquipmentAttributes_LM();
        //Archer
        ((LivingAccessor) this).tickActiveItemStack_LM();

        //アイテム回収
        pickupItems();

        //InventoryTick
        ((PlayerEntityInventoryAccessor) this).getPlayerInventory_LMRB().updateItems();

        this.refreshPositionAndAngles(getOrigin().getX(), getOrigin().getY(), getOrigin().getZ(),
                this.getOrigin().getYaw(), this.getOrigin().getPitch());
    }

    protected void pickupItems() {
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            Box box2;
            if (this.hasVehicle() && !this.getVehicle().isRemoved()) {
                box2 = this.getBoundingBox().union(this.getVehicle().getBoundingBox()).expand(1.0D, 0.0D, 1.0D);
            } else {
                box2 = this.getBoundingBox().expand(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.world.getOtherEntities(this, box2);
            List<Entity> list2 = Lists.newArrayList();

            for (Entity entity : list) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list2.add(entity);
                } else if (!entity.isRemoved()) {
                    ((PlayerAccessor) this).onCollideWithEntity_LM(entity);
                }
            }

            if (!list2.isEmpty()) {
                ((PlayerAccessor) this).onCollideWithEntity_LM(Util.getRandom(list2, this.random));
            }
        }
    }

    @Override
    public void sendPickup(Entity item, int count) {
        getOrigin().sendPickup(item, count);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        return getOrigin().getDimensions(pose);
    }

    //id系

    @Override
    public int getId() {
        int id = getOrigin().getId();
        if (super.getId() != id) setId(id);
        return id;
    }

    @Override
    public UUID getUuid() {
        UUID uuid = getOrigin().getUuid();
        if (super.getUuid() != uuid) setUuid(uuid);
        return uuid;
    }

    @Override
    public String getUuidAsString() {
        return getOrigin().getUuidAsString();
    }

    //座標系

    @Override
    public Vec3d getPos() {
        Vec3d vec = getOrigin().getPos();
        setPos(vec.x, vec.y, vec.z);
        return vec;
    }

    @Override
    public double getEyeY() {
        return getOrigin().getEyeY();
    }

    @Override
    public BlockPos getBlockPos() {
        return getOrigin().getBlockPos();
    }

    @Override
    public Box getBoundingBox(EntityPose pose) {
        return getOrigin().getBoundingBox(pose);
    }

    //体力

    @Override
    public void heal(float amount) {
        getOrigin().heal(amount);
    }

    @Override
    public float getHealth() {
        return getOrigin().getHealth();
    }

    @Override
    public void setHealth(float health) {
        getOrigin().setHealth(health);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return getOrigin().damage(source, amount);
    }

    public static class FakePlayNetworkHandler extends ServerPlayNetworkHandler {

        public FakePlayNetworkHandler(MinecraftServer server, ServerPlayerEntity playerIn) {
            super(server, new FakeClientConnection(), playerIn);
        }

        @Override
        public void sendPacket(Packet<?> packet) {
        }

        @Override
        public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        }
    }

    public static class FakeClientConnection extends ClientConnection {

        public FakeClientConnection() {
            super(NetworkSide.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
        }
    }
}

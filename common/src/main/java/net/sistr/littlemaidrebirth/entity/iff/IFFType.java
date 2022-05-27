package net.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.village.Merchant;
import net.minecraft.world.World;

import java.util.Optional;

public class IFFType {
    protected IFFTag iffTag;
    protected final EntityType<?> entityType;
    protected Entity entity;

    public IFFType(IFFTag iffTag, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.entityType = entityType;
    }

    public IFF createIFF() {
        return new IFF(iffTag, this, entityType);
    }

    public boolean checkEntity(World world) {
        entity = entityType.create(world);
        if (entity instanceof Monster && !(entity instanceof CreeperEntity)
                && !(entity instanceof EndermanEntity)) {
            iffTag = IFFTag.ENEMY;
            return true;
        }
        if (entity instanceof AnimalEntity || entity instanceof WaterCreatureEntity
                || entity instanceof Npc || entity instanceof Merchant) {
            iffTag = IFFTag.FRIEND;
            return true;
        }
        return entity instanceof LivingEntity || this.entityType == EntityType.PLAYER;
    }

    public Optional<LivingEntity> getEntity() {
        return Optional.ofNullable((LivingEntity) entity);
    }

}

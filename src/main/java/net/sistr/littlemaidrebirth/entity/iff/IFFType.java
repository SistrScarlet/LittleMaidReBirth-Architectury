package net.sistr.littlemaidrebirth.entity.iff;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.text.TranslatableText;
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

    @Environment(EnvType.CLIENT)
    public void render(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        if (!(entity instanceof LivingEntity)) return;
        InventoryScreen.drawEntity(x, y, 15, mouseX, mouseY, (LivingEntity) entity);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        textRenderer.drawWithShadow(matrices, new TranslatableText(entityType.getTranslationKey()),
                (float) x + 60, (float) y - textRenderer.fontHeight, 0xFFFFFFFF);
    }

    public IFF createIFF() {
        return new IFF(iffTag, this, entityType);
    }

    public boolean checkEntity(World world) {
        entity = entityType.create(world);
        if (entity instanceof Monster && !(entity instanceof CreeperEntity)) {
            iffTag = IFFTag.ENEMY;
            return true;
        }
        if (entity instanceof AnimalEntity || entity instanceof WaterCreatureEntity
                || entity instanceof Npc || entity instanceof Merchant) {
            iffTag = IFFTag.FRIEND;
            return true;
        }
        return entity instanceof LivingEntity;
    }

    public Optional<LivingEntity> getEntity() {
        return Optional.ofNullable((LivingEntity) entity);
    }

}

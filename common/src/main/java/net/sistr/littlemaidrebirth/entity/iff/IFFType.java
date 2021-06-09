package net.sistr.littlemaidrebirth.entity.iff;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class IFFType {
    public static final Logger LOGGER = LogManager.getLogger();
    protected IFFTag iffTag;
    protected final EntityType<?> entityType;
    protected Entity entity;
    protected boolean renderClashed;

    public IFFType(IFFTag iffTag, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.entityType = entityType;
    }

    @Environment(EnvType.CLIENT)
    public void render(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        textRenderer.drawWithShadow(matrices, new TranslatableText(entityType.getTranslationKey()),
                (float) x + 60, (float) y - textRenderer.fontHeight, 0xFFFFFFFF);
        if (renderClashed || !(entity instanceof LivingEntity)) return;
        try {
            InventoryScreen.drawEntity(x, y, 15, mouseX, mouseY, (LivingEntity) entity);
        } catch (Exception e) {
            LOGGER.warn("描画処理がクラッシュしました。" + entityType + ":" + entity);
            e.printStackTrace();
            renderClashed = true;
            //行われない終了処理を行う
            //ちょっと強引
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            immediate.draw();
            EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            entityRenderDispatcher.setRenderShadows(true);
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
            DiffuseLighting.enableGuiDepthLighting();
        }
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

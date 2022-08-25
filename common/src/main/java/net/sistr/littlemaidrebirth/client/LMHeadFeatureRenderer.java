/*
 * Decompiled with CFR 0.1.1 (FabricMC 57d88659).
 */
package net.sistr.littlemaidrebirth.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.Vec3f;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Map;

@Environment(value = EnvType.CLIENT)
public class LMHeadFeatureRenderer<T extends LittleMaidEntity, M extends EntityModel<T>>
        extends FeatureRenderer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> headModels;

    public LMHeadFeatureRenderer(FeatureRendererContext<T, M> context, EntityModelLoader loader) {
        this(context, loader, 1.0f, 1.0f, 1.0f);
    }

    public LMHeadFeatureRenderer(FeatureRendererContext<T, M> context, EntityModelLoader loader, float scaleX, float scaleY, float scaleZ) {
        super(context);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.headModels = SkullBlockEntityRenderer.getModels(loader);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                       int light, T livingEntity,
                       float animationProgress, float g, float h, float j, float k, float l) {
        var lastStack = livingEntity.getInventory().getStack(18);
        var lastItem = lastStack.getItem();
        boolean showLastItem = !lastStack.isEmpty()
                && lastItem instanceof BlockItem
                && ((BlockItem) lastItem).getBlock() instanceof PlantBlock;
        ItemStack itemStack = ((LivingEntity) livingEntity).getEquippedStack(EquipmentSlot.HEAD);
        boolean showHeadItem = !itemStack.isEmpty();
        if (!showLastItem && !showHeadItem) {
            return;
        }
        matrixStack.push();
        matrixStack.scale(this.scaleX, this.scaleY, this.scaleZ);
        ((ModelWithHead) this.getContextModel()).getHead().rotate(matrixStack);
        if (showLastItem) {
            matrixStack.push();
            translate(matrixStack, false);
            matrixStack.translate(-0.5, 0.35, -0.5);
            MinecraftClient.getInstance().getBlockRenderManager()
                    .renderBlockAsEntity(((BlockItem) lastItem).getBlock().getDefaultState(),
                            matrixStack,
                            vertexConsumerProvider,
                            light,
                            OverlayTexture.DEFAULT_UV);
            matrixStack.pop();
        }

        if (showHeadItem) {
            Item item = itemStack.getItem();
            if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof AbstractSkullBlock) {
                NbtCompound nbtCompound;
                matrixStack.scale(1.1875f, -1.1875f, -1.1875f);
                GameProfile gameProfile = null;
                if (itemStack.hasNbt() && (nbtCompound = itemStack.getNbt()).contains("SkullOwner", 10)) {
                    gameProfile = NbtHelper.toGameProfile(nbtCompound.getCompound("SkullOwner"));
                }
                matrixStack.translate(-0.5, 0.0, -0.5);
                SkullBlock.SkullType skullType = ((AbstractSkullBlock) ((BlockItem) item).getBlock()).getSkullType();
                SkullBlockEntityModel skullBlockEntityModel = this.headModels.get(skullType);
                RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(skullType, gameProfile);
                SkullBlockEntityRenderer.renderSkull(null, 180.0f, animationProgress, matrixStack, vertexConsumerProvider, light, skullBlockEntityModel, renderLayer);
            } else if (!(item instanceof ArmorItem) || ((ArmorItem) item).getSlotType() != EquipmentSlot.HEAD) {
                translate(matrixStack, false);
                MinecraftClient.getInstance().getHeldItemRenderer()
                        .renderItem(livingEntity, itemStack, ModelTransformation.Mode.HEAD,
                                false, matrixStack, vertexConsumerProvider, light);
            }
        }
        matrixStack.pop();
    }

    public static void translate(MatrixStack matrices, boolean villager) {
        matrices.translate(0.0, -0.25, 0.0);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0f));
        matrices.scale(0.625f, -0.625f, -0.625f);
        if (villager) {
            matrices.translate(0.0, 0.1875, 0.0);
        }
    }
}


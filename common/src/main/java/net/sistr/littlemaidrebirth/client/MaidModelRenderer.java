package net.sistr.littlemaidrebirth.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelArmorLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelHeldItemLayer;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelLightLayer;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.maidmodel.ModelMultiBase;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMMatrixStack;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.Tameable;

import static net.sistr.littlemaidmodelloader.maidmodel.IModelCaps.*;

@Environment(EnvType.CLIENT)
public class MaidModelRenderer extends MobEntityRenderer<LittleMaidEntity, LMMultiModel<LittleMaidEntity>> {
    private static final Identifier NULL_TEXTURE = new Identifier(LMRBMod.MODID, "null");

    public MaidModelRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        //エラー吐くので<>消した(ゴリ押し)
        this.addFeature(new MultiModelArmorLayer(this));
        this.addFeature(new MultiModelHeldItemLayer(this));
        this.addFeature(new MultiModelLightLayer(this));
        this.addFeature(new LMHeadFeatureRenderer<>(this, ctx.getModelLoader()));
    }

    @Override
    protected void setupTransforms(LittleMaidEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .ifPresent(model -> model.setupTransform(entity.getCaps(),
                        new MMMatrixStack(matrices), animationProgress, bodyYaw, tickDelta));
    }

    @Override
    protected void scale(LittleMaidEntity entity, MatrixStack matrices, float amount) {
        entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .map(model -> (float) ((ModelMultiBase) model).getCapsValue(caps_ScaleFactor))
                .ifPresent(scale -> matrices.scale(scale, scale, scale));
    }

    @Override
    public void render(LittleMaidEntity livingEntity, float entityYaw, float partialTicks, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int light) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        profiler.push("littlemaidmodelloader:mm");
        livingEntity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelMultiBase)
                .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            livingEntity.getModel(IHasMultiModel.Layer.INNER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
            livingEntity.getModel(IHasMultiModel.Layer.OUTER, part)
                    .filter(model -> model instanceof ModelMultiBase)
                    .ifPresent(model -> syncCaps(livingEntity, (ModelMultiBase) model, partialTicks));
        }
        super.render(livingEntity, entityYaw, partialTicks, matrixStack, vertexConsumerProvider, light);
        profiler.pop();
    }

    public void syncCaps(LittleMaidEntity entity, ModelMultiBase model, float partialTicks) {
        float swingProgress = entity.getHandSwingProgress(partialTicks);
        float right = 0;
        float left = 0;
        if (entity.preferredHand == Hand.MAIN_HAND) {
            if (entity.getMainArm() == Arm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        } else {
            if (entity.getMainArm() != Arm.RIGHT) {
                right = swingProgress;
            } else {
                left = swingProgress;
            }
        }
        model.setCapsValue(caps_onGround, right, left);
        model.setCapsValue(caps_isRiding, entity.hasVehicle());
        model.setCapsValue(caps_isSneak, entity.isSneaking());
        model.setCapsValue(caps_isChild, entity.isBaby());
        model.setCapsValue(caps_heldItemLeft, 0F);
        model.setCapsValue(caps_heldItemRight, 0F);
        model.setCapsValue(caps_aimedBow, false);
        model.setCapsValue(caps_entityIdFactor, 0F);
        model.setCapsValue(caps_ticksExisted, entity.age);

        model.setCapsValue(IModelCaps.caps_aimedBow, entity.isAimingBow());
        model.setCapsValue(IModelCaps.caps_isWait, entity.getMovingState() == Tameable.MovingState.WAIT);
        model.setCapsValue(IModelCaps.caps_isContract, entity.hasTameOwner());
        model.setCapsValue(IModelCaps.caps_isBloodsuck, entity.isBloodSuck());
        model.setCapsValue(IModelCaps.caps_isClock, entity.getMainHandStack().getItem() == Items.CLOCK
                || entity.getOffHandStack().getItem() == Items.CLOCK);
    }

    @Override
    public Identifier getTexture(LittleMaidEntity entity) {
        return entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }

}

package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.maidmodel.ModelLittleMaidBase;
import net.sistr.littlemaidmodelloader.maidmodel.ModelRenderer;

/**
 * LM専用に拡張
 */
public class LMMultiModel<T extends LivingEntity & IHasMultiModel> extends MultiModel<T> implements ModelWithHead {
    private T entity;
    private final ModelPart modelPart = new ModelPart(ImmutableList.of(), ImmutableMap.of());

    @Override
    public void animateModel(T entity, float limbAngle, float limbDistance, float tickDelta) {
        this.entity = entity;
        super.animateModel(entity, limbAngle, limbDistance, tickDelta);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.entity = entity;
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
    }

    @Override
    public ModelPart getHead() {
        this.entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD)
                .filter(model -> model instanceof ModelLittleMaidBase)
                .map(model -> (ModelLittleMaidBase) model)
                .ifPresent(model -> {
                    modelPart.pivotX = 0;
                    modelPart.pivotY = 0;
                    modelPart.pivotZ = 0;
                    modelPart.roll = 0;
                    modelPart.yaw = 0;
                    modelPart.pitch = 0;
                    ModelRenderer modelRenderer;
                    ItemStack stack = this.entity.getEquippedStack(EquipmentSlot.HEAD);
                    //todo このコード実装変じゃない？確認
                    if (this.entity instanceof MobEntity mob
                            && mob.getPreferredEquipmentSlot(stack) == EquipmentSlot.HEAD) {
                        modelRenderer = model.bipedHead;
                    } else {
                        modelRenderer = model.bipedHead;
                    }
                    while (modelRenderer != null) {
                        modelPart.pivotX += (modelRenderer.rotationPointX + modelRenderer.offsetX * 16.0f) * 0.9375F;
                        modelPart.pivotY += (modelRenderer.rotationPointY + modelRenderer.offsetY * 16.0f) * 0.9375F;
                        modelPart.pivotZ += (modelRenderer.rotationPointZ + modelRenderer.offsetZ * 16.0f) * 0.9375F;
                        modelPart.roll += modelRenderer.rotateAngleZ;
                        modelPart.yaw += modelRenderer.rotateAngleY;
                        modelPart.pitch += modelRenderer.rotateAngleX;
                        modelRenderer = modelRenderer.pearent;
                    }
                });
        return modelPart;
    }
}

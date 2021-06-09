package net.sistr.littlemaidrebirth.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.sistr.littlemaidmodelloader.client.renderer.MultiModelRenderer;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.maidmodel.ModelMultiBase;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.Tameable;

@Environment(EnvType.CLIENT)
public class MaidModelRenderer extends MultiModelRenderer<LittleMaidEntity> {

    public MaidModelRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void syncCaps(LittleMaidEntity entity, ModelMultiBase model, float partialTicks) {
        super.syncCaps(entity, model, partialTicks);
        model.setCapsValue(IModelCaps.caps_aimedBow, entity.isAimingBow());
        model.setCapsValue(IModelCaps.caps_isWait, entity.getMovingState() == Tameable.MovingState.WAIT);
        model.setCapsValue(IModelCaps.caps_isContract, entity.hasTameOwner());
    }

}

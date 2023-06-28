package net.sistr.littlemaidrebirth.client;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;

public class MaidSoulRenderer extends EntityRenderer<MaidSoulEntity> {
    private static final Identifier TEXTURE = new Identifier(LMRBMod.MODID, "textures/entity/maid_soul/maid_soul.png");

    public MaidSoulRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(MaidSoulEntity entity) {
        return TEXTURE;
    }
}

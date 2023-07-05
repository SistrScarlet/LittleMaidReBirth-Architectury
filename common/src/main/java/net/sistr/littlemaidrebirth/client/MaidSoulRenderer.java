package net.sistr.littlemaidrebirth.client;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MaidSoulRenderer extends EntityRenderer<MaidSoulEntity> {
    private static final Identifier TEXTURE = new Identifier(LMRBMod.MODID, "textures/entity/maid_soul/maid_soul.png");
    private static final Identifier HEART = new Identifier("textures/particle/heart.png");

    public MaidSoulRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MaidSoulEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        float progress = (entity.age + tickDelta) % 40 / 40;
        float radius = 0.25f;
        float cos = MathHelper.cos(progress * MathHelper.PI * 2) * radius;
        float sin = MathHelper.sin(progress * MathHelper.PI * 2) * radius;
        float x = 0;
        float z = 0;
        float y = 0;
        float x1 = x + cos;
        float x2 = x - cos;
        float z1 = z + sin;
        float z2 = z - sin;
        float y1 = y + radius;
        float y2 = y - radius;
        var consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(HEART));
        var entry = matrices.peek();
        var posMatrix = entry.getPositionMatrix();
        var normMatrix = entry.getNormalMatrix();
        //反時計回りが表
        //表を見て、右上、左上、左下、右下の順
        vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 1.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 0.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 0.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 1.0f, 1.0f);
        //裏、左右反転、右上、右下、左下、左上
        vertex(posMatrix, normMatrix, consumer, x1, y1, z1, 0.0f, 0.0f);
        vertex(posMatrix, normMatrix, consumer, x1, y2, z1, 0.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y2, z2, 1.0f, 1.0f);
        vertex(posMatrix, normMatrix, consumer, x2, y1, z2, 1.0f, 0.0f);
    }

    public void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertexConsumer,
                       float x, float y, float z, float u, float v) {
        vertexConsumer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE)
                .normal(normalMatrix, 0, 0, 1)
                .next();
    }

    @Override
    public Identifier getTexture(MaidSoulEntity entity) {
        return TEXTURE;
    }
}

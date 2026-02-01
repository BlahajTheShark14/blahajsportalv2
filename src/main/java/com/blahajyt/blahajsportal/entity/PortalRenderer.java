package com.blahajyt.blahajsportal.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class PortalRenderer extends EntityRenderer<PortalEntity> {
    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PortalEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Forgatás a fal iránya alapján
        float rotation = entity.getFacing().toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));

        VertexConsumer builder = buffer.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = poseStack.last().pose();

        int r = (entity.getPortalColor() == 0) ? 0 : 255;
        int g = (entity.getPortalColor() == 0) ? 150 : 100;
        int b = (entity.getPortalColor() == 0) ? 255 : 0;

        // 1x2-es téglalap rajzolása
        builder.vertex(matrix, -0.5f, 0, 0.01f).color(r, g, b, 255).endVertex();
        builder.vertex(matrix, 0.5f, 0, 0.01f).color(r, g, b, 255).endVertex();
        builder.vertex(matrix, 0.5f, 2.0f, 0.01f).color(r, g, b, 255).endVertex();
        builder.vertex(matrix, -0.5f, 2.0f, 0.01f).color(r, g, b, 255).endVertex();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return null;
    }
}
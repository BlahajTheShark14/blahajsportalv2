package com.blahajyt.blahajsportal.client.renderer;

import com.blahajyt.blahajsportal.entity.PortalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class PortalRenderer extends EntityRenderer<PortalEntity> {

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PortalEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 1. Pozícionálás és forgatás a fal síkjára
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getFacing().toYRot()));

        // Z-fighting elleni védelem (0.0125 egy jó középút az eltoláshoz is)
        poseStack.translate(0, 0, 0.0125);

        // 2. Színek (Élénk Portal-kék és Portal-narancs)
        int r, g, b;
        if (entity.getPortalColor() == 0) { // KÉK
            r = 0; g = 120; b = 255;
        } else { // NARANCS
            r = 255; g = 140; b = 0;
        }

        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        Matrix4f matrix = poseStack.last().pose();

        // 3. Renderelési logika a peremvastagsággal
        if (entity.getPair() == null) {
            // NINCS PÁRJA: Teli felület
            drawQuad(builder, matrix, -0.45f, 0.45f, 0.0f, 2.0f, r, g, b, 180);
        } else {
            // VAN PÁRJA: Optimalizált peremvastagság (0.1)
            float v = 0.1f;

            // Alsó keret
            drawQuad(builder, matrix, -0.45f, 0.45f, 0.0f, v, r, g, b, 255);
            // Felső keret
            drawQuad(builder, matrix, -0.45f, 0.45f, 2.0f - v, 2.0f, r, g, b, 255);
            // Bal oldali keret
            drawQuad(builder, matrix, -0.45f, -0.45f + v, v, 2.0f - v, r, g, b, 255);
            // Jobb oldali keret
            drawQuad(builder, matrix, 0.45f - v, 0.45f, v, 2.0f - v, r, g, b, 255);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void drawQuad(VertexConsumer builder, Matrix4f matrix, float minX, float maxX, float minY, float maxY, int r, int g, int b, int a) {
        builder.vertex(matrix, minX, minY, 0).color(r, g, b, a).uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, maxX, minY, 0).color(r, g, b, a).uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, maxX, maxY, 0).color(r, g, b, a).uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
        builder.vertex(matrix, minX, maxY, 0).color(r, g, b, a).uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.jayemceekay.shadowedhearts.registry.ModItems;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DowsingMachineRendererImpl extends AbstractModeRenderer {

    private static ItemStack directionArrowStack;

    private static ItemStack getDirectionArrowStack() {
        if (directionArrowStack == null) {
            directionArrowStack = new ItemStack(ModItems.DIRECTION_ARROW.get());
        }
        return directionArrowStack;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int width, int height, float alpha, float time, float partialTick) {
        super.render(guiGraphics, width, height, alpha, time, partialTick);
        renderDowsingArrow(guiGraphics, width / 2, height / 2, alpha, partialTick);
    }

    private void renderDowsingArrow(GuiGraphics guiGraphics, int centerX, int centerY, float alpha, float partialTick) {
        if (DowsingMachineLogic.dowsingTargetPos == null) return;

        Minecraft mc = Minecraft.getInstance();
        BlockPos target = DowsingMachineLogic.dowsingTargetPos;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition().add(mc.player.getLookAngle().normalize().scale(1.25f));

        Vector3f dirWorld = new Vector3f(
                (float) (target.getX() + 0.5 - camPos.x),
                (float) (target.getY() + 0.5 - camPos.y),
                (float) (target.getZ() + 0.5 - camPos.z)
        );

        float dist = dirWorld.length();
        dirWorld.normalize();

        Vector3f forwardVec = cam.getLookVector();
        Vector3f upVec = cam.getUpVector();

        Vector3f forward = forwardVec.normalize();
        Vector3f camUp = upVec.normalize();
        Vector3f camRight = cam.getLeftVector().normalize();

        float x = dirWorld.dot(camRight);
        float y = dirWorld.dot(camUp);
        float z = dirWorld.dot(forward);

        Vector3f dirCamera = new Vector3f(x, y, z).normalize();

        Quaternionf rotation = new Quaternionf()
                .rotationTo(dirCamera, new Vector3f(0, 1, 0));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 100);
        guiGraphics.pose().scale(25, -25, 25);
        guiGraphics.pose().mulPose(rotation);

        ItemStack stack = getDirectionArrowStack();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);

        Lighting.setupForFlatItems();
        mc.getItemRenderer().render(
                stack,
                ItemDisplayContext.GUI,
                false,
                guiGraphics.pose(),
                guiGraphics.bufferSource(),
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                model
        );
        guiGraphics.flush();
        Lighting.setupFor3DItems();

        guiGraphics.pose().popPose();

        String distText = (int) dist + "m";
        int alphaBits = (int) (alpha * 255) << 24;

        guiGraphics.drawCenteredString(mc.font, distText, centerX, centerY + 20, 0x00FFFF | alphaBits);

        String materialName = DowsingMachineLogic.DOWSING_MATERIALS
                .get(DowsingMachineLogic.selectedDowsingMaterialIndex)
                .getName()
                .getString();

        guiGraphics.drawCenteredString(mc.font, materialName, centerX, centerY - 124, 0x00FFFF | alphaBits);
    }
}

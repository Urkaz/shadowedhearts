package com.jayemceekay.shadowedhearts.client.gui;

import com.jayemceekay.shadowedhearts.menu.AuraReaderUpgradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class AuraReaderUpgradeScreen extends AbstractContainerScreen<AuraReaderUpgradeMenu> {

    public AuraReaderUpgradeScreen(AuraReaderUpgradeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 132;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Minimal frame: draw a translucent dark background
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int bgColor = 0x88000000; // semi-transparent black
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, bgColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        for(Slot slot : this.menu.slots) {
            guiGraphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF000000);
        }
    }
}

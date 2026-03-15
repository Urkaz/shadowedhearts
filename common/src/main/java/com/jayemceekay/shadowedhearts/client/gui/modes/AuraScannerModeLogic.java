package com.jayemceekay.shadowedhearts.client.gui.modes;

import net.minecraft.client.Minecraft;

public interface AuraScannerModeLogic {
    void tick(Minecraft mc);
    void onActivate(Minecraft mc);
    void onDeactivate(Minecraft mc);
    boolean handleInput(Minecraft mc);
}

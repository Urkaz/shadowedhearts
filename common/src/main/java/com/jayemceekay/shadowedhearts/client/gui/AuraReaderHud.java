package com.jayemceekay.shadowedhearts.client.gui;

import com.jayemceekay.fluxui.hud.core.HudManager;
import com.jayemceekay.fluxui.hud.core.HudNode;

public class AuraReaderHud {
    private static HudNode root;
    private static boolean initialized;

    public static void init() {
        if (!initialized) {
            HudManager.init();
            initialized = true;
        }
        rebuild();
    }

    public static void rebuild() {
        root = AuraScannerHudFactory.create(AuraReaderManager.HUD_STATE, HudManager.animator());
        HudManager.setRoot(root);
    }
}

package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.jayemceekay.shadowedhearts.client.gui.AuraReaderManager;
import net.minecraft.client.Minecraft;

public class PokedexScannerLogic extends AbstractModeLogic {

    @Override
    public void tick(Minecraft mc) {
        pokedexTicksInUse++;
        AuraReaderManager.POKEDEX_USAGE_CONTEXT.useTick(mc.player, pokedexTicksInUse,true);
    }

    @Override
    public void onActivate(Minecraft mc) {}

    @Override
    public void onDeactivate(Minecraft mc) {}

    @Override
    public boolean handleInput(Minecraft mc) {
        return false;
    }
}

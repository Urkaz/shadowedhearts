// common/src/main/java/com/jayemceekay/shadowedhearts/core/ModMenuTypes.java
package com.jayemceekay.shadowedhearts.registry;

import com.jayemceekay.shadowedhearts.Shadowedhearts;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Cross-platform MenuType registry (Architectury). */
public final class ModMenuTypes {
    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Shadowedhearts.MOD_ID, Registries.MENU);

    // Aura Reader Upgrades menu
    public static final RegistrySupplier<MenuType<com.jayemceekay.shadowedhearts.menu.AuraReaderUpgradeMenu>> AURA_READER_UPGRADES =
            MENUS.register("aura_reader_upgrades",
                    () -> new MenuType<>(com.jayemceekay.shadowedhearts.menu.AuraReaderUpgradeMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    /** Call once during common setup on both loaders. */
    public static void init() {
        MENUS.register();
    }

}

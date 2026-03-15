package com.jayemceekay.shadowedhearts.content.items;

import com.jayemceekay.shadowedhearts.content.upgrades.IAuraUpgrade;
import com.jayemceekay.shadowedhearts.content.upgrades.UpgradeSlotType;
import net.minecraft.world.item.Item;

public class PokedexIntegratorItem extends Item implements IAuraUpgrade {
    public PokedexIntegratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public UpgradeSlotType slotType() {
        return UpgradeSlotType.PROCESSOR;
    }
}

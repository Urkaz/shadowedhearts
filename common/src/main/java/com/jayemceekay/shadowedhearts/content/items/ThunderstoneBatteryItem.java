package com.jayemceekay.shadowedhearts.content.items;

import com.jayemceekay.shadowedhearts.content.upgrades.IAuraUpgrade;
import com.jayemceekay.shadowedhearts.content.upgrades.UpgradeSlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Example upgrade item: Thunderstone Battery (fits BATTERY slot). */
public class ThunderstoneBatteryItem extends Item implements IAuraUpgrade {
    public ThunderstoneBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public UpgradeSlotType slotType() {
        return UpgradeSlotType.BATTERY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable("item.shadowedhearts.thunderstone_battery.tooltip"));
        super.appendHoverText(stack, context, tooltip, type);
    }
}

package com.jayemceekay.shadowedhearts.common.thermal.conditionals;

import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.common.thermal.ThermalSourceRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Adds heat when the player is holding a heat-source item (e.g., a torch) in either hand.
 * Uses ThermalSourceRegistry's block definitions when the held item is a BlockItem.
 */
public class HoldingHeatSourceConditional implements OperationalTempConditional {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("shadowedhearts", "holding_heat_source");

    public static void register() { OperationalTempRegistry.register(ID, new HoldingHeatSourceConditional()); }

    @Override
    public ResourceLocation id() { return ID; }

    @Override
    public int priority() {
        // Run after fluids/biome but before light; similar category to nearby sources
        return 420;
    }

    @Override
    public float computeDeltaC(Level level, Player player, ItemStack auraReader, float currentTempC) {
        if (level == null || player == null) return 0.0f;

        float delta = 0.0f;

        // Offhand: full effect (common use-case: holding a torch)
        delta += heldItemContribution(player.getOffhandItem(), 1.0f);
        // Mainhand: slightly reduced effect to avoid stacking too strongly
        delta += heldItemContribution(player.getMainHandItem(), 0.75f);

        return delta;
    }

    private float heldItemContribution(ItemStack stack, float scale) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        Item item = stack.getItem();

        // Special-case a few obvious items that aren't BlockItems
        if (item == Items.LAVA_BUCKET) return 2.0f * scale;
        if (item == Items.WATER_BUCKET) return -0.6f * scale;

        if (item instanceof BlockItem bi) {
            var def = ThermalSourceRegistry.blockSources().get(bi.getBlock());
            if (def != null) {
                // Treat as held at ~1m effective distance to avoid extreme spikes.
                // Convert additive °C at contact into per-tick delta using the assumed proportionality K.
                final float K_ASSUMED_PER_TICK = 0.03f;
                return def.contactAdditiveC * scale * K_ASSUMED_PER_TICK;
            }
        }
        return 0.0f;
    }
}

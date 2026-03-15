package com.jayemceekay.shadowedhearts.content.items;

import com.jayemceekay.shadowedhearts.common.aura.AuraReaderCharge;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempConditional;
import com.jayemceekay.shadowedhearts.common.thermal.OperationalTempRegistry;
import com.jayemceekay.shadowedhearts.content.upgrades.IAuraUpgrade;
import com.jayemceekay.shadowedhearts.content.upgrades.UpgradeSlotType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AuraReaderItem extends Item implements Equipable {
    public static final int MAX_CHARGE = 12000; // 10 minutes at 20/sec? Or maybe less. Let's say 12000 ticks = 10 mins.
    // Frame-rate independent smoothing toward inferred equilibrium, with different rates for heating vs cooling
    private static final float HEAT_RATE_PER_SEC = 0.18f; // warms toward target faster
    private static final float COOL_RATE_PER_SEC = 0.10f; // cools toward target slower (unless active cooling upgrades later)
    private static final float TICK_DT_SECONDS = 1.0f / 20.0f; // server tick time
    // Assumed per-tick proportionality used by conditionals: delta ≈ (T_eq - current) * K
    // We invert this to infer an approximate equilibrium target from the summed per-tick delta.
    private static final float K_ASSUMED_PER_TICK = 0.03f; // typical 0.01..0.05 in built-ins; choose mid-range
    // ==== Custom Data keys ====
    private static final String STATE_ROOT = "ReaderState";
    private static final String TEMP_KEY = "OperationalTemp"; // legacy normalized [-1..1]
    private static final String TEMP_C_KEY = "OperationalTempC"; // new Celsius scale

    // Base operating range in Celsius (upgrade hooks can alter these dynamically per-stack)
    public static final float DEFAULT_MIN_TEMP_C = -40.0f;
    public static final float DEFAULT_MAX_TEMP_C = 110.0f;
    public static final float SAFE_MAX_TEMP_C = 75.0f;
    public static final float SAFE_MIN_TEMP_C = 0.0f;

    public AuraReaderItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            AuraReaderCharge.ensureInitialized(stack, MAX_CHARGE);
            // Server-authoritative operational temperature updates and persistence
            if (entity instanceof Player player) {
                try {
                    float current = getOperationalTempC(stack);
                    float sumDelta = computeTempDeltaC(level, player, stack, current);
                    float minC = getMinOperatingTempC(stack);
                    float maxC = getMaxOperatingTempC(stack);
                    // Infer an approximate equilibrium temperature from the net delta using the assumed K
                    float inferredTarget = current + (sumDelta / K_ASSUMED_PER_TICK);
                    inferredTarget = Mth.clamp(inferredTarget, minC, maxC);

                    // Exponential smoothing toward target with different rates for heating vs cooling
                    float rate = (inferredTarget > current) ? HEAT_RATE_PER_SEC : COOL_RATE_PER_SEC;
                    float alpha = 1.0f - (float) Math.exp(-rate * TICK_DT_SECONDS);
                    float updated = Mth.lerp(alpha, current, inferredTarget);
                    updated = Mth.clamp(updated, minC, maxC);
                    setOperationalTempC(stack, updated);
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Require SHIFT + right-click (from main hand) to equip; plain right-click opens the upgrades GUI
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            boolean mainHand = hand == InteractionHand.MAIN_HAND;
            if (player.isShiftKeyDown() && mainHand) {
                // Attempt to equip to the vanilla HEAD slot (Accessories integration will also respect this)
                var slot = getEquipmentSlot();
                ItemStack currentlyEquipped = player.getItemBySlot(slot);
                if (currentlyEquipped.isEmpty()) {
                    // Move one item into the head slot
                    player.setItemSlot(slot, stack.copyWithCount(1));
                    stack.shrink(1);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                } else {
                    // Already wearing something in head; fail to prevent accidental swaps
                    return InteractionResultHolder.fail(stack);
                }
            } else {
                // Open the Aura Reader Upgrades screen (server-authoritative menu)
                MenuProvider provider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("screen.shadowedhearts.aura_reader_upgrades");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                        return new com.jayemceekay.shadowedhearts.menu.AuraReaderUpgradeMenu(containerId, inv);
                    }
                };
                sp.openMenu(provider);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        int charge = AuraReaderCharge.get(stack);
        tooltip.add(Component.translatable("item.shadowedhearts.aura_reader.tooltip"));
        tooltip.add(Component.translatable("tooltip.shadowedhearts.aura_reader.charge", charge, MAX_CHARGE));
        // List installed upgrades from CustomData
        List<ItemStack> upgrades = readUpgrades(stack, context != null ? context.registries() : null);
        if (!upgrades.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.shadowedhearts.aura_reader.upgrades"));
            for (ItemStack up : upgrades) {
                if (up.isEmpty()) continue;
                Component name = up.getHoverName();
                String slot = "";
                if (up.getItem() instanceof IAuraUpgrade upg) {
                    UpgradeSlotType st = upg.slotType();
                    slot = st != null ? (" [" + st.name().toLowerCase() + "]") : "";
                }
                tooltip.add(Component.literal(" - ").append(name).append(slot));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.shadowedhearts.aura_reader.no_upgrades"));
        }
        super.appendHoverText(stack, context, tooltip, type);
    }

    /**
     * Reads the operational temperature [-1.0..1.0] stored on this ItemStack via DataComponents.CUSTOM_DATA.
     * Returns 0.0f if absent.
     */
    public static float getOperationalTemp(ItemStack stack) {
        // Kept for backward compatibility. Prefer getOperationalTempC.
        if (stack == null || stack.isEmpty()) return 0.0f;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0.0f;
        CompoundTag root = data.copyTag();
        if (root == null) return 0.0f;
        CompoundTag state = root.getCompound(STATE_ROOT);
        if (!state.contains(TEMP_KEY)) return 0.0f;
        return Mth.clamp(state.getFloat(TEMP_KEY), -1.0f, 1.0f);
    }

    /**
     * Writes the operational temperature [-1.0..1.0] to this ItemStack via DataComponents.CUSTOM_DATA.
     * Existing custom data (like upgrades) is preserved.
     */
    public static void setOperationalTemp(ItemStack stack, float value) {
        // Legacy writer; prefer setOperationalTempC. Still writes legacy key for mods expecting it.
        if (stack == null || stack.isEmpty()) return;
        float v = Mth.clamp(value, -1.0f, 1.0f);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = (data != null ? data.copyTag() : new CompoundTag());
        CompoundTag state = root.getCompound(STATE_ROOT);
        state.putFloat(TEMP_KEY, v);
        root.put(STATE_ROOT, state);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    // ==== New Celsius-scale API ====

    /**
     * Reads the operational temperature in Celsius. Falls back to converting the legacy normalized value if needed.
     */
    public static float getOperationalTempC(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag root = data.copyTag();
            if (root != null) {
                CompoundTag state = root.getCompound(STATE_ROOT);
                if (state.contains(TEMP_C_KEY)) {
                    return state.getFloat(TEMP_C_KEY);
                }
                // Fallback from legacy normalized scale [-1..1] → [-40..110]
                if (state.contains(TEMP_KEY)) {
                    float norm = Mth.clamp(state.getFloat(TEMP_KEY), -1.0f, 1.0f);
                    return lerpCelsiusFromNormalized(norm);
                }
            }
        }
        return 0.0f;
    }

    /**
     * Writes the operational temperature in Celsius to the ItemStack.
     * Also updates the legacy normalized key for compatibility with older readers.
     */
    public static void setOperationalTempC(ItemStack stack, float valueC) {
        if (stack == null || stack.isEmpty()) return;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = (data != null ? data.copyTag() : new CompoundTag());
        CompoundTag state = root.getCompound(STATE_ROOT);
        state.putFloat(TEMP_C_KEY, valueC);
        // Maintain legacy normalized representation too (approximate mapping)
        float normalized = mapCelsiusToNormalized(valueC);
        state.putFloat(TEMP_KEY, normalized);
        root.put(STATE_ROOT, state);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /**
     * Smooth heat instability severity 0..1 based on safe and absolute bounds.
     * 0 at/below SAFE_MAX, 1 at/above absolute MAX.
     */
    public static float getHeatSeverity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        float temp = getOperationalTempC(stack);
        float safeMax = SAFE_MAX_TEMP_C;
        float absMax = Math.max(safeMax + 0.001f, getMaxOperatingTempC(stack));
        float num = Math.max(0.0f, temp - safeMax);
        float den = Math.max(0.001f, absMax - safeMax);
        return Mth.clamp(num / den, 0.0f, 1.0f);
    }

    /**
     * Smooth cold instability severity 0..1 based on safe and absolute bounds.
     * 0 at/above SAFE_MIN, 1 at/below absolute MIN.
     */
    public static float getColdSeverity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        float temp = getOperationalTempC(stack);
        float safeMin = SAFE_MIN_TEMP_C;
        float absMin = Math.min(safeMin - 0.001f, getMinOperatingTempC(stack));
        float num = Math.max(0.0f, safeMin - temp);
        float den = Math.max(0.001f, safeMin - absMin);
        return Mth.clamp(num / den, 0.0f, 1.0f);
    }

    /**
     * Overall instability severity as the max of cold and heat severities.
     */
    public static float getInstabilitySeverity(ItemStack stack) {
        return Math.max(getHeatSeverity(stack), getColdSeverity(stack));
    }

    private static float lerpCelsiusFromNormalized(float norm) {
        // Map -1..1 to -40..110 linearly
        float t = (norm + 1.0f) * 0.5f; // 0..1
        return -40.0f + t * (110.0f - (-40.0f));
    }

    private static float mapCelsiusToNormalized(float c) {
        float t = (c - (-40.0f)) / (110.0f - (-40.0f)); // 0..1
        return Mth.clamp(t * 2.0f - 1.0f, -1.0f, 1.0f);
    }

    /**
     * Base minimum operating temperature for this stack in Celsius.
     * Future: include installed upgrades here.
     */
    public static float getMinOperatingTempC(ItemStack stack) {
        return DEFAULT_MIN_TEMP_C;
    }

    /**
     * Base maximum operating temperature for this stack in Celsius.
     * Future: include installed upgrades here.
     */
    public static float getMaxOperatingTempC(ItemStack stack) {
        return DEFAULT_MAX_TEMP_C;
    }

    // ==== Server-side delta computation ====
    private static float computeTempDeltaC(Level level, Player player, ItemStack stack, float currentTempC) {
        float sum = 0.0f;
        for (OperationalTempConditional c : OperationalTempRegistry.all()) {
            try {
                sum += c.computeDeltaC(level, player, stack, currentTempC);
            } catch (Throwable ignored) {}
        }
        // Safety clamp to avoid absurd per-tick spikes from custom conditionals
        return Mth.clamp(sum, -0.5f, 0.5f);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float) AuraReaderCharge.get(stack) / (float) MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF; // Cyan
    }

    // ==== Persistence helpers (mirror AuraReaderUpgradeMenu) ====
    private static final String UPGRADE_ROOT = "ReaderUpgrades";
    private static final String UPGRADE_KEY_PREFIX = "U";

    public static List<ItemStack> readUpgrades(ItemStack reader, HolderLookup.Provider registries) {
        List<ItemStack> result = new ArrayList<>();
        if (reader.isEmpty()) return result;
        CustomData data = reader.get(DataComponents.CUSTOM_DATA);
        if (data == null) return result;
        CompoundTag root = data.copyTag();
        CompoundTag upg = root.getCompound(UPGRADE_ROOT);
        for (int i = 0; i < 6; i++) {
            String key = UPGRADE_KEY_PREFIX + i;
            if (upg.contains(key)) {
                Tag itemTag = upg.get(key);
                if (registries != null && itemTag instanceof CompoundTag ct) {
                    ItemStack.parse(registries, ct).ifPresent(result::add);
                }
            }
        }
        return result;
    }
}

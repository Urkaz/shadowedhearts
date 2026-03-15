package com.jayemceekay.shadowedhearts.client.gui.modes;

import com.jayemceekay.shadowedhearts.client.ModKeybinds;
import com.jayemceekay.shadowedhearts.client.aura.AuraPulseRenderer;
import com.jayemceekay.shadowedhearts.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.List;

public class DowsingMachineLogic extends AbstractModeLogic {

    /** List of blocks detectable by the Dowsing Machine mode. */
    public static final List<net.minecraft.world.level.block.Block> DOWSING_MATERIALS = Arrays.asList(
            net.minecraft.world.level.block.Blocks.COAL_ORE,
            net.minecraft.world.level.block.Blocks.IRON_ORE,
            net.minecraft.world.level.block.Blocks.GOLD_ORE,
            net.minecraft.world.level.block.Blocks.DIAMOND_ORE,
            net.minecraft.world.level.block.Blocks.REDSTONE_ORE,
            net.minecraft.world.level.block.Blocks.LAPIS_ORE,
            net.minecraft.world.level.block.Blocks.EMERALD_ORE,
            net.minecraft.world.level.block.Blocks.COPPER_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE
    );

    /** Index of the currently selected material in DOWSING_MATERIALS. */
    public static int selectedDowsingMaterialIndex = 0;
    /** Current position of a block detected by the Dowsing Machine. */
    public static BlockPos dowsingTargetPos = null;

    @Override
    public void tick(Minecraft mc) {
        if (dowsingTargetPos != null) {
            double dist = Math.sqrt(dowsingTargetPos.distSqr(mc.player.blockPosition()));
            int range = 32; // same as pulse range
            maxIntensity = Math.max(maxIntensity, (float) Math.max(0, 1.0 - (dist / range)));
        }
    }

    @Override
    public void onActivate(Minecraft mc) {
    }

    @Override
    public void onDeactivate(Minecraft mc) {
        dowsingTargetPos = null;
    }

    @Override
    public boolean handleInput(Minecraft mc) {
        if (ModKeybinds.consumeAuraPulsePress()) {
            if (pulseCooldown <= 0) {
                doDowsingPulse(mc);
                pulseCooldown = PULSE_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private void doDowsingPulse(Minecraft mc) {
        net.minecraft.world.level.block.Block targetBlock = DOWSING_MATERIALS.get(selectedDowsingMaterialIndex);
        BlockPos playerPos = mc.player.blockPosition();
        int range = 32;
        BlockPos closest = null;
        double minDistSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-range, -range, -range), playerPos.offset(range, range, range))) {
            if (mc.level.getBlockState(pos).is(targetBlock)) {
                double distSq = pos.distSqr(playerPos);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    closest = pos.immutable();
                }
            }
        }

        if (closest != null) {
            dowsingTargetPos = closest;
            mc.player.playSound(ModSounds.AURA_SCANNER_BEEP.get(), 1.0f, 1.5f);
            AuraPulseRenderer.spawnPulse(mc.player.position(), 0.0f, 1.0f, 1.0f, range); // Cyan pulse
        } else {
            dowsingTargetPos = null;
            mc.player.playSound(ModSounds.AURA_READER_UNEQUIP.get(), 1.0f, 0.5f);
        }
    }
}

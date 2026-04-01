package com.jayemceekay.shadowedhearts.config;

import com.jayemceekay.shadowedhearts.Shadowedhearts;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Snag Machine configuration using ModConfigSpec.
 */
public final class SnagConfig implements ISnagConfig {
    private boolean loaded = false;
    public static final ModConfigSpec SPEC;
    private static final Data DATA = new Data();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DATA.build(builder);
        SPEC = builder.build();
    }

    @Override
    public int energyPerAttempt() {
        if (!isLoaded()) return ISnagConfig.super.energyPerAttempt();
        return DATA.energyPerAttempt.get();
    }

    @Override
    public int toggleCooldownTicks() {
        if (!isLoaded()) return ISnagConfig.super.toggleCooldownTicks();
        return DATA.toggleCooldownTicks.get();
    }

    @Override
    public boolean rechargeOnVictory() {
        if (!isLoaded()) return ISnagConfig.super.rechargeOnVictory();
        return DATA.rechargeOnVictory.get();
    }

    @Override
    public boolean rechargeInPvp() {
        if (!isLoaded()) return ISnagConfig.super.rechargeInPvp();
        return DATA.rechargeInPvp.get();
    }

    @Override
    public int rechargeBase() {
        if (!isLoaded()) return ISnagConfig.super.rechargeBase();
        return DATA.rechargeBase.get();
    }

    @Override
    public double rechargePerLevel() {
        if (!isLoaded()) return ISnagConfig.super.rechargePerLevel();
        return DATA.rechargePerLevel.get();
    }

    @Override
    public int rechargePerNpc() {
        if (!isLoaded()) return ISnagConfig.super.rechargePerNpc();
        return DATA.rechargePerNpc.get();
    }

    @Override
    public int rechargeMin() {
        if (!isLoaded()) return ISnagConfig.super.rechargeMin();
        return DATA.rechargeMin.get();
    }

    @Override
    public int rechargeMax() {
        if (!isLoaded()) return ISnagConfig.super.rechargeMax();
        return DATA.rechargeMax.get();
    }

    @Override
    public boolean auraReaderRechargeOnVictory() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargeOnVictory();
        return DATA.auraReaderRechargeOnVictory.get();
    }

    @Override
    public boolean auraReaderRechargeInPvp() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargeInPvp();
        return DATA.auraReaderRechargeInPvp.get();
    }

    @Override
    public int auraReaderRechargeBase() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargeBase();
        return DATA.auraReaderRechargeBase.get();
    }

    @Override
    public double auraReaderRechargePerLevel() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargePerLevel();
        return DATA.auraReaderRechargePerLevel.get();
    }

    @Override
    public int auraReaderRechargePerNpc() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargePerNpc();
        return DATA.auraReaderRechargePerNpc.get();
    }

    @Override
    public int auraReaderRechargeMin() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargeMin();
        return DATA.auraReaderRechargeMin.get();
    }

    @Override
    public int auraReaderRechargeMax() {
        if (!isLoaded()) return ISnagConfig.super.auraReaderRechargeMax();
        return DATA.auraReaderRechargeMax.get();
    }

    @Override
    public int prototypeCapacity() {
        if (!isLoaded()) return ISnagConfig.super.prototypeCapacity();
        return DATA.prototypeCapacity.get();
    }

    @Override
    public int advancedCapacity() {
        if (!isLoaded()) return ISnagConfig.super.advancedCapacity();
        return DATA.advancedCapacity.get();
    }

    @Override
    public boolean failStackingBonus() {
        if (!isLoaded()) return ISnagConfig.super.failStackingBonus();
        return DATA.snaggingPity.failStackingBonus.get();
    }

    @Override
    public double failBonusPerAttempt() {
        if (!isLoaded()) return ISnagConfig.super.failBonusPerAttempt();
        return DATA.snaggingPity.failBonusPerAttempt.get();
    }

    @Override
    public double maxFailBonus() {
        if (!isLoaded()) return ISnagConfig.super.maxFailBonus();
        return DATA.snaggingPity.maxFailBonus.get();
    }

    @Override
    public ModConfigSpec getSpec() {
        return SPEC;
    }

    private static final class Data {
        public ModConfigSpec.IntValue energyPerAttempt;
        public ModConfigSpec.IntValue toggleCooldownTicks;

        public ModConfigSpec.IntValue prototypeCapacity;
        public ModConfigSpec.IntValue advancedCapacity;

        public ModConfigSpec.BooleanValue rechargeOnVictory;
        public ModConfigSpec.BooleanValue rechargeInPvp;
        public ModConfigSpec.IntValue rechargeBase;
        public ModConfigSpec.DoubleValue rechargePerLevel;
        public ModConfigSpec.IntValue rechargePerNpc;
        public ModConfigSpec.IntValue rechargeMin;
        public ModConfigSpec.IntValue rechargeMax;

        public ModConfigSpec.BooleanValue auraReaderRechargeOnVictory;
        public ModConfigSpec.BooleanValue auraReaderRechargeInPvp;
        public ModConfigSpec.IntValue auraReaderRechargeBase;
        public ModConfigSpec.DoubleValue auraReaderRechargePerLevel;
        public ModConfigSpec.IntValue auraReaderRechargePerNpc;
        public ModConfigSpec.IntValue auraReaderRechargeMin;
        public ModConfigSpec.IntValue auraReaderRechargeMax;

        public final SnaggingPityConfig snaggingPity = new SnaggingPityConfig();

        private void build(ModConfigSpec.Builder builder) {
            energyPerAttempt = builder
                    .comment("The amount of energy consumed by the Snag Machine for each snag attempt.")
                    .defineInRange("energy_per_attempt", 50, 0, 1000);
            
            toggleCooldownTicks = builder
                    .comment("Cooldown in ticks (20 ticks = 1 second) between toggling the Snag Machine on or off.")
                    .defineInRange("toggle_cooldown_ticks", 20, 0, 1200);

            builder.push("prototype_snag_machine");
            prototypeCapacity = builder
                    .comment("The energy capacity of the Prototype Snag Machine.")
                    .defineInRange("capacity", 50, 1, 10000);
            builder.pop();

            builder.push("advanced_snag_machine");
            advancedCapacity = builder
                    .comment("The energy capacity of the Advanced Snag Machine.")
                    .defineInRange("capacity", 100, 1, 10000);
            builder.pop();
            

            builder.push("recharge");
            rechargeOnVictory = builder
                    .comment("If true, the Snag Machine recharges energy when the player wins a battle.")
                    .define("on_victory", true);
            
            rechargeInPvp = builder
                    .comment("If true, the Snag Machine recharges energy when the player wins a PvP battle.")
                    .define("in_pvp", false);
            
            rechargeBase = builder
                    .comment("The base amount of energy recharged on victory.")
                    .defineInRange("base", 10, 0, 1000);
            
            rechargePerLevel = builder
                    .comment("Additional energy recharged per level of the defeated Pokémon.")
                    .defineInRange("per_level", 0.25, 0.0, 100.0);
            
            rechargePerNpc = builder
                    .comment("Additional energy recharged per Pokémon in the NPC's party.")
                    .defineInRange("per_npc", 3, 0, 100);
            
            rechargeMin = builder
                    .comment("The minimum energy recharged on victory.")
                    .defineInRange("min", 5, 0, 1000);
            
            rechargeMax = builder
                    .comment("The maximum energy recharged on victory.")
                    .defineInRange("max", 15, 0, 1000);
            builder.pop();
            

            builder.push("aura_reader_recharge");
            auraReaderRechargeOnVictory = builder
                    .comment("If true, the Aura Reader recharges energy when the player wins a battle.")
                    .define("on_victory", true);
            
            auraReaderRechargeInPvp = builder
                    .comment("If true, the Aura Reader recharges energy when the player wins a PvP battle.")
                    .define("in_pvp", false);
            
            auraReaderRechargeBase = builder
                    .comment("The base amount of energy recharged for the Aura Reader on victory.")
                    .defineInRange("base", 200, 0, 12000);
            
            auraReaderRechargePerLevel = builder
                    .comment("Additional energy recharged for the Aura Reader per level of the defeated Pokémon.")
                    .defineInRange("per_level", 5.0, 0.0, 1000.0);
            
            auraReaderRechargePerNpc = builder
                    .comment("Additional energy recharged for the Aura Reader per Pokémon in the NPC's party.")
                    .defineInRange("per_npc", 60, 0, 12000);
            
            auraReaderRechargeMin = builder
                    .comment("The minimum energy recharged for the Aura Reader on victory.")
                    .defineInRange("min", 100, 0, 12000);
            
            auraReaderRechargeMax = builder
                    .comment("The maximum energy recharged for the Aura Reader on victory.")
                    .defineInRange("max", 3000, 0, 12000);
            builder.pop();

            builder.push("snagging_pity");
            snaggingPity.build(builder);
            builder.pop();
        }
    }

    public static final class SnaggingPityConfig {
        public ModConfigSpec.BooleanValue failStackingBonus;
        public ModConfigSpec.DoubleValue failBonusPerAttempt;
        public ModConfigSpec.DoubleValue maxFailBonus;

        private void build(ModConfigSpec.Builder builder) {
            failStackingBonus = builder
                    .comment("If true, each failed snag attempt increases the success chance of the next attempt.")
                    .define("failStackingBonus", true);

            failBonusPerAttempt = builder
                    .comment("The bonus added to the catch rate for each failed snag attempt.")
                    .defineInRange("failBonusPerAttempt", 0.05, 0.0, 1.0);

            maxFailBonus = builder
                    .comment("The maximum bonus that can be accumulated from failed snag attempts.")
                    .defineInRange("maxFailBonus", 0.25, 0.0, 1.0);
        }
    }

    @Override
    public void load() {
        loaded = true;
        Shadowedhearts.LOGGER.info("SnagConfig loaded...");
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }
}

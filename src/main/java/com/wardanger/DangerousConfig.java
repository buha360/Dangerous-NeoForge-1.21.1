package com.wardanger;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class DangerousConfig {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ModConfigSpec.DoubleValue baseHealthMultiplier;
        public final ModConfigSpec.DoubleValue maxHealthMultiplier;
        public final ModConfigSpec.DoubleValue healthMultiplierIncrement;
        public final ModConfigSpec.IntValue daysPerIncrement;
        public final ModConfigSpec.DoubleValue weaponChance;
        public final ModConfigSpec.DoubleValue enchantmentChance;
        public final ModConfigSpec.ConfigValue<List<String>> availableBowEnchantments;
        public final ModConfigSpec.ConfigValue<List<String>> availableWeaponEnchantments;
        public final ModConfigSpec.ConfigValue<List<String>> availableArmorEnchantments;
        public final ModConfigSpec.DoubleValue easyGearChance;
        public final ModConfigSpec.DoubleValue normalGearChance;
        public final ModConfigSpec.DoubleValue hardGearChance;
        public final ModConfigSpec.DoubleValue creeperSpeedMultiplier;
        public final ModConfigSpec.DoubleValue spiderSpeedMultiplier;
        public final ModConfigSpec.ConfigValue<List<String>> surfaceArmor;
        public final ModConfigSpec.ConfigValue<List<String>> surfaceWeapons;
        public final ModConfigSpec.ConfigValue<List<String>> deepArmor;
        public final ModConfigSpec.ConfigValue<List<String>> deepWeapons;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("Health Multiplier Settings");

            builder.push("Base Health Multiplier");
            baseHealthMultiplier = builder
                    .comment(
                            "Base health multiplier for enemies.",
                            "This value determines the starting multiplier for enemy health."
                    )
                    .defineInRange("baseHealthMultiplier", 1.0, 0.1, 100.0);
            builder.pop();

            builder.push("Max Health Multiplier");
            maxHealthMultiplier = builder
                    .comment(
                            "Maximum health multiplier for enemies.",
                            "This is the maximum limit the multiplier can reach."
                    )
                    .defineInRange("maxHealthMultiplier", 2.0, 0.1, 10.0);
            builder.pop();

            builder.push("Health Multiplier Increment");
            healthMultiplierIncrement = builder
                    .comment(
                            "Increment to increase health multiplier after reaching certain amount of days.",
                            "This value specifies by how much the multiplier increases."
                    )
                    .defineInRange("healthMultiplierIncrement", 0.2, 0.1, 10.0);
            builder.pop();

            builder.push("Days Per Increment");
            daysPerIncrement = builder
                    .comment(
                            "How many in-game days it takes for the health multiplier to increment."
                    )
                    .defineInRange("daysPerIncrement", 24, 1, 100);
            builder.pop();

            builder.push("Gear Spawn Chances");
            surfaceArmor = builder
                    .comment("List of armor mobs can spawn with near the surface.")
                    .define("surfaceArmor", List.of(
                            "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
                            "chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots",
                            "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots"
                    ));

            surfaceWeapons = builder
                    .comment("List of weapons mobs can spawn with near the surface.")
                    .define("surfaceWeapons", List.of(
                            "stone_sword", "stone_axe", "iron_sword", "iron_axe"
                    ));

            deepArmor = builder
                    .comment("List of armor mobs can spawn with in deep caves.")
                    .define("deepArmor", List.of(
                            "diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots",
                            "gold_helmet", "gold_chestplate", "gold_leggings", "gold_boots",
                            "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots",
                            "chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots"
                    ));

            deepWeapons = builder
                    .comment("List of weapons mobs can spawn with in deep caves.")
                    .define("deepWeapons", List.of(
                            "iron_sword", "iron_axe", "diamond_sword", "diamond_axe"
                    ));

            easyGearChance = builder
                    .comment("Chance for mobs to spawn with gear in EASY mode.")
                    .defineInRange("easyGearChance", 0.10, 0.0, 1.0);

            normalGearChance = builder
                    .comment("Chance for mobs to spawn with gear in NORMAL mode.")
                    .defineInRange("normalGearChance", 0.20, 0.0, 1.0);

            hardGearChance = builder
                    .comment("Chance for mobs to spawn with gear in HARD mode.")
                    .defineInRange("hardGearChance", 0.35, 0.0, 1.0);
            builder.pop();

            builder.push("Equipment Settings");
            weaponChance = builder
                    .comment("Chance for mob to get a weapon.")
                    .defineInRange("weaponChance", 0.18, 0.0, 1.0);

            enchantmentChance = builder
                    .comment("Chance for item to get enchanted.")
                    .defineInRange("enchantmentChance", 0.25, 0.0, 1.0);

            availableBowEnchantments = builder
                    .comment("Enchantments available for bows.")
                    .define("availableBowEnchantments", List.of(
                            "power", "infinity", "flame", "punch"
                    ));

            availableWeaponEnchantments = builder
                    .comment("Enchantments available for melee weapons.")
                    .define("availableWeaponEnchantments", List.of(
                            "sharpness", "smite", "fire_aspect", "unbreaking", "knockback"
                    ));

            availableArmorEnchantments = builder
                    .comment("Enchantments available for armor.")
                    .define("availableArmorEnchantments", List.of(
                            "protection", "unbreaking", "fire_protection", "projectile_protection"
                    ));
            builder.pop();

            builder.push("Creeper Speed Settings");
            creeperSpeedMultiplier = builder
                    .comment("Multiplier to increase Creeper movement speed.",
                            "This value controls how fast Creepers can move.",
                            "Range: 1.0 (normal speed) to 5.0 (very fast)")
                    .defineInRange("creeperSpeedMultiplier", 1.4, 1.0, 5.0);
            builder.pop();

            builder.push("Spider Speed Settings");
            spiderSpeedMultiplier = builder
                    .comment("Multiplier to increase Spider movement speed.",
                            "This value controls how fast Spiders can move.",
                            "Range: 1.0 (normal speed) to 5.0 (very fast)")
                    .defineInRange("spiderSpeedMultiplier", 1.25, 1.0, 5.0);
            builder.pop();

            builder.pop();
        }
    }
}
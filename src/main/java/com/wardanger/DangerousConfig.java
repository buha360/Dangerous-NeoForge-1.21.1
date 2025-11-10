package com.wardanger;

import com.google.common.collect.Lists;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class DangerousConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final DangerousConfig CONFIG;

    public final ModConfigSpec.BooleanValue enableChatAnnouncements;
    public final ModConfigSpec.DoubleValue baseHealthMultiplier;
    public final ModConfigSpec.DoubleValue maxHealthMultiplier;
    public final ModConfigSpec.DoubleValue healthMultiplierIncrement;
    public final ModConfigSpec.IntValue daysPerIncrement;
    public final ModConfigSpec.DoubleValue weaponChance;
    public final ModConfigSpec.DoubleValue enchantCountLevel1;
    public final ModConfigSpec.DoubleValue enchantCountLevel2;
    public final ModConfigSpec.DoubleValue enchantCountLevel3;
    public final ModConfigSpec.DoubleValue easyGearChance;
    public final ModConfigSpec.DoubleValue normalGearChance;
    public final ModConfigSpec.DoubleValue hardGearChance;
    public final ModConfigSpec.DoubleValue creeperSpeedMultiplier;
    public final ModConfigSpec.DoubleValue spiderSpeedMultiplier;
    public final ModConfigSpec.ConfigValue<List<String>> surfaceArmor;
    public final ModConfigSpec.ConfigValue<List<String>> surfaceWeapons;
    public final ModConfigSpec.ConfigValue<List<String>> caveArmor;
    public final ModConfigSpec.ConfigValue<List<?>> surfaceArmorSets;
    public final ModConfigSpec.ConfigValue<List<?>> caveArmorSets;
    public final ModConfigSpec.ConfigValue<List<?>> deepCaveArmorSets;
    public final ModConfigSpec.ConfigValue<List<String>> caveWeapons;
    public final ModConfigSpec.ConfigValue<List<String>> deepCaveArmor;
    public final ModConfigSpec.ConfigValue<List<String>> deepCaveWeapons;
    public final ModConfigSpec.ConfigValue<List<String>> availableBowEnchantments;
    public final ModConfigSpec.ConfigValue<List<String>> availableWeaponEnchantments;
    public final ModConfigSpec.ConfigValue<List<String>> availableArmorEnchantments;

    static {
        Pair<DangerousConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(DangerousConfig::new);
        COMMON_SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    DangerousConfig(ModConfigSpec.Builder builder) {
        builder.push("General Settings");
        enableChatAnnouncements = builder
                .comment("Enable or disable chat announcements from the mod.")
                .define("enableChatAnnouncements", true);
        builder.pop();

        builder.push("Health Multiplier Settings");
        baseHealthMultiplier = builder
                .comment("Base health multiplier for enemies.")
                .defineInRange("baseHealthMultiplier", 1.0, 0.1, 100.0);

        maxHealthMultiplier = builder
                .comment("Maximum health multiplier for enemies.")
                .defineInRange("maxHealthMultiplier", 2.0, 0.1, 10.0);

        healthMultiplierIncrement = builder
                .comment("Increment to increase health multiplier after reaching certain amount of days.")
                .defineInRange("healthMultiplierIncrement", 0.2, 0.1, 10.0);

        daysPerIncrement = builder
                .comment("How many in-game days it takes for the health multiplier to increment.")
                .defineInRange("daysPerIncrement", 24, 1, 100);
        builder.pop();

        builder.push("Gear Spawn Chances");
        surfaceArmor = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of armor mobs can spawn with near the surface.")
                .defineList("surfaceArmor",
                        Lists.newArrayList("minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots",
                                "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots"),
                        obj -> obj instanceof String);

        surfaceWeapons = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of weapons mobs can spawn with near the surface.")
                .defineList("surfaceWeapons",
                        Lists.newArrayList("minecraft:stone_sword", "minecraft:stone_axe", "minecraft:iron_sword", "minecraft:iron_axe"),
                        obj -> obj instanceof String);

        caveArmor = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of armor mobs can spawn with in caves.")
                .defineList("caveArmor",
                        Lists.newArrayList(
                                "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
                                "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots"),
                        obj -> obj instanceof String);

        caveWeapons = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of weapons mobs can spawn with in caves.")
                .defineList("caveWeapons",
                        Lists.newArrayList("minecraft:iron_sword", "minecraft:iron_axe"),
                        obj -> obj instanceof String);

        deepCaveArmor = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of armor mobs can spawn with in deep caves.")
                .defineList("deepCaveArmor",
                        Lists.newArrayList("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots",
                                "minecraft:golden_helmet", "minecraft:golden_chestplate", "minecraft:golden_leggings", "minecraft:golden_boots",
                                "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
                                "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots"),
                        obj -> obj instanceof String);

        deepCaveWeapons = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("List of weapons mobs can spawn with in deep caves.")
                .defineList("deepCaveWeapons",
                        Lists.newArrayList("minecraft:iron_sword", "minecraft:iron_axe", "minecraft:diamond_sword", "minecraft:diamond_axe"),
                        obj -> obj instanceof String);

        easyGearChance = builder.comment("Chance for mobs to spawn with gear in EASY mode.")
                .defineInRange("easyGearChance", 0.10, 0.0, 1.0);
        normalGearChance = builder.comment("Chance for mobs to spawn with gear in NORMAL mode.")
                .defineInRange("normalGearChance", 0.20, 0.0, 1.0);
        hardGearChance = builder.comment("Chance for mobs to spawn with gear in HARD mode.")
                .defineInRange("hardGearChance", 0.35, 0.0, 1.0);

        surfaceArmorSets = builder
                .comment("Full armor sets near surface. Accepts either a String \"[mod:helmet, mod:chest, mod:legs, mod:boots, 25]\" or a list [\"mod:helmet\",\"mod:chest\",\"mod:legs\",\"mod:boots\",25]. Percent: 25->25%, 0.5->0.5%.")
                .defineList("surfaceArmorSets",
                        com.google.common.collect.Lists.newArrayList(
                                "[minecraft:leather_helmet, minecraft:leather_chestplate, minecraft:leather_leggings, minecraft:leather_boots, 1]"),
                        o -> (o instanceof String) || (o instanceof java.util.List<?>)
                );

        caveArmorSets = builder
                .comment("Full armor sets in caves. Same formats as surfaceArmorSets.")
                .defineList("caveArmorSets",
                        com.google.common.collect.Lists.newArrayList("[minecraft:iron_helmet, minecraft:iron_chestplate, minecraft:iron_leggings, minecraft:iron_boots, 1]"),
                        o -> (o instanceof String) || (o instanceof java.util.List<?>)
                );

        deepCaveArmorSets = builder
                .comment("Full armor sets in deep caves. Same formats as surfaceArmorSets.")
                .defineList("deepCaveArmorSets",
                        com.google.common.collect.Lists.newArrayList("[minecraft:diamond_helmet, minecraft:diamond_chestplate, minecraft:diamond_leggings, minecraft:diamond_boots, 1]"),
                        o -> (o instanceof String) || (o instanceof java.util.List<?>)
                );

        builder.pop();

        builder.push("Equipment Settings");
        weaponChance = builder.comment("Chance for mob to get a weapon.")
                .defineInRange("weaponChance", 0.18, 0.0, 1.0);

        enchantCountLevel1 = builder
                .comment("Chance for item to receive 1 enchantment. (0.35 = 35%)")
                .defineInRange("enchantCountLevel1", 0.35, 0.0, 1.0);

        enchantCountLevel2 = builder
                .comment("Chance for item to receive 2 enchantments. (0.15 = 15%)")
                .defineInRange("enchantCountLevel2", 0.15, 0.0, 1.0);

        enchantCountLevel3 = builder
                .comment("Chance for item to receive 3 enchantments. (0.05 = 5%)")
                .defineInRange("enchantCountLevel3", 0.05, 0.0, 1.0);


        availableBowEnchantments = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("Enchantments available for bows.")
                .defineList("availableBowEnchantments",
                        Lists.newArrayList("minecraft:power", "minecraft:infinity", "minecraft:flame", "minecraft:punch"),
                        obj -> obj instanceof String);

        availableWeaponEnchantments = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("Enchantments available for melee weapons.")
                .defineList("availableWeaponEnchantments",
                        Lists.newArrayList("minecraft:sharpness", "minecraft:smite", "minecraft:fire_aspect",
                                "minecraft:unbreaking", "minecraft:knockback", "apotheosis:deep_wounds"),
                        obj -> obj instanceof String);

        availableArmorEnchantments = (ModConfigSpec.ConfigValue<List<String>>) (Object) builder
                .comment("Enchantments available for armor.")
                .defineList("availableArmorEnchantments",
                        Lists.newArrayList("minecraft:protection", "minecraft:unbreaking", "minecraft:fire_protection",
                                "minecraft:projectile_protection"),
                        obj -> obj instanceof String);
        builder.pop();

        builder.push("Creeper Speed Settings");
        creeperSpeedMultiplier = builder
                .comment("Multiplier to increase Creeper movement speed.")
                .defineInRange("creeperSpeedMultiplier", 1.4, 1.0, 5.0);
        builder.pop();

        builder.push("Spider Speed Settings");
        spiderSpeedMultiplier = builder
                .comment("Multiplier to increase Spider movement speed.")
                .defineInRange("spiderSpeedMultiplier", 1.25, 1.0, 5.0);
        builder.pop();
    }
}
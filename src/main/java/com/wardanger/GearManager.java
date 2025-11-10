package com.wardanger;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.data.internal.NeoForgeEnchantmentTagsProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GearManager {

    private static final Random RANDOM = new Random();
    private static final String EQUIPMENT_MODIFIED_TAG = "dangerous_equipment_modified";
    private static final int SURFACE_Y_THRESHOLD = 64;
    private static final int CAVE_Y_THRESHOLD = 0;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double BRUTE_NETHERITE_AXE_CHANCE = 0.05; // 5%
    private static final double BRUTE_DIAMOND_AXE_CHANCE   = 0.15; // 15%
    private record ArmorSet(ResourceLocation head, ResourceLocation chest, ResourceLocation legs, ResourceLocation boots, double chance) {}

    public static void equipMobBasedOnDifficulty(Mob mob, ServerLevel world) {
        Difficulty difficulty = world.getDifficulty();
        CompoundTag entityData = mob.getPersistentData();

        if (entityData.getBoolean(EQUIPMENT_MODIFIED_TAG)) {
            return;
        }

        ResourceLocation zombieType = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        ResourceLocation skeletonType = ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton");
        ResourceLocation creeperType = ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");
        ResourceLocation zombifiedPiglinType = ResourceLocation.fromNamespaceAndPath("minecraft", "zombified_piglin");
        ResourceLocation piglinType = ResourceLocation.fromNamespaceAndPath("minecraft", "piglin");
        ResourceLocation piglinBruteType = ResourceLocation.fromNamespaceAndPath("minecraft", "piglin_brute");
        ResourceLocation pillagerType = ResourceLocation.fromNamespaceAndPath("minecraft", "pillager");
        ResourceLocation vindicatorType = ResourceLocation.fromNamespaceAndPath("minecraft", "vindicator");
        ResourceLocation evokerType = ResourceLocation.fromNamespaceAndPath("minecraft", "evoker");
        ResourceLocation illusionerType = ResourceLocation.fromNamespaceAndPath("minecraft", "illusioner");

        if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(zombieType)) {
            giveZombieWeapon(mob, world);
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.surfaceArmor.get(),
                        DangerousConfig.CONFIG.surfaceWeapons.get(),
                        DangerousConfig.CONFIG.surfaceArmorSets.get(),
                        world);
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.caveArmor.get(),
                        DangerousConfig.CONFIG.caveWeapons.get(),
                        DangerousConfig.CONFIG.caveArmorSets.get(),
                        world);
            } else {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.deepCaveArmor.get(),
                        DangerousConfig.CONFIG.deepCaveWeapons.get(),
                        DangerousConfig.CONFIG.deepCaveArmorSets.get(),
                        world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(skeletonType)) {
            giveSkeletonWeapon(mob, world);
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.surfaceArmor.get(),
                        DangerousConfig.CONFIG.surfaceWeapons.get(),
                        DangerousConfig.CONFIG.surfaceArmorSets.get(),
                        world);
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.caveArmor.get(),
                        DangerousConfig.CONFIG.caveWeapons.get(),
                        DangerousConfig.CONFIG.caveArmorSets.get(),
                        world);
            } else {
                giveLevelGear(mob, difficulty,
                        DangerousConfig.CONFIG.deepCaveArmor.get(),
                        DangerousConfig.CONFIG.deepCaveWeapons.get(),
                        DangerousConfig.CONFIG.deepCaveArmorSets.get(),
                        world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(creeperType)) {
            increaseCreeperSpeed((Creeper) mob);
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(zombifiedPiglinType)) {
            giveGoldArmorPiecesIfChance(mob, difficulty, world);
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(piglinType)) {
            giveGoldArmorPiecesIfChance(mob, difficulty, world);
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(piglinBruteType)) {
            giveGoldArmorPiecesIfChance(mob, difficulty, world);
            givePiglinBruteWeapon(mob, world);
        }else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(pillagerType)
                || mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(vindicatorType)
                || mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(evokerType)
                || mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(illusionerType)) {

            giveIllagerIronArmorPiecesIfChance(mob, difficulty, world);

            if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(pillagerType)) {
                givePillagerMeleeIfChance(mob, world);
            } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(vindicatorType)) {
                upgradeVindicatorWeaponIfLucky(mob, world);
            } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(evokerType)) {
                giveIllagerIronArmorPiecesIfChance(mob, difficulty, world);
            }
        }

        entityData.putBoolean(EQUIPMENT_MODIFIED_TAG, true);
    }

    private static final ResourceLocation OMINOUS_BANNER_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "ominous_banner");

    private static boolean isOminousBanner(ItemStack s) {
        if (s.isEmpty()) return false;
        return OMINOUS_BANNER_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    private static void giveIllagerIronArmorPiecesIfChance(Mob mob, Difficulty difficulty, ServerLevel world) {
        double chance = getGearChanceForDifficulty(difficulty);

        equipFixedArmorIfChance(mob, EquipmentSlot.HEAD,  getItemByName("minecraft:iron_helmet"),     chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.CHEST, getItemByName("minecraft:iron_chestplate"), chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.LEGS,  getItemByName("minecraft:iron_leggings"),   chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.FEET,  getItemByName("minecraft:iron_boots"),      chance, world);
    }

    private static void givePillagerMeleeIfChance(Mob mob, ServerLevel world) {
        double weaponChance = DangerousConfig.CONFIG.weaponChance.get();
        if (RANDOM.nextDouble() >= weaponChance) return;

        ItemStack off = mob.getOffhandItem();
        if (isOminousBanner(off)) return; // patrol captain - no change

        String[] candidates = new String[] {
                "minecraft:iron_sword", "minecraft:iron_axe",
                "minecraft:diamond_sword", "minecraft:diamond_axe"
        };

        String pick = candidates[RANDOM.nextInt(candidates.length)];
        ItemStack melee = getItemByName(pick);
        if (melee.isEmpty()) return;

        enchantItem(melee, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
        giveNBT(melee);

        if (mob.getOffhandItem().isEmpty()) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, melee);
        } else if (mob.getMainHandItem().isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, melee);
        }
    }

    private static void upgradeVindicatorWeaponIfLucky(Mob mob, ServerLevel world) {
        double chance = DangerousConfig.CONFIG.weaponChance.get() * 0.7; // picit ritkább, tetszés szerint
        if (RANDOM.nextDouble() >= chance) return;

        ItemStack main = mob.getMainHandItem();
        if (main.isEmpty()) return;

        ResourceLocation cur = BuiltInRegistries.ITEM.getKey(main.getItem());
        boolean meleeLike = cur.getPath().contains("sword") || cur.getPath().contains("axe");
        if (!meleeLike) return;

        String[] upgrades = new String[] { "minecraft:diamond_axe", "minecraft:diamond_sword" };
        String pick = upgrades[RANDOM.nextInt(upgrades.length)];
        ItemStack upgraded = getItemByName(pick);
        if (upgraded.isEmpty()) return;

        enchantItem(upgraded, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
        giveNBT(upgraded);
        mob.setItemSlot(EquipmentSlot.MAINHAND, upgraded);
    }

    private static void giveGoldArmorPiecesIfChance(Mob mob, Difficulty difficulty, ServerLevel world) {
        double chance = getGearChanceForDifficulty(difficulty);

        equipFixedArmorIfChance(mob, EquipmentSlot.HEAD,  getItemByName("minecraft:golden_helmet"),     chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.CHEST, getItemByName("minecraft:golden_chestplate"), chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.LEGS,  getItemByName("minecraft:golden_leggings"),   chance, world);
        equipFixedArmorIfChance(mob, EquipmentSlot.FEET,  getItemByName("minecraft:golden_boots"),      chance, world);
    }

    private static void equipFixedArmorIfChance(Mob mob, EquipmentSlot slot, ItemStack stack, double chance, ServerLevel world) {
        if (stack.isEmpty()) return;
        if (RANDOM.nextDouble() >= chance) return;

        enchantItem(stack, DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
        giveNBT(stack);
        mob.setItemSlot(slot, stack);
    }

    private static void givePiglinBruteWeapon(Mob mob, ServerLevel world) {
        if (RANDOM.nextDouble() < BRUTE_NETHERITE_AXE_CHANCE) {
            ItemStack netheriteAxe = getItemByName("minecraft:netherite_axe");
            if (!netheriteAxe.isEmpty()) {
                enchantItem(netheriteAxe, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
                giveNBT(netheriteAxe);
                mob.setItemSlot(EquipmentSlot.MAINHAND, netheriteAxe);
                return;
            }
        }

        if (RANDOM.nextDouble() < BRUTE_DIAMOND_AXE_CHANCE) {
            ItemStack diamondAxe = getItemByName("minecraft:diamond_axe");
            if (!diamondAxe.isEmpty()) {
                enchantItem(diamondAxe, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
                giveNBT(diamondAxe);
                mob.setItemSlot(EquipmentSlot.MAINHAND, diamondAxe);
                return;
            }
        }

        ItemStack goldAxe = getItemByName("minecraft:golden_axe");
        if (!goldAxe.isEmpty() && mob.getMainHandItem().isEmpty()) {
            enchantItem(goldAxe, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
            giveNBT(goldAxe);
            mob.setItemSlot(EquipmentSlot.MAINHAND, goldAxe);
        }
    }

    private static void giveLevelGear(Mob mob, Difficulty difficulty, List<String> armorList, List<String> weaponList, List<?> armorSetList, ServerLevel world) {
        if (tryEquipArmorSet(mob, armorSetList, world)) return;

        equipArmorIfChance(mob, EquipmentSlot.HEAD,  "helmet",   getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.CHEST, "chestplate", getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.LEGS,  "leggings", getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.FEET,  "boots",    getGearChanceForDifficulty(difficulty), armorList, world);
    }

    private static boolean tryEquipArmorSet(Mob mob, List<?> armorSetList, ServerLevel world) {
        if (armorSetList == null || armorSetList.isEmpty()) return false;
        for (Object raw : armorSetList) {
            ArmorSet set;
            try {
                if (raw instanceof String s) set = parseArmorSetString(s);
                else if (raw instanceof java.util.List<?> lst) set = parseArmorSetList(lst);
                else { LOGGER.warn("Unknown armor set entry type: {}", raw); continue; }
                if (set == null) continue;
                if (RANDOM.nextDouble() >= set.chance()) continue;

                ItemStack head  = getItemByName(set.head().toString());
                ItemStack chest = getItemByName(set.chest().toString());
                ItemStack legs  = getItemByName(set.legs().toString());
                ItemStack boots = getItemByName(set.boots().toString());
                if (head.isEmpty() || chest.isEmpty() || legs.isEmpty() || boots.isEmpty()) {
                    LOGGER.warn("Armor set skipped due to missing item: {}", raw);
                    continue;
                }

                enchantItem(head,  DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
                enchantItem(chest, DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
                enchantItem(legs,  DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
                enchantItem(boots, DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
                giveNBT(head); giveNBT(chest); giveNBT(legs); giveNBT(boots);

                mob.setItemSlot(EquipmentSlot.HEAD,  head);
                mob.setItemSlot(EquipmentSlot.CHEST, chest);
                mob.setItemSlot(EquipmentSlot.LEGS,  legs);
                mob.setItemSlot(EquipmentSlot.FEET,  boots);
                return true;
            } catch (Throwable t) {
                LOGGER.warn("Failed to parse/apply armor set entry: {}", raw, t);
            }
        }
        return false;
    }

    private static ArmorSet parseArmorSetString(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]"))   s = s.substring(0, s.length() - 1);

        String[] parts = s.split(",");
        if (parts.length != 5) {
            LOGGER.warn("Invalid armor set format (need 5 entries): {}", raw);
            return null;
        }
        String h = parts[0].trim();
        String c = parts[1].trim();
        String l = parts[2].trim();
        String b = parts[3].trim();
        String p = parts[4].trim();

        ResourceLocation rh = ResourceLocation.tryParse(h);
        ResourceLocation rc = ResourceLocation.tryParse(c);
        ResourceLocation rl = ResourceLocation.tryParse(l);
        ResourceLocation rb = ResourceLocation.tryParse(b);
        if (rh == null || rc == null || rl == null || rb == null) {
            LOGGER.warn("Invalid item id in armor set: {}", raw);
            return null;
        }

        double pct;
        try { pct = Double.parseDouble(p); }
        catch (NumberFormatException e) {
            LOGGER.warn("Invalid percentage in armor set: {}", raw);
            return null;
        }
        double chance = clamp01(percentToUnit(pct));
        return new ArmorSet(rh, rc, rl, rb, chance);
    }

    // ÚJ: beágyazott listás parser: ["m:h","m:c","m:l","m:b", 25]
    private static ArmorSet parseArmorSetList(List<?> lst) {
        if (lst.size() != 5) {
            LOGGER.warn("Invalid armor set list size (need 5): {}", lst);
            return null;
        }

        Object oh = lst.get(0), oc = lst.get(1), ol = lst.get(2), ob = lst.get(3), op = lst.get(4);

        if (!(oh instanceof String h && oc instanceof String c && ol instanceof String l && ob instanceof String b)) {
            LOGGER.warn("Armor set list expects 4 strings + 1 number: {}", lst);
            return null;
        }

        ResourceLocation rh = ResourceLocation.tryParse(h);
        ResourceLocation rc = ResourceLocation.tryParse(c);
        ResourceLocation rl = ResourceLocation.tryParse(l);
        ResourceLocation rb = ResourceLocation.tryParse(b);

        if (rh == null || rc == null || rl == null || rb == null) {
            LOGGER.warn("Invalid item id in armor set list: {}", lst);
            return null;
        }

        double pct;
        if (op instanceof Number n) {
            pct = n.doubleValue();
        } else if (op instanceof String sp) {
            try { pct = Double.parseDouble(sp); } catch (NumberFormatException e) { return null; }
        } else {
            LOGGER.warn("Armor set percentage must be number or numeric string: {}", lst);
            return null;
        }
        double chance = clamp01(percentToUnit(pct));
        return new ArmorSet(rh, rc, rl, rb, chance);
    }

    private static double percentToUnit(double pct) {
        return pct / 100.0;
    }

    private static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }

    private static void equipArmorIfChance(Mob mob, EquipmentSlot slot, String armorType, double chance, List<String> availableArmor, ServerLevel world) {
        List<String> filteredArmor = availableArmor.stream()
                .filter(armor -> armor.contains(armorType))
                .toList();

        if (!filteredArmor.isEmpty() && RANDOM.nextDouble() < chance) {
            String selectedArmor = filteredArmor.get(RANDOM.nextInt(filteredArmor.size()));
            ItemStack armor = new ItemStack(Objects.requireNonNull(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(selectedArmor))));

            enchantItem(armor, DangerousConfig.CONFIG.availableArmorEnchantments.get(), world);
            giveNBT(armor);
            mob.setItemSlot(slot, armor);
        }
    }

    private static double getGearChanceForDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> DangerousConfig.CONFIG.easyGearChance.get();
            case NORMAL -> DangerousConfig.CONFIG.normalGearChance.get();
            case HARD -> DangerousConfig.CONFIG.hardGearChance.get();
            default -> 0.0;
        };
    }

    private static void giveNBT(ItemStack item) {
        if (item.isEmpty()) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("dangerous_equipment_modified", true);
        CustomData customData = CustomData.of(tag);
        item.set(DataComponents.CUSTOM_DATA, customData);
    }

    private static void giveZombieWeapon(Mob mob, ServerLevel world) {
        double weaponChance = DangerousConfig.CONFIG.weaponChance.get();
        double randomValue = RANDOM.nextDouble();

        if (randomValue < weaponChance) {
            List<String> weaponList;
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                weaponList = DangerousConfig.CONFIG.surfaceWeapons.get();
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                weaponList = DangerousConfig.CONFIG.caveWeapons.get();
            } else {
                weaponList = DangerousConfig.CONFIG.deepCaveWeapons.get();
            }

            String weaponName = weaponList.get(RANDOM.nextInt(weaponList.size()));
            ItemStack weapon = getItemByName(weaponName);

            if (!weapon.isEmpty()) {
                enchantItem(weapon, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
                giveNBT(weapon);
                mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            }
        }
    }

    private static void giveSkeletonWeapon(Mob mob, ServerLevel world) {
        double weaponChance = DangerousConfig.CONFIG.weaponChance.get();
        double randomValue = RANDOM.nextDouble();

        ItemStack bow = getItemByName("minecraft:bow");
        enchantItem(bow, DangerousConfig.CONFIG.availableBowEnchantments.get(), world);
        giveNBT(bow);
        mob.setItemSlot(EquipmentSlot.MAINHAND, bow);

        if (randomValue < weaponChance) {
            List<String> weaponList;
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                weaponList = DangerousConfig.CONFIG.surfaceWeapons.get();
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                weaponList = DangerousConfig.CONFIG.caveWeapons.get();
            } else {
                weaponList = DangerousConfig.CONFIG.deepCaveWeapons.get();
            }

            String weaponName = weaponList.get(RANDOM.nextInt(weaponList.size()));
            ItemStack weapon = getItemByName(weaponName);

            if (!weapon.isEmpty()) {
                enchantItem(weapon, DangerousConfig.CONFIG.availableWeaponEnchantments.get(), world);
                giveNBT(weapon);
                mob.setItemSlot(EquipmentSlot.OFFHAND, weapon);
            }
        }
    }

    private static void enchantItem(ItemStack item, List<String> availableEnchantments, ServerLevel world) {
        if (item.isEmpty()) return;

        int enchantCount = getRandomEnchantCount();
        if (enchantCount <= 0) {
            LOGGER.info("[Enchant Debug] Item {} got 0 enchant rolls (skipped)",
                    BuiltInRegistries.ITEM.getKey(item.getItem()));
            return;
        }

        Registry<Enchantment> enchantmentRegistry = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        List<Enchantment> possible = new java.util.ArrayList<>();

        if (availableEnchantments.size() == 1 && availableEnchantments.getFirst().equals("*")) {
            for (Enchantment e : enchantmentRegistry) {
                if (e.canEnchant(item)) possible.add(e);
            }
            LOGGER.info("[Enchant Debug] Wildcard: found {} compatible enchantments for {}",
                    possible.size(), BuiltInRegistries.ITEM.getKey(item.getItem()));
        } else {
            for (String id : availableEnchantments) {
                if (!id.contains(":")) {
                    LOGGER.warn("[Enchant Debug] Invalid enchant id: {}", id);
                    continue;
                }
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                Enchantment e = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, rl));
                if (e != null && e.canEnchant(item)) {
                    possible.add(e);
                }
            }
            LOGGER.info("[Enchant Debug] Using listed enchants: {} possible for {}",
                    possible.size(), BuiltInRegistries.ITEM.getKey(item.getItem()));
        }

        if (possible.isEmpty()) {
            LOGGER.info("[Enchant Debug] No valid enchantments found for {}", BuiltInRegistries.ITEM.getKey(item.getItem()));
            return;
        }

        java.util.Collections.shuffle(possible, RANDOM);
        int applied = Math.min(enchantCount, possible.size());
        java.util.List<String> appliedNames = new java.util.ArrayList<>();

        Collections.shuffle(possible, RANDOM);

        for (int i = 0; i < applied; i++) {
            Enchantment e = possible.get(i);
            int level = 1 + RANDOM.nextInt(e.getMaxLevel());
            item.enchant(enchantmentRegistry.wrapAsHolder(e), level);

            ResourceLocation id = enchantmentRegistry.getKey(e);
            if (id == null) id = Registries.ENCHANTMENT.location();

            appliedNames.add((id != null ? id.toString() : "unknown_enchant") + " (lvl " + level + ")");
        }

        LOGGER.info("[Enchant Debug] Enchanted {} with {} enchant(s): {}",
                BuiltInRegistries.ITEM.getKey(item.getItem()), applied, appliedNames);
    }

    private static int getRandomEnchantCount() {
        double r = RANDOM.nextDouble();
        double c3 = DangerousConfig.CONFIG.enchantCountLevel3.get();
        double c2 = DangerousConfig.CONFIG.enchantCountLevel2.get();
        double c1 = DangerousConfig.CONFIG.enchantCountLevel1.get();

        if (r < c3) return 3;
        if (r < c3 + c2) return 2;
        if (r < c3 + c2 + c1) return 1;
        return 0;
    }

    private static void increaseCreeperSpeed(Creeper creeper) {
        AttributeInstance speedAttribute = creeper.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double speedMultiplier = DangerousConfig.CONFIG.creeperSpeedMultiplier.get();
            speedAttribute.setBaseValue(speedAttribute.getBaseValue() * speedMultiplier);
        }
    }

    private static ItemStack getItemByName(String itemName) {
        if (!itemName.contains(":")) {
            LOGGER.warn("Invalid item format in the config: {}. Correct format: modid:item_name", itemName);
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(itemName);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            LOGGER.warn("Item not found in the registry: {}", itemName);
            return ItemStack.EMPTY;
        }

        return new ItemStack(BuiltInRegistries.ITEM.get(itemId));
    }
}
package com.wardanger;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GearManager {

    private static final Random RANDOM = new Random();
    private static final String EQUIPMENT_MODIFIED_TAG = "dangerous_equipment_modified";
    private static final int SURFACE_Y_THRESHOLD = 64;
    private static final int CAVE_Y_THRESHOLD = 0;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void equipMobBasedOnDifficulty(Mob mob, ServerLevel world) {
        Difficulty difficulty = world.getDifficulty();
        CompoundTag entityData = mob.getPersistentData();

        if (entityData.getBoolean(EQUIPMENT_MODIFIED_TAG)) {
            return;
        }

        ResourceLocation zombieType = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        ResourceLocation skeletonType = ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton");
        ResourceLocation creeperType = ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");

        if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(zombieType)) {
            giveZombieWeapon(mob, world);
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.surfaceArmor.get(), DangerousConfig.CONFIG.surfaceWeapons.get(), world);
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.caveArmor.get(), DangerousConfig.CONFIG.caveWeapons.get(), world);
            } else {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.deepCaveArmor.get(), DangerousConfig.CONFIG.deepCaveWeapons.get(), world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(skeletonType)) {
            giveSkeletonWeapon(mob, world);
            if (mob.getY() >= SURFACE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.surfaceArmor.get(), DangerousConfig.CONFIG.surfaceWeapons.get(), world);
            } else if (mob.getY() >= CAVE_Y_THRESHOLD) {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.caveArmor.get(), DangerousConfig.CONFIG.caveWeapons.get(), world);
            } else {
                giveLevelGear(mob, difficulty, DangerousConfig.CONFIG.deepCaveArmor.get(), DangerousConfig.CONFIG.deepCaveWeapons.get(), world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(creeperType)) {
            increaseCreeperSpeed((Creeper) mob);
        }

        entityData.putBoolean(EQUIPMENT_MODIFIED_TAG, true);
    }

    private static void giveLevelGear(Mob mob, Difficulty difficulty, List<String> armorList, List<String> weaponList, ServerLevel world) {
        equipArmorIfChance(mob, EquipmentSlot.HEAD, "helmet", getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.CHEST, "chestplate", getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.LEGS, "leggings", getGearChanceForDifficulty(difficulty), armorList, world);
        equipArmorIfChance(mob, EquipmentSlot.FEET, "boots", getGearChanceForDifficulty(difficulty), armorList, world);
    }

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

        DangerousConfig.CONFIG.enchantmentChance.get();

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
        if (item.isEmpty() || availableEnchantments.isEmpty()) {
            return;
        }

        double enchantmentChance = DangerousConfig.CONFIG.enchantmentChance.get();
        double randomValue = RANDOM.nextDouble();

        if (randomValue < enchantmentChance) {
            String selectedEnchantment = availableEnchantments.get(RANDOM.nextInt(availableEnchantments.size()));

            if (!selectedEnchantment.contains(":")) {
                LOGGER.warn("Invalid enchantment format in the config: {}. Correct format: modid:enchantment_name", selectedEnchantment);
                return;
            }

            ResourceLocation enchantmentId = ResourceLocation.tryParse(selectedEnchantment);
            Registry<Enchantment> enchantmentRegistry = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

            assert enchantmentId != null;
            if (!enchantmentRegistry.containsKey(enchantmentId)) {
                LOGGER.warn("Enchantment not found in the registry: {}", selectedEnchantment);
                return;
            }

            Holder<Enchantment> enchantmentHolder = enchantmentRegistry.getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
            int enchantmentLevel = 1 + RANDOM.nextInt(enchantmentHolder.value().getMaxLevel());
            item.enchant(enchantmentHolder, enchantmentLevel);
        }
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
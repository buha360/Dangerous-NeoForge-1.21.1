package com.wardanger;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GearManager {

    private static final Random RANDOM = new Random();
    private static final String EQUIPMENT_MODIFIED_TAG = "dangerous_equipment_modified";
    private static final int SURFACE_Y_THRESHOLD = 12;

    public static void equipMobBasedOnDifficulty(Mob mob, ServerLevel world) {
        Difficulty difficulty = world.getDifficulty();
        CompoundTag entityData = mob.getPersistentData();

        if (entityData.getBoolean(EQUIPMENT_MODIFIED_TAG)) {
            return;
        }

        if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"))) {
            giveZombieWeapon(mob, world);
            if (mob.getY() > SURFACE_Y_THRESHOLD) {
                giveSurfaceLevelGear(mob, difficulty, world);
            } else {
                giveDeepLevelGear(mob, difficulty, world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton"))) {
            giveSkeletonWeapon(mob, world);
            if (mob.getY() > SURFACE_Y_THRESHOLD) {
                giveSurfaceLevelGear(mob, difficulty, world);
            } else {
                giveDeepLevelGear(mob, difficulty, world);
            }
        } else if (mob.getType() == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "creeper"))) {
            increaseCreeperSpeed((Creeper) mob);
        }

        entityData.putBoolean(EQUIPMENT_MODIFIED_TAG, true);
    }

    private static void giveSurfaceLevelGear(Mob mob, Difficulty difficulty, ServerLevel world) {
        equipArmor(mob, EquipmentSlot.HEAD, DangerousConfig.COMMON.surfaceArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.CHEST, DangerousConfig.COMMON.surfaceArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.LEGS, DangerousConfig.COMMON.surfaceArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.FEET, DangerousConfig.COMMON.surfaceArmor.get(), difficulty, world);
    }

    private static void giveDeepLevelGear(Mob mob, Difficulty difficulty, ServerLevel world) {
        equipArmor(mob, EquipmentSlot.HEAD, DangerousConfig.COMMON.deepArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.CHEST, DangerousConfig.COMMON.deepArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.LEGS, DangerousConfig.COMMON.deepArmor.get(), difficulty, world);
        equipArmor(mob, EquipmentSlot.FEET, DangerousConfig.COMMON.deepArmor.get(), difficulty, world);
    }

    private static void equipArmor(Mob mob, EquipmentSlot slot, List<String> armorList, Difficulty difficulty, ServerLevel world) {
        if (!armorList.isEmpty() && RANDOM.nextDouble() < getGearChanceForDifficulty(difficulty)) {
            String armorName = armorList.get(RANDOM.nextInt(armorList.size()));
            ItemStack armor = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", armorName)));

            enchantItem(armor, DangerousConfig.COMMON.availableArmorEnchantments.get(), world);

            giveNBT(armor);

            if (slot == EquipmentSlot.HEAD && armor.getItem().canEquip(armor, EquipmentSlot.HEAD, mob)) {
                mob.setItemSlot(slot, armor);
            } else if (slot == EquipmentSlot.CHEST && armor.getItem().canEquip(armor, EquipmentSlot.CHEST, mob)) {
                mob.setItemSlot(slot, armor);
            } else if (slot == EquipmentSlot.LEGS && armor.getItem().canEquip(armor, EquipmentSlot.LEGS, mob)) {
                mob.setItemSlot(slot, armor);
            } else if (slot == EquipmentSlot.FEET && armor.getItem().canEquip(armor, EquipmentSlot.FEET, mob)) {
                mob.setItemSlot(slot, armor);
            }
        }
    }

    private static double getGearChanceForDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> DangerousConfig.COMMON.easyGearChance.get();
            case NORMAL -> DangerousConfig.COMMON.normalGearChance.get();
            case HARD -> DangerousConfig.COMMON.hardGearChance.get();
            default -> 0.0;
        };
    }

    private static void giveNBT(ItemStack item) {
        if (item.isEmpty()) {
            return;
        }

        DangerousConfig.COMMON.enchantmentChance.get();

        CompoundTag tag = new CompoundTag();
        tag.putBoolean("dangerous_equipment_modified", true);
        CustomData customData = CustomData.of(tag);
        item.set(DataComponents.CUSTOM_DATA, customData);
    }

    private static void giveZombieWeapon(Mob mob, ServerLevel world) {
        if (RANDOM.nextDouble() < DangerousConfig.COMMON.weaponChance.get()) {
            List<String> weaponList;

            if (mob.getY() < SURFACE_Y_THRESHOLD) {
                weaponList = DangerousConfig.COMMON.deepWeapons.get();
            } else {
                weaponList = DangerousConfig.COMMON.surfaceWeapons.get();
            }

            String weaponName = weaponList.get(RANDOM.nextInt(weaponList.size()));
            ItemStack weapon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", weaponName)));

            enchantItem(weapon, DangerousConfig.COMMON.availableWeaponEnchantments.get(), world);
            giveNBT(weapon);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }

    private static void giveSkeletonWeapon(Mob mob, ServerLevel world) {
        ItemStack bow = new ItemStack(Items.BOW);
        enchantItem(bow, DangerousConfig.COMMON.availableBowEnchantments.get(), world);
        giveNBT(bow);
        mob.setItemSlot(EquipmentSlot.MAINHAND, bow);

        if (RANDOM.nextDouble() < DangerousConfig.COMMON.weaponChance.get()) {
            List<String> weaponList;

            if (mob.getY() < SURFACE_Y_THRESHOLD) {
                weaponList = DangerousConfig.COMMON.deepWeapons.get();
            } else {
                weaponList = DangerousConfig.COMMON.surfaceWeapons.get();
            }

            String weaponName = weaponList.get(RANDOM.nextInt(weaponList.size()));
            ItemStack weapon = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", weaponName)));

            enchantItem(weapon, DangerousConfig.COMMON.availableWeaponEnchantments.get(), world);
            giveNBT(weapon);
            mob.setItemSlot(EquipmentSlot.OFFHAND, weapon);
        }
    }

    private static void enchantItem(ItemStack item, List<String> availableEnchantments, ServerLevel world) {
        if (item.isEmpty() || availableEnchantments.isEmpty()) {
            return;
        }

        double enchantmentChance = RANDOM.nextDouble();
        if (enchantmentChance < DangerousConfig.COMMON.enchantmentChance.get()) {
            String selectedEnchantment = availableEnchantments.get(RANDOM.nextInt(availableEnchantments.size()));
            ResourceLocation enchantmentId = ResourceLocation.tryParse(selectedEnchantment);

            if (enchantmentId != null) {
                HolderLookup.Provider provider = world.registryAccess();
                HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = provider.lookupOrThrow(Registries.ENCHANTMENT);
                Optional<Holder.Reference<Enchantment>> enchantmentHolder = enchantmentRegistry.get(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));

                enchantmentHolder.ifPresent(holder -> {
                    int enchantmentLevel = 1 + RANDOM.nextInt(3);
                    item.enchant(holder, enchantmentLevel);
                });
            }
        }
    }

    private static void increaseCreeperSpeed(Creeper creeper) {
        AttributeInstance speedAttribute = creeper.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            double speedMultiplier = DangerousConfig.COMMON.creeperSpeedMultiplier.get();
            speedAttribute.setBaseValue(speedAttribute.getBaseValue() * speedMultiplier);
        }
    }
}
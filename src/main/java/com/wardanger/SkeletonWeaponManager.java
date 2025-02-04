package com.wardanger;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkeletonWeaponManager {

    private static final Map<UUID, Boolean> skeletonMeleeMode = new HashMap<>();
    private static final Map<UUID, Long> lastSwitchTime = new HashMap<>();
    private static final long SWITCH_COOLDOWN = 4000L;

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof AbstractSkeleton skeleton && !skeleton.level().isClientSide) {
            long currentTime = System.currentTimeMillis();
            UUID skeletonId = skeleton.getUUID();

            if (lastSwitchTime.containsKey(skeletonId) && (currentTime - lastSwitchTime.get(skeletonId) < SWITCH_COOLDOWN)) {
                return;
            }

            updateSkeletonWeapon(skeleton);

            lastSwitchTime.put(skeletonId, currentTime);
        }
    }

    private void updateSkeletonWeapon(AbstractSkeleton skeleton) {
        boolean shouldBeMelee = isPlayerNearby(skeleton);
        UUID skeletonId = skeleton.getUUID();

        if (skeletonMeleeMode.getOrDefault(skeletonId, false) != shouldBeMelee) {
            if (shouldBeMelee) {
                switchToMelee(skeleton);
            } else {
                switchToRanged(skeleton);
            }
            skeletonMeleeMode.put(skeletonId, shouldBeMelee);
        }
    }

    private boolean isPlayerNearby(AbstractSkeleton skeleton) {
        List<Player> nearbyPlayers = skeleton.level().getEntitiesOfClass(Player.class,
                skeleton.getBoundingBox().inflate(6.0), EntitySelector.NO_SPECTATORS);
        return !nearbyPlayers.isEmpty();
    }

    private void switchToRanged(AbstractSkeleton skeleton) {
        ItemStack mainHand = skeleton.getMainHandItem();
        ItemStack offHand = skeleton.getOffhandItem();

        if ((mainHand.getItem() instanceof SwordItem || mainHand.getItem() instanceof AxeItem) && offHand.getItem() instanceof BowItem) {
            skeleton.setItemSlot(EquipmentSlot.OFFHAND, mainHand);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, offHand);
            setAttackMode(skeleton, false);
        }
    }

    private void switchToMelee(AbstractSkeleton skeleton) {
        ItemStack mainHand = skeleton.getMainHandItem();
        ItemStack offHand = skeleton.getOffhandItem();

        if (mainHand.getItem() instanceof BowItem && (offHand.getItem() instanceof SwordItem || offHand.getItem() instanceof AxeItem)) {
            skeleton.setItemSlot(EquipmentSlot.OFFHAND, mainHand);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, offHand);
            setAttackMode(skeleton, true);
        }
    }

    private void setAttackMode(AbstractSkeleton skeleton, boolean melee) {
        skeleton.goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof RangedBowAttackGoal || goal.getGoal() instanceof MeleeAttackGoal);

        if (melee) {
            skeleton.goalSelector.addGoal(4, new MeleeAttackGoal(skeleton, 1.2D, false));
        } else {
            skeleton.goalSelector.addGoal(4, new RangedBowAttackGoal<>(skeleton, 1.0D, 20, 15.0F));
        }

        skeleton.setAggressive(melee);
    }
}
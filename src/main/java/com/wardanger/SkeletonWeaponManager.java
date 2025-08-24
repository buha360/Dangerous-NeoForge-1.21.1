package com.wardanger;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SkeletonWeaponManager {

    private static final Map<UUID, Boolean> skeletonMeleeMode = new HashMap<>();
    private static final Map<UUID, Long>   lastSwitchTick     = new HashMap<>();
    private static final Map<UUID, ItemStack> stashedBow      = new HashMap<>();

    private static final long SWITCH_COOLDOWN_TICKS = 20L; // ~1s @20TPS
    private static final double MELEE_IN  = 6.0;
    private static final double MELEE_OUT = 8.0;

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton) || skeleton.level().isClientSide) return;

        long now = skeleton.level().getGameTime();
        UUID id = skeleton.getUUID();
        if (now - lastSwitchTick.getOrDefault(id, 0L) < SWITCH_COOLDOWN_TICKS) return;

        updateSkeletonWeapon(skeleton);
        lastSwitchTick.put(id, now);
    }

    private void updateSkeletonWeapon(AbstractSkeleton skeleton) {
        boolean shouldBeMelee = shouldGoMelee(skeleton);
        UUID id = skeleton.getUUID();

        if (skeletonMeleeMode.getOrDefault(id, false) != shouldBeMelee) {
            if (shouldBeMelee) switchToMelee(skeleton);
            else switchToRanged(skeleton);

            forceAttackMode(skeleton, shouldBeMelee);
            skeletonMeleeMode.put(id, shouldBeMelee);
        }
    }

    private boolean shouldGoMelee(AbstractSkeleton skeleton) {
        if (!hasMeleeWeapon(skeleton)) return false;

        LivingEntity tgt = skeleton.getTarget();
        boolean nowMelee = skeletonMeleeMode.getOrDefault(skeleton.getUUID(), false);

        if (tgt == null || !tgt.isAlive()) {
            List<Player> players = skeleton.level().getEntitiesOfClass(
                    Player.class, skeleton.getBoundingBox().inflate(MELEE_IN), EntitySelector.NO_SPECTATORS);
            return !players.isEmpty();
        }
        double d = skeleton.distanceTo(tgt);
        boolean los = skeleton.hasLineOfSight(tgt);

        if (!nowMelee) {
            return los && d <= MELEE_IN;
        } else {
            return !(d >= MELEE_OUT || !los);
        }
    }

    private static boolean isMelee(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem);
    }
    private static boolean isBow(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof BowItem);
    }
    private static boolean hasMeleeWeapon(AbstractSkeleton sk) {
        return isMelee(sk.getMainHandItem()) || isMelee(sk.getOffhandItem());
    }

    private void switchToMelee(AbstractSkeleton sk) {
        UUID id = sk.getUUID();
        ItemStack main = sk.getMainHandItem();
        ItemStack off  = sk.getOffhandItem();

        ItemStack foundBow = ItemStack.EMPTY;
        if (isBow(main)) {
            foundBow = main.copy();
            sk.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (isBow(off)) {
            foundBow = off.copy();
            sk.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        if (!foundBow.isEmpty()) stashedBow.put(id, foundBow);

        main = sk.getMainHandItem();
        off  = sk.getOffhandItem();
        if (!isMelee(main)) {
            if (isMelee(off)) {
                sk.setItemSlot(EquipmentSlot.MAINHAND, off.copy());
                sk.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    private void switchToRanged(AbstractSkeleton sk) {
        UUID id = sk.getUUID();

        ItemStack bow = stashedBow.getOrDefault(id, ItemStack.EMPTY);
        if (bow.isEmpty()) {
            if (isBow(sk.getMainHandItem())) bow = sk.getMainHandItem().copy();
            else if (isBow(sk.getOffhandItem())) bow = sk.getOffhandItem().copy();
        }

        ItemStack main = sk.getMainHandItem();
        ItemStack off  = sk.getOffhandItem();
        ItemStack melee = isMelee(main) ? main.copy() : (isMelee(off) ? off.copy() : ItemStack.EMPTY);

        if (!bow.isEmpty()) {
            sk.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
            stashedBow.remove(id);
        }
        if (!melee.isEmpty()) {
            if (!isBow(melee)) sk.setItemSlot(EquipmentSlot.OFFHAND, melee);
        } else {
            if (isBow(off)) sk.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    @SuppressWarnings("unchecked")
    private void forceAttackMode(AbstractSkeleton sk, boolean melee) {
        sk.stopUsingItem();

        RangedBowAttackGoal<AbstractSkeleton> ranged = null;
        MeleeAttackGoal meleeGoal = null;

        for (WrappedGoal wg : sk.goalSelector.getAvailableGoals()) {
            Goal g = wg.getGoal();
            if (g instanceof RangedBowAttackGoal<?> rg) {
                ranged = (RangedBowAttackGoal<AbstractSkeleton>) rg;
            } else if (g instanceof MeleeAttackGoal mg) {
                meleeGoal = mg;
            }
        }

        if (ranged != null)   sk.goalSelector.removeGoal(ranged);
        if (meleeGoal != null) sk.goalSelector.removeGoal(meleeGoal);

        if (melee) {
            sk.goalSelector.addGoal(4, new NoPoseMeleeAttackGoal(sk, 1.2D, false));
        } else {
            sk.goalSelector.addGoal(4, new RangedBowAttackGoal<>(sk, 1.0D, 20, 15.0F));
        }

        sk.setAggressive(false);
        sk.getNavigation().stop();
    }

    private static class NoPoseMeleeAttackGoal extends MeleeAttackGoal {
        private final AbstractSkeleton skel;
        public NoPoseMeleeAttackGoal(AbstractSkeleton mob, double speed, boolean longMemory) {
            super(mob, speed, longMemory);
            this.skel = mob;
        }
        @Override public void start() { super.start(); skel.setAggressive(false); }
        @Override public void stop()  { super.stop();  skel.setAggressive(false); }
        @Override public void tick()  { super.tick();  skel.setAggressive(false); }
        @Override protected void checkAndPerformAttack(@NotNull LivingEntity t) {
            super.checkAndPerformAttack(t);
            skel.setAggressive(false);
        }
    }
}
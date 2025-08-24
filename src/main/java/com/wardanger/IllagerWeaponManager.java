package com.wardanger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import java.util.*;

public class IllagerWeaponManager {

    private static final Map<UUID, Boolean> meleeMode = new HashMap<>();
    private static final Map<UUID, Long> lastSwitchTick = new HashMap<>();
    private static final Map<UUID, ItemStack> stashedCrossbow = new HashMap<>();
    private static final Map<UUID, Long> lastModeChangeTick = new HashMap<>();

    private static final long SWITCH_COOLDOWN_TICKS = 20L; // 1s @20 TPS
    private static final long MODE_LOCK_TICKS = 120L;  // 6s egy módban
    private static final double MELEE_IN  = 6.0;
    private static final double MELEE_OUT = 12.0;

    private static final ResourceLocation OMINOUS_BANNER_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "ominous_banner");

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager)) return;
        if (pillager.level().isClientSide) return;

        final long now = pillager.level().getGameTime();
        final UUID id  = pillager.getUUID();

        // általános switch cooldown
        if (now - lastSwitchTick.getOrDefault(id, 0L) < SWITCH_COOLDOWN_TICKS) return;
        // min idő az aktuális módban
        if (now - lastModeChangeTick.getOrDefault(id, 0L) < MODE_LOCK_TICKS) return;

        updatePillager(pillager, now);
        lastSwitchTick.put(id, now);
    }

    private void updatePillager(Pillager mob, long nowTick) {
        boolean wantMelee = shouldGoMelee(mob, hasMeleeWeaponConsideringBanner(mob));
        UUID id = mob.getUUID();
        boolean curMelee = meleeMode.getOrDefault(id, false);

        if (curMelee != wantMelee) {
            if (wantMelee) switchToMelee(mob);
            else           switchToRanged(mob);
            meleeMode.put(id, wantMelee);
            lastModeChangeTick.put(id, nowTick);
        }
    }

    private void switchToMelee(Pillager mob) {
        UUID id = mob.getUUID();

        // crossbow → stash (offhandhez nem nyúlunk, ha banner)
        ItemStack main = mob.getMainHandItem();
        ItemStack off  = mob.getOffhandItem();

        if (isCrossbow(main)) {
            stashedCrossbow.put(id, main.copy());
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (!isOminousBanner(off) && isCrossbow(off)) {
            stashedCrossbow.put(id, off.copy());
            mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        // melee a mainhandbe, ha offhandben volt (és nem banner)
        main = mob.getMainHandItem();
        off  = mob.getOffhandItem();
        if (!isMelee(main) && isMelee(off) && !isOminousBanner(off)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, off.copy());
            mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        forceAttackMode_melee(mob);
    }

    private void switchToRanged(Pillager mob) {
        UUID id = mob.getUUID();

        // ha mainhandben melee van, próbáld megmenteni offhandbe (ha nem banner)
        ItemStack main = mob.getMainHandItem();
        ItemStack off  = mob.getOffhandItem();
        if (isMelee(main)) {
            if (isOminousBanner(off)) {
                // nincs hova menteni → maradjon melee módban
                forceAttackMode_melee(mob);
                return;
            }
            if (off.isEmpty() || isCrossbow(off)) {
                // ha offhand üres vagy crossbow van ott (nem banner), takaríts és tedd át
                if (isCrossbow(off)) mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                mob.setItemSlot(EquipmentSlot.OFFHAND, main.copy());
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }

        // crossbow vissza mainhandbe (stash előnyben)
        ItemStack bow = stashedCrossbow.getOrDefault(id, ItemStack.EMPTY);
        if (bow.isEmpty()) {
            if (isCrossbow(mob.getMainHandItem())) bow = mob.getMainHandItem().copy();
            else if (isCrossbow(mob.getOffhandItem()) && !isOminousBanner(mob.getOffhandItem())) bow = mob.getOffhandItem().copy();
        }
        if (!bow.isEmpty()) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, bow.copy());
            stashedCrossbow.remove(id);
        }

        // offhandből takarítsd ki a crossbow-t (bannerhez nem nyúlunk)
        if (!isOminousBanner(mob.getOffhandItem()) && isCrossbow(mob.getOffhandItem())) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        forceAttackMode_ranged(mob);
    }

    // ===== GOAL / STATE KEZELÉS =====

    private void forceAttackMode_melee(Pillager mob) {
        mob.stopUsingItem();
        mob.setChargingCrossbow(false);
        mob.getNavigation().stop();
        mob.setAggressive(true);

        // távolíts el MINDEN RangedCrossbowAttackGoal és MeleeAttackGoal példányt
        removeAllGoals(mob, RangedCrossbowAttackGoal.class);
        removeAllGoals(mob, MeleeAttackGoal.class);

        // add csak a melee-t
        mob.goalSelector.addGoal(4, new MeleeAttackGoal(mob, 1.2D, false));
    }

    private void forceAttackMode_ranged(Pillager mob) {
        mob.stopUsingItem();
        mob.setChargingCrossbow(false);
        mob.getNavigation().stop();
        mob.setAggressive(false);

        removeAllGoals(mob, RangedCrossbowAttackGoal.class);
        removeAllGoals(mob, MeleeAttackGoal.class);

        mob.goalSelector.addGoal(4, new RangedCrossbowAttackGoal<>(mob, 1.0D, 15.0F));
    }

    private void removeAllGoals(Pillager mob, Class<? extends Goal> type) {
        // collect, majd mindet vedd le
        List<WrappedGoal> toRemove = new ArrayList<>();
        for (WrappedGoal wg : mob.goalSelector.getAvailableGoals()) {
            if (type.isInstance(wg.getGoal())) {
                toRemove.add(wg);
            }
        }
        for (WrappedGoal wg : toRemove) {
            mob.goalSelector.removeGoal(wg.getGoal());
        }
    }

    // ===== DÖNTÉS =====

    private boolean shouldGoMelee(Pillager mob, boolean hasMelee) {
        if (!hasMelee) return false;

        LivingEntity tgt = mob.getTarget();
        boolean nowMelee = meleeMode.getOrDefault(mob.getUUID(), false);

        if (tgt == null || !tgt.isAlive()) {
            List<Player> players = mob.level().getEntitiesOfClass(
                    Player.class, mob.getBoundingBox().inflate(MELEE_IN), EntitySelector.NO_SPECTATORS);
            return !players.isEmpty();
        }

        double d = mob.distanceTo(tgt);
        if (!nowMelee) {
            return mob.hasLineOfSight(tgt) && d <= MELEE_IN;
        } else {
            return !(d <= MELEE_OUT);
        }
    }

    private static boolean isMelee(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem);
    }

    private static boolean isCrossbow(ItemStack s) {
        return !s.isEmpty() && (s.getItem() instanceof CrossbowItem);
    }

    private static boolean isOminousBanner(ItemStack s) {
        if (s.isEmpty()) return false;
        return OMINOUS_BANNER_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    private static boolean hasMeleeWeaponConsideringBanner(Pillager m) {
        if (isMelee(m.getMainHandItem())) return true;
        return isMelee(m.getOffhandItem()) && !isOminousBanner(m.getOffhandItem());
    }
}
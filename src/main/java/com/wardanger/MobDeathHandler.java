package com.wardanger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Iterator;

public class MobDeathHandler {

    @SubscribeEvent
    public void onMobDeath(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel)) {
            return;
        }

        if (event.getEntity() instanceof Mob mob && mob instanceof Monster) {
            Iterator<ItemEntity> iterator = event.getDrops().iterator();

            while (iterator.hasNext()) {
                ItemEntity itemEntity = iterator.next();
                ItemStack itemStack = itemEntity.getItem();

                if (hasDangerousTag(itemStack)) {
                    iterator.remove();
                }
            }
        }
    }

    private static boolean hasDangerousTag(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        return customData.copyTag().getBoolean("dangerous_equipment_modified");
    }
}
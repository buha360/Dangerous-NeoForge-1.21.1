package com.wardanger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class SpiderSpeedEnhancement {
    private static final String SPIDER_BUFF_TAG = "dangerous_spider_buff";

    @SubscribeEvent
    public void onSpiderSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Spider spider && event.getLevel() instanceof ServerLevel) {
            CompoundTag entityData = spider.getPersistentData();

            if (!entityData.getBoolean(SPIDER_BUFF_TAG)) {
                increaseSpiderSpeed(spider);
                entityData.putBoolean(SPIDER_BUFF_TAG, true);
            }
        }
    }

    private static void increaseSpiderSpeed(Spider spider) {
        double speedMultiplier = DangerousConfig.CONFIG.spiderSpeedMultiplier.get();

        AttributeInstance speedAttribute = spider.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(speedAttribute.getBaseValue() * speedMultiplier);
        }
    }
}
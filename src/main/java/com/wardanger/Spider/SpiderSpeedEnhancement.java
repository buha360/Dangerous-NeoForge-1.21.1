package com.wardanger.Spider;

import com.wardanger.DangerousConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class SpiderSpeedEnhancement {
    private static final String SPIDER_BUFF_TAG = "dangerous_spider_buff";
    private static final int SPIDER_VENOM_DURATION_TICKS = 300; // 15s @ 20tps
    private static final double SPIDER_VENOM_REGEN_MULTIPLIER = 0.30;
    private static final double SPIDER_VENOM_APPLY_CHANCE = 0.30; // 30%

    @SubscribeEvent
    public void onSpiderAttackApplyVenom(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        if (!(source.getEntity() instanceof Spider)) {
            return;
        }

        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        if (level.random.nextDouble() >= SPIDER_VENOM_APPLY_CHANCE) {
            return;
        }

        target.addEffect(new MobEffectInstance(
                SpiderEffect.SPIDER_VENOM,
                SPIDER_VENOM_DURATION_TICKS,
                0,
                false,
                true
        ));
    }

    @SubscribeEvent
    public void onLivingHealReduceWithSpiderVenom(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();

        MobEffectInstance effect = entity.getEffect(SpiderEffect.SPIDER_VENOM);
        if (effect == null) return;

        float original = event.getAmount();
        float reduced = (float)(original * SPIDER_VENOM_REGEN_MULTIPLIER);

        if (reduced < 0.01f) reduced = 0.01f;

        event.setAmount(reduced);
    }

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
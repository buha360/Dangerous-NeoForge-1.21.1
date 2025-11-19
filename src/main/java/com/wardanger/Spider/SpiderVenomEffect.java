package com.wardanger.Spider;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

public class SpiderVenomEffect extends MobEffect {

    public SpiderVenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E8B57);
    }

    public boolean shouldRenderInvText(MobEffectInstance effect) {
        return true;
    }

    public boolean shouldRenderIcon(MobEffectInstance effect) {
        return true;
    }

    public boolean shouldRender(MobEffectInstance effect) {
        return true;
    }
}
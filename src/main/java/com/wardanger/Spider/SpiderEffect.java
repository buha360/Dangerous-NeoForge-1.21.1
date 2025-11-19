package com.wardanger.Spider;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.*;

public class SpiderEffect {

    public static final String MODID = "dangerous";

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SPIDER_VENOM =
            EFFECTS.register("spider_venom", SpiderVenomEffect::new);

    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}


package com.wardanger;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

@Mod(Dangerous.MODID)
public class Dangerous {

    public static final String MODID = "dangerous";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String HEALTH_MODIFIED_TAG = "dangerous_hp_modified";
    private float healthMultiplier = 1.0f;
    private double maxHealthMultiplier;
    private double healthMultiplierIncrement;
    private int daysPerIncrement;
    private static long lastCheckedDay = 0;
    private boolean finalFormReached = false;

    public Dangerous(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, DangerousConfig.COMMON_SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new SkeletonWeaponManager());
        NeoForge.EVENT_BUS.register(new IllagerWeaponManager());
        NeoForge.EVENT_BUS.register(new MobDeathHandler());
        NeoForge.EVENT_BUS.register(new SpiderSpeedEnhancement());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Dangerous Mod setup complete.");
    }

    public void onConfigLoad(final ModConfigEvent.Loading event) {
        loadConfigValues();
    }

    public void onConfigReload(final ModConfigEvent.Reloading event) {
        loadConfigValues();
    }

    private void loadConfigValues() {
        healthMultiplier = DangerousConfig.CONFIG.baseHealthMultiplier.get().floatValue();
        maxHealthMultiplier = DangerousConfig.CONFIG.maxHealthMultiplier.get();
        healthMultiplierIncrement = DangerousConfig.CONFIG.healthMultiplierIncrement.get();
        daysPerIncrement = DangerousConfig.CONFIG.daysPerIncrement.get();
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel serverWorld) {
            boolean isFirstJoin = player.getPersistentData().getBoolean("dangerous_first_join");

            if (!isFirstJoin) {
                player.getPersistentData().putBoolean("dangerous_first_join", true);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendWorldInfoToPlayer(player, serverWorld);
                    }
                }, 2500);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().putBoolean("dangerous_first_join", false);
        }
    }

    private void sendWorldInfoToPlayer(ServerPlayer player, ServerLevel world) {
        long currentDay = world.getDayTime() / 24000L;
        double healthMultiplier = DangerousConfig.CONFIG.baseHealthMultiplier.get().floatValue();
        int daysPerIncrement = DangerousConfig.CONFIG.daysPerIncrement.get();

        long daysUntilNextIncrement = daysPerIncrement - (currentDay % daysPerIncrement);

        Component dangerousMessage = Component.literal("            === Dangerous Mod Stats ===")
                .withStyle(style -> style.withBold(true).withColor(0xFF5555));

        Component emptyLine = Component.literal(" ");

        Component combinedMessage = Component.literal("Day: ")
                .append(Component.literal("" + currentDay).withStyle(style -> style.withColor(0x00FF00)))
                .append(Component.literal(" | Mob HP Multiplier: "))
                .append(Component.literal("" + healthMultiplier).withStyle(style -> style.withColor(0xFFFF55)))
                .append(Component.literal(" | Days Until Next Increment: "))
                .append(Component.literal("" + daysUntilNextIncrement).withStyle(style -> style.withColor(0x55FFFF)));

        if (DangerousConfig.CONFIG.enableChatAnnouncements.get()) {
            player.sendSystemMessage(dangerousMessage);
            player.sendSystemMessage(emptyLine);
            player.sendSystemMessage(combinedMessage);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Dangerous Mod is ready and loaded!");
        loadConfigValues();

        ServerLevel overworld = event.getServer().getLevel(ServerLevel.OVERWORLD);
        if (overworld != null) {
            long currentDay = overworld.getDayTime() / 24000L;
            initializeHealthMultiplier(currentDay);
        }
    }

    private void initializeHealthMultiplier(long currentDay) {
        long daysPassed = currentDay / daysPerIncrement;
        for (int i = 0; i < daysPassed; i++) {
            if (healthMultiplier < maxHealthMultiplier) {
                healthMultiplier += (float) healthMultiplierIncrement;
                if (healthMultiplier > maxHealthMultiplier) {
                    healthMultiplier = (float) maxHealthMultiplier;
                    finalFormReached = true;
                    break;
                }
            }
        }
        lastCheckedDay = currentDay;
    }

    @SubscribeEvent
    public void onWorldTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverWorld && !serverWorld.isClientSide()) {
            ServerLevelData worldData = (ServerLevelData) serverWorld.getServer().getWorldData();
            long currentDay = worldData.getDayTime() / 24000;

            if (lastCheckedDay == 0) {
                lastCheckedDay = currentDay;
            }

            long daysPassed = currentDay - lastCheckedDay;

            if (daysPassed >= daysPerIncrement) {
                long increments = daysPassed / daysPerIncrement;
                for (long i = 0; i < increments; i++) {
                    increaseHealthMultiplier(serverWorld);
                }
                lastCheckedDay += increments * daysPerIncrement;
            }
        }
    }

    private void increaseHealthMultiplier(ServerLevel world) {
        if (healthMultiplier < maxHealthMultiplier) {
            healthMultiplier += (float) healthMultiplierIncrement;
            if (healthMultiplier > maxHealthMultiplier) {
                healthMultiplier = (float) maxHealthMultiplier;
            }

            if (DangerousConfig.CONFIG.enableChatAnnouncements.get()) {
                Component message = Component.literal("The enemies become more DANGEROUS!")
                        .withStyle(style -> style.withBold(true).withColor(0xFF5555));

                world.getPlayers(player -> true).forEach(player -> player.sendSystemMessage(message));

                if (healthMultiplier == maxHealthMultiplier && !finalFormReached) {
                    finalFormReached = true;
                    Component finalMessage = Component.literal("The enemies have reached their FINAL FORM!")
                            .withStyle(style -> style.withBold(true).withColor(0xFFAA00));

                    world.getPlayers(player -> true).forEach(player -> player.sendSystemMessage(finalMessage));
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverWorld && event.getEntity() instanceof Mob mob) {
            if (mob instanceof Monster) {
                Difficulty difficulty = serverWorld.getDifficulty();
                adjustHealthBasedOnDifficulty(mob, difficulty, healthMultiplier);
                GearManager.equipMobBasedOnDifficulty(mob, serverWorld);
            }
        }
    }

    private void adjustHealthBasedOnDifficulty(LivingEntity entity, Difficulty difficulty, float multiplier) {
        CompoundTag entityData = entity.getPersistentData();
        if (!entityData.getBoolean(HEALTH_MODIFIED_TAG)) {
            float originalHealth = entity.getMaxHealth();
            float newHealth;

            switch (difficulty) {
                case EASY, NORMAL, HARD -> newHealth = originalHealth * multiplier;
                default -> newHealth = originalHealth;
            }

            Objects.requireNonNull(entity.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(newHealth);
            entity.setHealth(newHealth);

            entityData.putBoolean(HEALTH_MODIFIED_TAG, true);
        }
    }

    @EventBusSubscriber(modid = Dangerous.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Dangerous Mod client setup complete.");
        }
    }
}
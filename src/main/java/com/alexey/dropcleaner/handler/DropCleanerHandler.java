package com.alexey.dropcleaner.handler;

import com.alexey.dropcleaner.config.DropCleanerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DropCleanerHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("DropCleaner");
    private int currentTick = 0;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        DropCleanerConfig config = DropCleanerConfig.get();
        currentTick++;

        // Проверяем раз в секунду (каждые 20 тиков)
        if (currentTick % 20 == 0) {
            int elapsedSeconds = currentTick / 20;
            int remainingSeconds = config.intervalSeconds - elapsedSeconds;

            if (config.broadcastWarnings && config.warningSeconds.contains(remainingSeconds)) {
                broadcastWarning(server, remainingSeconds);
            }

            if (remainingSeconds <= 0) {
                performCleanup(server, false);
                currentTick = 0;
            }
        }
    }

    public void resetTimer() {
        this.currentTick = 0;
    }

    public int getRemainingSeconds() {
        DropCleanerConfig config = DropCleanerConfig.get();
        int elapsedSeconds = currentTick / 20;
        return Math.max(0, config.intervalSeconds - elapsedSeconds);
    }

    public CleanupResult performCleanup(MinecraftServer server, boolean manual) {
        return performCleanup(server, manual, false);
    }

    public CleanupResult performCleanup(MinecraftServer server, boolean manual, boolean forceAll) {
        DropCleanerConfig config = DropCleanerConfig.get();
        int totalItemsRemoved = 0;
        int totalEntitiesRemoved = 0;
        int totalXpOrbsRemoved = 0;

        // При ручной очистке (/clearlag или /cleardrop) возраст предметов НЕ проверяется (0 сек)
        // — очищаются 100% всех лежащих на земле блоков сразу!
        // При автоочистке берётся значение из конфига (по умолчанию 0 сек).
        int minAgeTicks = manual ? 0 : (config.minimumItemAgeSeconds * 20);

        List<Entity> toRemove = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            toRemove.clear();
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved()) continue;

                if (entity instanceof ItemEntity itemEntity) {
                    if (forceAll || shouldClean(itemEntity, minAgeTicks, config)) {
                        toRemove.add(itemEntity);
                    }
                } else if (entity instanceof ExperienceOrb xpOrb) {
                    if (forceAll || config.cleanExperienceOrbs) {
                        toRemove.add(xpOrb);
                    }
                }
            }

            for (Entity entity : toRemove) {
                totalEntitiesRemoved++;
                if (entity instanceof ItemEntity itemEntity) {
                    totalItemsRemoved += itemEntity.getItem().getCount();
                } else if (entity instanceof ExperienceOrb) {
                    totalXpOrbsRemoved++;
                }
                entity.discard();
            }
        }

        CleanupResult result = new CleanupResult(totalEntitiesRemoved, totalItemsRemoved, totalXpOrbsRemoved, forceAll);

        if (config.broadcastCleanup || manual) {
            broadcastResult(server, result);
        }

        LOGGER.info("[DropCleaner] Очистка завершена: удалено {} сущностей (всего {} предметов, {} сфер опыта, force={})",
                totalEntitiesRemoved, totalItemsRemoved, totalXpOrbsRemoved, forceAll);

        return result;
    }

    private boolean shouldClean(ItemEntity itemEntity, int minAgeTicks, DropCleanerConfig config) {
        // Проверяем возраст только если minAgeTicks > 0 и возраст неотрицательный
        if (minAgeTicks > 0 && itemEntity.getAge() >= 0 && itemEntity.getAge() < minAgeTicks) {
            return false;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack == null || stack.isEmpty()) return true;

        if (config.protectNamedItems) {
            if (itemEntity.hasCustomName() || stack.has(DataComponents.CUSTOM_NAME)) {
                return false;
            }
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null) {
            String id = key.toString();
            String path = key.getPath();

            if (config.protectNetherite && path.contains("netherite")) {
                return false;
            }

            if (config.protectShulkerBoxes && path.contains("shulker_box")) {
                return false;
            }

            if (config.protectedItems.contains(id)) {
                return false;
            }
        }

        return true;
    }

    private void broadcastWarning(MinecraftServer server, int remainingSeconds) {
        String timeStr;
        if (remainingSeconds >= 60) {
            int minutes = remainingSeconds / 60;
            timeStr = minutes + (minutes == 1 ? " минуту" : (minutes < 5 ? " минуты" : " минут"));
        } else {
            timeStr = remainingSeconds + (remainingSeconds == 1 ? " секунду" : (remainingSeconds < 5 ? " секунды" : " секунд"));
        }

        Component msg = Component.literal("[Очистка] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Лежащие на полу предметы будут удалены через ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(timeStr + "!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private void broadcastResult(MinecraftServer server, CleanupResult result) {
        var builder = Component.literal("[Очистка] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Очищено предметов на земле: ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(result.totalItems() + " шт. ")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("(" + result.totalEntities() + " пачек/стаков" +
                        (result.totalXpOrbs() > 0 ? ", " + result.totalXpOrbs() + " сфер опыта" : "") +
                        ").")
                        .withStyle(ChatFormatting.GRAY));

        if (result.force()) {
            builder.append(Component.literal(" [FORCE]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        server.getPlayerList().broadcastSystemMessage(builder, false);
    }

    public record CleanupResult(int totalEntities, int totalItems, int totalXpOrbs, boolean force) {}
}

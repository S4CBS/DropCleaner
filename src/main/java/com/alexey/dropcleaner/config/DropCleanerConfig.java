package com.alexey.dropcleaner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DropCleanerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("DropCleaner");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("dropcleaner.json");
    private static DropCleanerConfig INSTANCE = new DropCleanerConfig();

    public int intervalSeconds = 600; // 10 минут
    public int minimumItemAgeSeconds = 15; // предметы моложе 15 сек не удалять
    public List<Integer> warningSeconds = new ArrayList<>(List.of(60, 30, 10, 5, 3, 2, 1));
    public boolean broadcastWarnings = true;
    public boolean broadcastCleanup = true;
    public boolean protectNamedItems = true;
    public boolean protectNetherite = true;
    public boolean protectShulkerBoxes = true;
    public List<String> protectedItems = new ArrayList<>(List.of(
            "minecraft:nether_star",
            "minecraft:dragon_egg",
            "minecraft:elytra",
            "minecraft:beacon",
            "minecraft:totem_of_undying"
    ));

    public static DropCleanerConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    DropCleanerConfig loaded = GSON.fromJson(reader, DropCleanerConfig.class);
                    if (loaded != null) {
                        INSTANCE = loaded;
                        INSTANCE.validate();
                        LOGGER.info("[DropCleaner] Конфигурация успешно загружена из {}", CONFIG_PATH);
                        return;
                    }
                }
            }
            INSTANCE = new DropCleanerConfig();
            save();
        } catch (Exception e) {
            LOGGER.error("[DropCleaner] Ошибка загрузки конфига, используются значения по умолчанию", e);
            INSTANCE = new DropCleanerConfig();
        }
    }

    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
            LOGGER.info("[DropCleaner] Конфигурация сохранена в {}", CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("[DropCleaner] Не удалось сохранить конфигурацию", e);
        }
    }

    public void validate() {
        if (intervalSeconds < 10) intervalSeconds = 10;
        if (minimumItemAgeSeconds < 0) minimumItemAgeSeconds = 0;
        if (warningSeconds == null) warningSeconds = new ArrayList<>(List.of(60, 30, 10));
        if (protectedItems == null) protectedItems = new ArrayList<>();
    }
}

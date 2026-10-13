package com.alexey.dropcleaner;

import com.alexey.dropcleaner.command.DropCleanerCommand;
import com.alexey.dropcleaner.config.DropCleanerConfig;
import com.alexey.dropcleaner.handler.DropCleanerHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DropCleanerMod.MOD_ID)
public class DropCleanerMod {
    public static final String MOD_ID = "dropcleaner";
    public static final Logger LOGGER = LoggerFactory.getLogger("DropCleaner");

    private final DropCleanerHandler handler;

    public DropCleanerMod(IEventBus modEventBus) {
        LOGGER.info("[DropCleaner] Инициализация мода очистки дропа (NeoForge 1.21.1)...");
        DropCleanerConfig.load();
        this.handler = new DropCleanerHandler();
        NeoForge.EVENT_BUS.register(this.handler);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("[DropCleaner] Мод успешно загружен! Интервал очистки: {} мин.", DropCleanerConfig.get().intervalSeconds / 60);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DropCleanerCommand.register(event.getDispatcher(), this.handler);
    }
}

package com.alexey.dropcleaner.command;

import com.alexey.dropcleaner.config.DropCleanerConfig;
import com.alexey.dropcleaner.handler.DropCleanerHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DropCleanerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, DropCleanerHandler handler) {
        // /clearlag, /cleardrop, /dropclean — это идентичные синонимы (алиасы) для удобства
        registerRoot(dispatcher, "clearlag", handler);
        registerRoot(dispatcher, "cleardrop", handler);
        registerRoot(dispatcher, "dropclean", handler);
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name, DropCleanerHandler handler) {
        dispatcher.register(Commands.literal(name)
                // Если команда вызвана без аргументов: /clearlag или /cleardrop
                // СРАЗУ запускаем 100% очистку всех лежащих блоков и предметов на земле!
                .requires(src -> src.hasPermission(2) || src.getServer().isSingleplayer())
                .executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    var result = handler.performCleanup(server, true, false);
                    handler.resetTimer();
                    return result.totalItems();
                })
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true, false);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("now")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true, false);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("force")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true, true);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("all")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true, true);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("time")
                        .requires(src -> true)
                        .executes(ctx -> {
                            int remaining = handler.getRemainingSeconds();
                            int min = remaining / 60;
                            int sec = remaining % 60;
                            Component msg = Component.literal("[Очистка] ")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                    .append(Component.literal("До следующей автоочистки дропа осталось: ")
                                            .withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(min + " мин. " + sec + " сек.")
                                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                            ctx.getSource().sendSuccess(() -> msg, false);
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            DropCleanerConfig.load();
                            Component msg = Component.literal("[Очистка] ")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                    .append(Component.literal("Конфигурация dropcleaner.json успешно перезагружена!")
                                            .withStyle(ChatFormatting.GREEN));
                            ctx.getSource().sendSuccess(() -> msg, true);
                            return 1;
                        }))
                .then(Commands.literal("help")
                        .requires(src -> true)
                        .executes(ctx -> {
                            sendHelp(ctx.getSource(), name);
                            return 1;
                        }))
        );
    }

    private static void sendHelp(CommandSourceStack source, String root) {
        Component help = Component.literal("--- [DropCleaner - Команды] ---\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("/" + root).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" - очистить 100% лежащих блоков и предметов прямо сейчас\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/" + root + " force").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" - принудительно очистить ВСЁ (включая незерит и опыт)\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/" + root + " time").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - узнать время до следующей автоочистки\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/" + root + " reload").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - перезагрузить конфигурацию dropcleaner.json\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("Подсказка: команды /clearlag и /cleardrop полностью одинаковы (синонимы).").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        source.sendSuccess(() -> help, false);
    }
}

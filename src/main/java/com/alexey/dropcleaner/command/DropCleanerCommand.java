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
        dispatcher.register(buildCommand("dropclean", handler));
        dispatcher.register(buildCommand("cleardrop", handler));
        dispatcher.register(buildCommand("clearlag", handler));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(String name, DropCleanerHandler handler) {
        return Commands.literal(name)
                .executes(ctx -> {
                    sendHelp(ctx.getSource(), name);
                    return 1;
                })
                .then(Commands.literal("time")
                        .executes(ctx -> {
                            int remaining = handler.getRemainingSeconds();
                            int min = remaining / 60;
                            int sec = remaining % 60;
                            Component msg = Component.literal("[Очистка] ")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                    .append(Component.literal("До следующей очистки дропа осталось: ")
                                            .withStyle(ChatFormatting.YELLOW))
                                    .append(Component.literal(min + " мин. " + sec + " сек.")
                                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                            ctx.getSource().sendSuccess(() -> msg, false);
                            return 1;
                        }))
                .then(Commands.literal("clear")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("now")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var result = handler.performCleanup(server, true);
                            handler.resetTimer();
                            return result.totalItems();
                        }))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            DropCleanerConfig.load();
                            Component msg = Component.literal("[Очистка] ")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                    .append(Component.literal("Конфигурация dropcleaner.json успешно перезагружена!")
                                            .withStyle(ChatFormatting.GREEN));
                            ctx.getSource().sendSuccess(() -> msg, true);
                            return 1;
                        }));
    }

    private static void sendHelp(CommandSourceStack source, String root) {
        Component help = Component.literal("--- [DropCleaner - Команды] ---\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("/" + root + " time").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - узнать время до следующей очистки\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/" + root + " clear").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - запустить очистку немедленно\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/" + root + " reload").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" - перезагрузить конфигурацию").withStyle(ChatFormatting.WHITE));
        source.sendSuccess(() -> help, false);
    }
}

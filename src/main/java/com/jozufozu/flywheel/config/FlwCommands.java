package com.jozufozu.flywheel.config;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.ModList;

public class FlwCommands {
	public static void registerClientCommands(RegisterClientCommandsEvent event) {
		FlwConfig config = FlwConfig.get();

		ConfigCommandBuilder commandBuilder = new ConfigCommandBuilder("flywheel");

		commandBuilder.addValue(config.client.backend, "backend", (builder, value) ->
			builder
				.executes(context -> {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player != null) {
						player.displayClientMessage(getEngineMessage(value.get()), false);
					}
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("type", BackendTypeArgument.INSTANCE)
					.executes(context -> {
						LocalPlayer player = Minecraft.getInstance().player;
						if (player != null) {
							BackendType type = context.getArgument("type", BackendType.class);
							value.set(type);

							Component message = getEngineMessage(type);
							player.displayClientMessage(message, false);

							Backend.reloadWorldRenderers();
						}
						return Command.SINGLE_SUCCESS;
					})));

		commandBuilder.addValue(config.client.debugNormals, "debugNormals", (builder, value) -> booleanValueCommand(builder, value,
				(source, bool) -> {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player == null) return;

					Component text = Component.translatable("Normal debug mode is currently: ").append(boolToText(bool));
					player.displayClientMessage(text, false);
				},
				(source, bool) -> {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player == null) return;

					Component text = boolToText(bool).append(Component.translatable(" normal debug mode").withStyle(ChatFormatting.WHITE));
					player.displayClientMessage(text, false);
				}
			));

		commandBuilder.addValue(config.client.limitUpdates, "limitUpdates", (builder, value) -> booleanValueCommand(builder, value,
				(source, bool) -> {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player == null) return;

					Component text = Component.translatable("Update limiting is currently: ").append(boolToText(bool));
					player.displayClientMessage(text, false);
				},
				(source, bool) -> {
					LocalPlayer player = Minecraft.getInstance().player;
					if (player == null) return;

					Component text = boolToText(bool).append(Component.translatable(" update limiting.").withStyle(ChatFormatting.WHITE));
					player.displayClientMessage(text, false);

					Backend.reloadWorldRenderers();
				}
			));

		commandBuilder.build(event.getDispatcher());
	}

	public static void booleanValueCommand(LiteralArgumentBuilder<CommandSourceStack> builder, ConfigValue<Boolean> value, BiConsumer<CommandSourceStack, Boolean> displayAction, BiConsumer<CommandSourceStack, Boolean> setAction) {
		builder
			.executes(context -> {
				displayAction.accept(context.getSource(), value.get());
				return Command.SINGLE_SUCCESS;
			})
			.then(Commands.literal("on")
				.executes(context -> {
					value.set(true);
					setAction.accept(context.getSource(), value.get());
					return Command.SINGLE_SUCCESS;
				}))
			.then(Commands.literal("off")
				.executes(context -> {
					value.set(false);
					setAction.accept(context.getSource(), value.get());
					return Command.SINGLE_SUCCESS;
				}));
	}

	public static MutableComponent boolToText(boolean b) {
		return b ? Component.translatable("enabled").withStyle(ChatFormatting.DARK_GREEN) : Component.translatable("disabled").withStyle(ChatFormatting.RED);
	}

	public static Component getEngineMessage(@NotNull BackendType type) {
		return switch (type) {
			case OFF -> Component.translatable("Disabled Flywheel").withStyle(ChatFormatting.RED);
			case INSTANCING -> Component.translatable("Using Instancing Engine").withStyle(ChatFormatting.GREEN);
			case BATCHING -> {
				MutableComponent msg = Component.translatable("Using Batching Engine").withStyle(ChatFormatting.GREEN);

				if (ModList.get()
						.isLoaded("create")) {
					// FIXME: batching engine contraption lighting issues
					msg.append(Component.translatable("\nWARNING: May cause issues with Create Contraptions").withStyle(ChatFormatting.RED));
				}

				yield msg;
			}
		};
	}

	public static class ConfigCommandBuilder {
		protected LiteralArgumentBuilder<CommandSourceStack> command;

		public ConfigCommandBuilder(String baseLiteral) {
			command = Commands.literal(baseLiteral);
		}

		public <T extends ConfigValue<?>> void addValue(T value, String subcommand, BiConsumer<LiteralArgumentBuilder<CommandSourceStack>, T> consumer) {
			LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(subcommand);
			consumer.accept(builder, value);
			command.then(builder);
		}

		public void build(CommandDispatcher<CommandSourceStack> dispatcher) {
			dispatcher.register(command);
		}
	}
}

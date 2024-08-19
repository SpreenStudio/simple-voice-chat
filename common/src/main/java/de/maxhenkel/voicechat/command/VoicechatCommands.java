package de.maxhenkel.voicechat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.permission.Permission;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import de.maxhenkel.voicechat.voice.server.ClientConnection;
import de.maxhenkel.voicechat.voice.server.Group;
import de.maxhenkel.voicechat.voice.server.PingManager;
import de.maxhenkel.voicechat.voice.server.Server;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoicechatCommands {

    public static final String VOICECHAT_COMMAND = "voicechat";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalBuilder = Commands.literal(VOICECHAT_COMMAND);

        literalBuilder.executes(commandSource -> help(dispatcher, commandSource));
        literalBuilder.then(Commands.literal("help").executes(commandSource -> help(dispatcher, commandSource)));

        literalBuilder.then(Commands.literal("test").requires((commandSource) -> checkPermission(commandSource, PermissionManager.INSTANCE.ADMIN_PERMISSION)).then(Commands.argument("target", EntityArgument.player()).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            ServerPlayer player = EntityArgument.getPlayer(commandSource, "target");
            Server server = Voicechat.SERVER.getServer();
            if (server == null) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.voice_chat_unavailable"), false);
                return 1;
            }

            if (!Voicechat.SERVER.isCompatible(player)) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.player_no_voicechat", player.getDisplayName(), CommonCompatibilityManager.INSTANCE.getModName()), false);
                return 1;
            }

            ClientConnection clientConnection = server.getConnections().get(player.getUUID());
            if (clientConnection == null) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.client_not_connected"), false);
                return 1;
            }
            try {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.sending_ping"), false);

                server.getPingManager().sendPing(clientConnection, 500, 10, new PingManager.PingListener() {

                    @Override
                    public void onPong(int attempts, long pingMilliseconds) {
                        if (attempts <= 1) {
                            commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.ping_received", pingMilliseconds), false);
                        } else {
                            commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.ping_received_attempt", attempts, pingMilliseconds), false);
                        }
                    }

                    @Override
                    public void onFailedAttempt(int attempts) {
                        commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.ping_retry"), false);
                    }

                    @Override
                    public void onTimeout(int attempts) {
                        commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.ping_timed_out", attempts), false);
                    }
                });
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.ping_sent_waiting"), false);
            } catch (Exception e) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.failed_to_send_ping", e.getMessage()), false);
                Voicechat.LOGGER.warn("Failed to send ping", e);
                return 1;
            }
            return 1;
        })));

        literalBuilder.then(Commands.literal("modify").requires((context) -> checkPermission(context, PermissionManager.INSTANCE.ADMIN_PERMISSION))
                .then(Commands.argument("type", StringArgumentType.string())
                        .then(Commands.argument("value", StringArgumentType.string())
                                .executes(context -> modify(context))
                        )
                )
        );

        literalBuilder.then(Commands.literal("invite").then(Commands.argument("target", EntityArgument.player()).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            ServerPlayer source = commandSource.getSource().getPlayerOrException();

            Server server = Voicechat.SERVER.getServer();
            if (server == null) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.voice_chat_unavailable"), false);
                return 1;
            }

            PlayerState state = server.getPlayerStateManager().getState(source.getUUID());

            if (state == null || !state.hasGroup()) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.not_in_group"), false);
                return 1;
            }

            ServerPlayer player = EntityArgument.getPlayer(commandSource, "target");
            Group group = server.getGroupManager().getGroup(state.getGroup());

            if (group == null) {
                return 1;
            }

            if (!Voicechat.SERVER.isCompatible(player)) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.player_no_voicechat", player.getDisplayName(), CommonCompatibilityManager.INSTANCE.getModName()), false);
                return 1;
            }

            String passwordSuffix = group.getPassword() == null ? "" : " \"" + group.getPassword() + "\"";
            player.sendSystemMessage(Component.translatable("message.voicechat.invite", source.getDisplayName(), Component.literal(group.getName()).withStyle(ChatFormatting.GRAY), ComponentUtils.wrapInSquareBrackets(Component.translatable("message.voicechat.accept_invite").withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/voicechat join " + group.getId().toString() + passwordSuffix)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.voicechat.accept_invite.hover"))))).withStyle(ChatFormatting.GREEN)));

            commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.invite_successful", player.getDisplayName()), false);

            return 1;
        })));

        literalBuilder.then(Commands.literal("join").then(Commands.argument("group_id", UuidArgument.uuid()).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            UUID groupID = UuidArgument.getUuid(commandSource, "group_id");
            return joinGroupById(commandSource.getSource(), groupID, null);
        })));

        literalBuilder.then(Commands.literal("join").then(Commands.argument("group_id", UuidArgument.uuid()).then(Commands.argument("password", StringArgumentType.string()).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            UUID groupID = UuidArgument.getUuid(commandSource, "group_id");
            String password = StringArgumentType.getString(commandSource, "password");
            return joinGroupById(commandSource.getSource(), groupID, password.isEmpty() ? null : password);
        }))));

        literalBuilder.then(Commands.literal("join").then(Commands.argument("group_name", StringArgumentType.string()).suggests(GroupNameSuggestionProvider.INSTANCE).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            String groupName = StringArgumentType.getString(commandSource, "group_name");
            return joinGroupByName(commandSource.getSource(), groupName, null);
        })));

        literalBuilder.then(Commands.literal("join").then(Commands.argument("group_name", StringArgumentType.string()).suggests(GroupNameSuggestionProvider.INSTANCE).then(Commands.argument("password", StringArgumentType.string()).executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            String groupName = StringArgumentType.getString(commandSource, "group_name");
            String password = StringArgumentType.getString(commandSource, "password");
            return joinGroupByName(commandSource.getSource(), groupName, password.isEmpty() ? null : password);
        }))));

        literalBuilder.then(Commands.literal("leave").executes((commandSource) -> {
            if (checkNoVoicechat(commandSource)) {
                return 0;
            }
            if (!Voicechat.SERVER_CONFIG.groupsEnabled.get()) {
                commandSource.getSource().sendFailure(Component.translatable("message.voicechat.groups_disabled"));
                return 1;
            }

            Server server = Voicechat.SERVER.getServer();
            if (server == null) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.voice_chat_unavailable"), false);
                return 1;
            }
            ServerPlayer source = commandSource.getSource().getPlayerOrException();

            PlayerState state = server.getPlayerStateManager().getState(source.getUUID());
            if (state == null || !state.hasGroup()) {
                commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.not_in_group"), false);
                return 1;
            }

            server.getGroupManager().leaveGroup(source);
            commandSource.getSource().sendSuccess(() -> Component.translatable("message.voicechat.leave_successful"), false);
            return 1;
        }));

        dispatcher.register(literalBuilder);
    }

    private static int modify(CommandContext<CommandSourceStack> context) {
        if (checkNoVoicechat(context)) {
            sendMSG(context, "Voicechat is not available");
            return 0;
        }
        String type = StringArgumentType.getString(context, "type");
        String value = StringArgumentType.getString(context, "value");

        List<String> allowed = List.of("allow_recording", "spectator_interaction", "spectator_player_possession", "force_voice_chat", "login_timeout", "broadcast_range", "allow_pings", "max_voice_distance", "crouch_distance_multiplier", "whisper_distance_multiplier", "enable_groups");
        if (!allowed.contains(type)) {
            sendMSG(context, "Invalid type");
            return 0;
        }
        try {
            switch (allowed.indexOf(type)) {
                case 0:
                    Voicechat.SERVER_CONFIG.allowRecording.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set allow_recording to " + value);
                    break;
                case 1:
                    Voicechat.SERVER_CONFIG.spectatorInteraction.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set spectator_interaction to " + value);
                    break;
                case 2:
                    Voicechat.SERVER_CONFIG.spectatorPlayerPossession.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set spectator_player_possession to " + value);
                    break;
                case 3:
                    Voicechat.SERVER_CONFIG.forceVoiceChat.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set force_voice_chat to " + value);
                    break;
                case 4:
                    Voicechat.SERVER_CONFIG.loginTimeout.set(Integer.parseInt(value));
                    sendMSG(context, "Set login_timeout to " + value);
                    break;
                case 5:
                    Voicechat.SERVER_CONFIG.broadcastRange.set(Double.parseDouble(value));
                    sendMSG(context, "Set broadcast_range to " + value);
                    break;
                case 6:
                    Voicechat.SERVER_CONFIG.allowPings.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set allow_pings to " + value);
                    break;
                case 7:
                    Voicechat.SERVER_CONFIG.voiceChatDistance.set(Double.parseDouble(value));
                    sendMSG(context, "Set max_voice_distance to " + value);
                    break;
                case 8:
                    Voicechat.SERVER_CONFIG.crouchDistanceMultiplier.set(Double.parseDouble(value));
                    sendMSG(context, "Set crouch_distance_multiplier to " + value);
                    break;
                case 9:
                    Voicechat.SERVER_CONFIG.whisperDistanceMultiplier.set(Double.parseDouble(value));
                    sendMSG(context, "Set whisper_distance_multiplier to " + value);
                    break;
                case 10:
                    Voicechat.SERVER_CONFIG.groupsEnabled.set(Boolean.parseBoolean(value));
                    sendMSG(context, "Set enable_groups to " + value);
                    break;
            }
        } catch (NumberFormatException e) {
            sendMSG(context, "Invalid value");
        }
        return 1;
    }

    public static void sendMSG(CommandContext<CommandSourceStack> source, String message) {
        source.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    private static Server joinGroup(CommandSourceStack source) throws CommandSyntaxException {
        if (!Voicechat.SERVER_CONFIG.groupsEnabled.get()) {
            source.sendFailure(Component.translatable("message.voicechat.groups_disabled"));
            return null;
        }

        Server server = Voicechat.SERVER.getServer();
        if (server == null) {
            source.sendSuccess(() -> Component.translatable("message.voicechat.voice_chat_unavailable"), false);
            return null;
        }
        ServerPlayer player = source.getPlayerOrException();

        if (!PermissionManager.INSTANCE.GROUPS_PERMISSION.hasPermission(player)) {
            source.sendSuccess(() -> Component.translatable("message.voicechat.no_group_permission"), false);
            return null;
        }

        return server;
    }

    private static int joinGroupByName(CommandSourceStack source, String groupName, @Nullable String password) throws CommandSyntaxException {
        Server server = joinGroup(source);
        if (server == null) {
            return 1;
        }

        List<Group> groups = server.getGroupManager().getGroups().values().stream().filter(group -> group.getName().equals(groupName)).collect(Collectors.toList());

        if (groups.isEmpty()) {
            source.sendFailure(Component.translatable("message.voicechat.group_does_not_exist"));
            return 1;
        }

        if (groups.size() > 1) {
            source.sendFailure(Component.translatable("message.voicechat.group_name_not_unique"));
            return 1;
        }

        return joinGroup(source, server, groups.get(0).getId(), password);
    }

    private static int joinGroupById(CommandSourceStack source, UUID groupID, @Nullable String password) throws CommandSyntaxException {
        Server server = joinGroup(source);
        if (server == null) {
            return 1;
        }
        return joinGroup(source, server, groupID, password);
    }

    private static int joinGroup(CommandSourceStack source, Server server, UUID groupID, @Nullable String password) throws CommandSyntaxException {
        Group group = server.getGroupManager().getGroup(groupID);

        if (group == null) {
            source.sendFailure(Component.translatable("message.voicechat.group_does_not_exist"));
            return 1;
        }

        server.getGroupManager().joinGroup(group, source.getPlayerOrException(), password);
        source.sendSuccess(() -> Component.translatable("message.voicechat.join_successful", Component.literal(group.getName()).withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int help(CommandDispatcher<CommandSourceStack> dispatcher, CommandContext<CommandSourceStack> commandSource) {
        if (checkNoVoicechat(commandSource)) {
            return 0;
        }
        CommandNode<CommandSourceStack> voicechatCommand = dispatcher.getRoot().getChild(VOICECHAT_COMMAND);
        Map<CommandNode<CommandSourceStack>, String> map = dispatcher.getSmartUsage(voicechatCommand, commandSource.getSource());
        for (Map.Entry<CommandNode<CommandSourceStack>, String> entry : map.entrySet()) {
            commandSource.getSource().sendSuccess(() -> Component.literal("/%s %s".formatted(VOICECHAT_COMMAND, entry.getValue())), false);
        }
        return map.size();
    }

    private static boolean checkNoVoicechat(CommandContext<CommandSourceStack> commandSource) {
        try {
            ServerPlayer player = commandSource.getSource().getPlayerOrException();
            if (Voicechat.SERVER.isCompatible(player)) {
                return false;
            }
            commandSource.getSource().sendFailure(Component.literal(Voicechat.TRANSLATIONS.voicechatNeededForCommandMessage.get().formatted(CommonCompatibilityManager.INSTANCE.getModName())));
            return true;
        } catch (Exception e) {
            commandSource.getSource().sendFailure(Component.literal(Voicechat.TRANSLATIONS.playerCommandMessage.get()));
            return true;
        }
    }

    private static boolean checkPermission(CommandSourceStack stack, Permission permission) {
        try {
            return permission.hasPermission(stack.getPlayerOrException());
        } catch (CommandSyntaxException e) {
            return stack.hasPermission(stack.getServer().getOperatorUserPermissionLevel());
        }
    }

}

package de.maxhenkel.voicechat.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.FabricVoicechatMod;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.net.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class FabricVoiceCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("voicechatserver")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(
                                        context -> {
                                            double ms = System.currentTimeMillis();
                                            Voicechat.getInstance().initializeConfigs();
                                            ms = System.currentTimeMillis() - ms;
                                            double finalMs = ms;
                                            context.getSource().sendSuccess(() -> Component.literal("Reloaded configs in " + finalMs + "ms."), false);
                                            return 1;
                                        }
                                )
                        )
                        .then(Commands.literal("toggleGroups")
                                .executes(
                                        context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) {
                                                return 0;
                                            }
                                            var enabled = !Voicechat.SERVER_CONFIG.groupsEnabled.get();
                                            Voicechat.SERVER_CONFIG.groupsEnabled.set(enabled);
                                            JsonObject obj = new JsonObject();
                                            obj.addProperty("status", enabled);
                                            PacketHandler.sendVoicePacket(player, "groups", obj);

                                            player.getServer().getPlayerList().getPlayers().forEach(p -> {
                                                if (p == player) {
                                                    return;
                                                }
                                                PacketHandler.sendVoicePacket(p, "groups", obj);
                                            });
                                            context.getSource().sendSuccess(() -> Component.literal("Groups are now " + (enabled ? "enabled" : "disabled")), false);
                                            return 1;
                                        }
                                )
                        )
                        .then(Commands.literal("toggleRecording")
                                .executes(
                                        context -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player == null) {
                                                return 0;
                                            }
                                            var enabled = !Voicechat.SERVER_CONFIG.allowRecording.get();
                                            Voicechat.SERVER_CONFIG.allowRecording.set(enabled);
                                            JsonObject obj = new JsonObject();
                                            obj.addProperty("status", enabled);
                                            PacketHandler.sendVoicePacket(player, "recording", obj);

                                            player.getServer().getPlayerList().getPlayers().forEach(p -> {
                                                if (p == player) {
                                                    return;
                                                }
                                                PacketHandler.sendVoicePacket(p, "recording", obj);
                                            });
                                            context.getSource().sendSuccess(() -> Component.literal("Recording are now " + (enabled ? "enabled" : "disabled")), false);
                                            return 1;
                                        }
                                )
                        )
        );
    }
}

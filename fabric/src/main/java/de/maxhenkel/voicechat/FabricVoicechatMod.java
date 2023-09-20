package de.maxhenkel.voicechat;

import de.maxhenkel.voicechat.commands.FabricVoiceCommand;
import de.maxhenkel.voicechat.integration.ViaVersionCompatibility;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class FabricVoicechatMod extends Voicechat implements ModInitializer, DedicatedServerModInitializer {
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        initialize();
        ViaVersionCompatibility.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, asd) -> {
            new FabricVoiceCommand().register(dispatcher);
        });
    }

    public static MinecraftServer getServer() {
        return server;
    }

    @Override
    public void onInitializeServer() {
        ServerTickEvents.START_SERVER_TICK.register(server1 -> {
            if (server == null) {
                server = server1;
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, asd) -> {
            new FabricVoiceCommand().register(dispatcher);
        });
    }
}

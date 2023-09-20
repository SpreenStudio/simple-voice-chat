package de.maxhenkel.voicechat;

import de.maxhenkel.voicechat.commands.FabricVoiceCommand;
import de.maxhenkel.voicechat.net.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

@Environment(EnvType.CLIENT)
public class FabricVoicechatClientMod extends VoicechatClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        initializeClient();
        PacketHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, asd) -> {
            new FabricVoiceCommand().register(dispatcher);
        });
    }

}

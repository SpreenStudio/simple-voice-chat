package de.maxhenkel.voicechat.net.messages;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

public class UpdateStatusReceiver {
    public static void receive(Minecraft minecraft, ClientPacketListener clientPacketListener, FriendlyByteBuf friendlyByteBuf, PacketSender packetSender) {
        String jsonString = friendlyByteBuf.readUtf();
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        ClientVoicechat manager = ClientManager.getClient();
        switch(json.get("action").getAsString()){
            case "groups" -> {
                boolean groups = json.get("status").getAsBoolean();
                manager.getConnection().getData().setGroupsEnabled(groups);
            }
            case "recording" -> {
                boolean muted = json.get("status").getAsBoolean();
                manager.getConnection().getData().setAllowRecording(muted);
            }
        }
    }
}

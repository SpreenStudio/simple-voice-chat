package de.maxhenkel.voicechat.net;

import com.google.gson.JsonObject;
import de.maxhenkel.voicechat.FabricVoicechatMod;
import de.maxhenkel.voicechat.net.messages.UpdateStatusReceiver;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;

public class PacketHandler {
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("voice_settings"), UpdateStatusReceiver::receive);
    }


    public static void sendPacket(ServerPlayer entity, String id, Object... args){
        FriendlyByteBuf buf = PacketByteBufs.create();
        for (Object arg : args) {
            if (arg instanceof String) {
                buf.writeUtf((String) arg);
            } else if (arg instanceof Integer) {
                buf.writeInt((Integer) arg);
            } else if (arg instanceof Double) {
                buf.writeDouble((Double) arg);
            } else if (arg instanceof Float) {
                buf.writeFloat((Float) arg);
            } else if (arg instanceof Boolean) {
                buf.writeBoolean((Boolean) arg);
            }
        }
        ServerPlayNetworking.send(entity, new ResourceLocation(id), buf);
    }

    public static void sendVoicePacket(ServerPlayer target, String action, JsonObject json){
        json.addProperty("action", action);
        sendPacket(target, "voice_settings", json.toString());
    }
}

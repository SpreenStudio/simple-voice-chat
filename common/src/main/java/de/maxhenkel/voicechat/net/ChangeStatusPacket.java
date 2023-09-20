package de.maxhenkel.voicechat.net;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.plugins.impl.VolumeCategoryImpl;
import de.maxhenkel.voicechat.voice.server.SettingStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ChangeStatusPacket implements Packet<ChangeStatusPacket> {

    public static final ResourceLocation ADD_CATEGORY = new ResourceLocation(Voicechat.MODID, "add_category");

    private SettingStatus settingStatus;

    public ChangeStatusPacket() {

    }

    public ChangeStatusPacket(SettingStatus settingStatus) {
        this.settingStatus = settingStatus;
    }

    public SettingStatus getSettingStatus() {
        return settingStatus;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return ADD_CATEGORY;
    }

    @Override
    public ChangeStatusPacket fromBytes(FriendlyByteBuf buf) {
        return new ChangeStatusPacket(SettingStatus.fromBytes(buf));
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        settingStatus.toBytes(buf);
    }

}

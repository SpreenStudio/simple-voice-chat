package de.maxhenkel.voicechat.voice.server;

import de.maxhenkel.voicechat.Voicechat;
import net.minecraft.network.FriendlyByteBuf;

public class SettingStatus {
    private int groupsEnabled, allowRecording;

    public SettingStatus(){
        groupsEnabled = 0;
        allowRecording = 0;
    }

    public SettingStatus(int groupsEnabled, int allowRecording){
        this.groupsEnabled = groupsEnabled;
        this.allowRecording = allowRecording;
    }

    public boolean isGroupsEnabled(){
        if (groupsEnabled != 0){
            return groupsEnabled == 1;
        }
        return Voicechat.SERVER_CONFIG.groupsEnabled.get();
    }

    public boolean isRecordingEnables(){
        if (allowRecording != 0){
            return allowRecording == 1;
        }
        return Voicechat.SERVER_CONFIG.allowRecording.get();
    }

    public static SettingStatus fromBytes(FriendlyByteBuf buf) {
        SettingStatus settingStatus = new SettingStatus();
        settingStatus.groupsEnabled = buf.readInt();
        settingStatus.allowRecording = buf.readInt();
        return settingStatus;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(groupsEnabled);
        buf.writeInt(allowRecording);
    }
}

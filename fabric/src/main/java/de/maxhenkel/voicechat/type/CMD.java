package de.maxhenkel.voicechat.type;

import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.Voicechat;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public abstract class CMD {
    private final Voicechat mod;

    public CMD(Voicechat mod) {
        this.mod = mod;
    }
    public abstract void register(CommandDispatcher<CommandSourceStack> dispatcher);

    public Voicechat getMod() {
        return mod;
    }
}

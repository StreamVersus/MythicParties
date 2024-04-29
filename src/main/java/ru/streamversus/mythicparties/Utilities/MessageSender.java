package ru.streamversus.mythicparties.Utilities;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
@SuppressWarnings("UnstableApiUsage")
public class MessageSender implements PluginMessageListener {
    private final String channel;
    private byte[] buffer;

    public MessageSender(Plugin plugin, String channel, String subchannel, Player p, Object... args){
        this.channel = channel;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        for (Object arg : args) {
            out.writeUTF(arg.toString());
        }
        p.sendPluginMessage(plugin, channel, out.toByteArray());
    }
    public MessageSender(Plugin plugin, String channel, String subchannel, Server p, Object... args){
        this.channel = channel;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        for (Object arg : args) {
            out.writeUTF(arg.toString());
        }
        p.sendPluginMessage(plugin, channel, out.toByteArray());
    }
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] bytes) {
        if(channel.equals(this.channel)) buffer = bytes;
        notify();
    }
    public ByteArrayDataInput getResult(){
        if(buffer == null){
            try {
                wait(1500);
            } catch (Exception ignored) {}
        }
        if(buffer == null) return null;
        ByteArrayDataInput in = ByteStreams.newDataInput(buffer);
        in.readUTF();
        return in;
    }
}

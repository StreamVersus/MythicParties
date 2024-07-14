package ru.streamversus.mythicparties.Utilities;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.MythicParties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//WARNING: ASYNC
//BUKKIT COULD CRASH IF USED INCORRECTLY
public class MessageSender implements PluginMessageListener {
    public final static List<Player> exec = new ArrayList<>();
    @Getter
    private final CompletableFuture<ByteArrayDataInput> future;
    private ByteArrayDataInput buffer;
    private final Player code;
    private final String subchannel;
    private final Object sync;

    @SneakyThrows
    public static CompletableFuture<ByteArrayDataInput> send(Plugin plugin, byte[] arr, String subchannel, Object sync) {
        return CompletableFuture.supplyAsync(() -> new MessageSender(plugin, arr, subchannel, sync).getFuture().join());
    }

    @SneakyThrows
    public MessageSender(Plugin plugin, byte[] arr, String subchannel, Object sync) {

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(MythicParties.getPlugin(), "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(MythicParties.getPlugin(), "BungeeCord", this);
        this.sync = sync;
        this.subchannel = subchannel;
        code = getPlayer().join();
        code.sendPluginMessage(plugin, "BungeeCord", arr);

        future = CompletableFuture.supplyAsync(this::asyncReturn);
    }
    @SneakyThrows
    private CompletableFuture<Player> getPlayer(){
        return CompletableFuture.supplyAsync(() -> {
            if(exec.isEmpty()){
                synchronized (sync) {
                    try {
                        wait(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if(exec.isEmpty()){
                throw new RuntimeException("No players in Exec!");
            }
            return exec.remove(0);
        });
    }

    @SneakyThrows
    private ByteArrayDataInput asyncReturn(){
        if(buffer == null){
            wait(1000);
        }
        if(buffer == null){
            throw new IllegalStateException("Timeout on Bungee Messaging");
        }

        exec.add(code);
        return buffer;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if(!channel.equals("BungeeCord")) return;
        if(player != code) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        System.out.println(subchannel);
        if(!subchannel.equals(this.subchannel)) return;

        buffer = in;
    }
}

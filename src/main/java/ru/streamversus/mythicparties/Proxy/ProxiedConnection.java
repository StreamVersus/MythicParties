package ru.streamversus.mythicparties.Proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.leonardosnt.bungeechannelapi.BungeeChannelApi;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Database.*;

import java.sql.*;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ProxiedConnection implements ProxyHandler,Listener, PluginMessageListener {
    @Getter
    private final BungeeChannelApi api;
    private final dbMap<UUID, Location> tpmap;
    private final ConfigParser config;
    @Getter
    private Connection connect;
    @Getter
    private String serverName;
    private final onlineServers serverList;
    private final connectedPlayers players;

    @SneakyThrows
    public ProxiedConnection(Plugin plugin, ConfigParser config) {
        this.config = config;
        this.api = BungeeChannelApi.of(plugin);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(MythicParties.getPlugin(), "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(MythicParties.getPlugin(), "BungeeCord", this);

        Class.forName("com.mysql.cj.jdbc.Driver");
        connect = DriverManager.getConnection(
                "jdbc:mysql://" + config.getUrl() + "/" + config.getName() + "?characterEncoding=utf8&autoReconnect=true",
                config.getUsername(), config.getPassword());

        this.tpmap = new tpMap(this);
        this.serverList = new onlineServers(this);
        this.players = new connectedPlayers(this);

        api.registerForwardListener("MythicParties", (channelName, player, data) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(data);
            String subchannel = in.readUTF();
            Player p = Bukkit.getPlayer(UUID.fromString(in.readUTF()));
            if(p == null) return;
            switch (subchannel){
                case "executeAs" -> p.performCommand(in.readUTF());
                case "playSound" -> config.playSound(in.readUTF(), p);
                case "sendMessage" -> config.sendMessage(p, in.readUTF());
                case "sendWithReplacers" -> config.sendWithReplacer(p, in.readUTF(), in.readUTF());
                default -> plugin.getLogger().severe("Unknown message!!");
            }
        });
    }
    public void disable(){
        api.unregister();
        serverList.remove(serverName);
        if(!serverList.isOnline()){
            invitedMap.drop();
            leaderMap.drop();
            onlineServers.drop();
            partyMap.drop();
            tpMap.drop();
            connectedPlayers.drop();
        }
    }

    public void setServerName(String name){
        serverName = name;
        serverList.add(name);
    }
    @SneakyThrows
    public List<String> getPlayerList() {
        return players.getNames();
    }
    @SneakyThrows
    @Override
    public List<String> getServerList() {
        return api.getServers().get();
    }

    public boolean isOnThisServer(UUID player) {
        return Bukkit.getPlayer(player) != null;
    }

    @Override
    public void teleportTo(OfflinePlayer p, String server, Location location) {
        if(Objects.equals(server, serverName)){
            Player player = p.getPlayer();
            assert player != null;
            player.teleport(location);
            return;
        }
        tpmap.add(p.getUniqueId(), location);
        api.connectOther(Objects.requireNonNull(p.getName()), server);
    }

    @Override
    public boolean executeAs(UUID player, String command) {
        if (isOnThisServer(player)) return Objects.requireNonNull(Bukkit.getPlayer(player)).performCommand(command);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("executeAs");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(command);
        forward(out);
        return true;
    }

    @Override
    public void playSound(UUID player, String sound) {
        if (isOnThisServer(player)) config.playSound(sound, Bukkit.getPlayer(player));
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("playSound");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(sound);
        forward(out);
    }

    @Override
    public boolean sendMessage(UUID player, String msgname) {
        if (isOnThisServer(player)) return config.sendMessage(Bukkit.getPlayer(player), msgname);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("sendMessage");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(msgname);
        forward(out);
        return true;
    }

    @Override
    public void sendWithReplacer(UUID player, String msgname, String replacer) {
        if (isOnThisServer(player)) {
            config.sendWithReplacer(Bukkit.getPlayer(player), replacer, msgname);
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("sendWithReplacers");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(replacer);
        out.writeUTF(msgname);
        forward(out);
    }

    private void forward(ByteArrayDataOutput message) {
        api.forward("ALL", "MythicParties", message.toByteArray());
    }
    @SneakyThrows
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (serverName == null) {
            MythicParties.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(MythicParties.getPlugin(), () -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("GetServer");
                event.getPlayer().sendPluginMessage(MythicParties.getPlugin(), "BungeeCord", out.toByteArray());
            }, 8L);
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte @NotNull [] bytes) {
        if(!s.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subchannel = in.readUTF();
        if(subchannel.equals("GetServer")){
            setServerName(in.readUTF());
        }
    }
}

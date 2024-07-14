package ru.streamversus.mythicparties.Proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Database.*;
import ru.streamversus.mythicparties.Utilities.MessageSender;

import java.sql.*;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ProxiedConnection implements ProxyHandler,Listener, PluginMessageListener {
    private final dbMap<UUID, Location> tpmap;
    private final Object synchronizer = new Object();
    private final ConfigParser config;
    @Getter
    private Connection connect;
    private final Plugin plugin;
    @Getter
    private String serverName;
    private final onlineServers serverList;
    private final connectedPlayers players;

    @SneakyThrows
    public ProxiedConnection(Plugin plugin, ConfigParser config) {
        this.plugin = plugin;
        this.config = config;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Class.forName("com.mysql.cj.jdbc.Driver");
        connect = DriverManager.getConnection(
                "jdbc:mysql://" + config.getUrl() + "/" + config.getName() + "?characterEncoding=utf8&autoReconnect=true",
                config.getUsername(), config.getPassword());

        this.tpmap = new tpMap(this);
        this.serverList = new onlineServers(this);
        this.players = new connectedPlayers(this);

    }
    public void disable(){
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
        return null;
    }

    public boolean isOnThisServer(UUID player) {
        return Bukkit.getPlayer(player) != null;
    }

    @Override
    public void teleportTo(OfflinePlayer p, String server, Location location) {

    }

    @Override
    public boolean executeAs(UUID player, String command) {
        if (isOnThisServer(player)) return Objects.requireNonNull(Bukkit.getPlayer(player)).performCommand(command);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("executeAs");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(command);
        return true;
    }

    @Override
    public void playSound(UUID player, String sound) {
        if (isOnThisServer(player)) config.playSound(sound, Bukkit.getPlayer(player));
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("playSound");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(sound);
    }

    @Override
    public boolean sendMessage(UUID player, String msgname) {
        if (isOnThisServer(player)) return config.sendMessage(Bukkit.getPlayer(player), msgname);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("sendMessage");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(msgname);
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
    }
    @SneakyThrows
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> MessageSender.exec.add(event.getPlayer()), 100);
        getServerList();
    }
    @SneakyThrows
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        MessageSender.exec.remove(event.getPlayer());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte @NotNull [] bytes) {

    }
}

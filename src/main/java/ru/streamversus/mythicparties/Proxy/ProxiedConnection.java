package ru.streamversus.mythicparties.Proxy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.leonardosnt.bungeechannelapi.BungeeChannelApi;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Database.*;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
public class ProxiedConnection implements ProxyHandler, Listener {
    private final Cache<UUID, Location> tpmap;
    private final ConfigParser config;
    @Getter
    private final Connection connect;
    private final Plugin plugin;
    private final onlineServers serverList;
    private final BungeeChannelApi api;

    @SneakyThrows
    public ProxiedConnection(Plugin plugin, ConfigParser config) {
        this.plugin = plugin;
        this.config = config;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Class.forName("com.mysql.cj.jdbc.Driver");
        connect = DriverManager.getConnection(
                "jdbc:mysql://" + config.getUrl() + "/" + config.getName() + "?characterEncoding=utf8&autoReconnect=true",
                config.getUsername(), config.getPassword());


        this.api = BungeeChannelApi.of(plugin);
        this.tpmap = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
        this.serverList = new onlineServers(this);

        api.registerForwardListener("mythicparties", (s, player, bytes) -> {
            ByteArrayDataInput io = ByteStreams.newDataInput(bytes);
            var channel = io.readUTF();
            switch(channel){
                case "teleportTo" -> {
                    UUID id = UUID.fromString(io.readUTF());
                    Location loc = util.deserializeLoc(io.readUTF());
                    tpmap.put(id, loc);
                }
                case "playSound" -> {
                    UUID id = UUID.fromString(io.readUTF());
                    if(isOnThisServer(id)){
                        MythicParties.getHandler().playSound(id, io.readUTF());
                    }
                }
                case "sendMessage" -> {
                    UUID id = UUID.fromString(io.readUTF());
                    if(isOnThisServer(id)){
                        MythicParties.getHandler().sendMessage(id, io.readUTF());
                    }
                }
                case "sendWithReplacers" -> {
                    UUID id = UUID.fromString(io.readUTF());
                    String replace = io.readUTF();

                    if(isOnThisServer(id)){
                        MythicParties.getConfigParser().sendWithReplacer(Bukkit.getPlayer(id), replace, io.readUTF());
                    }
                }
            }
        });
    }
    public void disable(){
        serverList.remove();
        if(!serverList.isOnline()){
            invitedMap.drop();
            leaderMap.drop();
            onlineServers.drop();
            partyMap.drop();
        }
    }
    @SneakyThrows
    public String getServerName(){
        if(!serverList.isSetup()){
            serverList.add(api.getServer().get());
        }
        return serverList.getName();
    }

    @SneakyThrows
    public AtomicReference<List<String>> getPlayerList() {
        return new AtomicReference<>(api.getPlayerList("ALL").join());
    }
    @SneakyThrows
    @Override
    public AtomicReference<List<String>> getServerList() {
        return new AtomicReference<>(api.getServers().join());
    }

    public boolean isOnThisServer(UUID player) {
        return Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).anyMatch(id -> id.equals(player));
    }

    public void connectHere(OfflinePlayer p) {
        api.connectOther(p.getName(), getServerName());
    }

    @Override
    public void teleportTo(UUID player, String server, Location location) {
        if (isOnThisServer(player)) {
            Bukkit.getPlayer(player).teleport(location);
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("teleportTo");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(util.serializeLoc(location));

        api.forward(server, "mythicparties", out.toByteArray());

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () ->
                api.connectOther(Bukkit.getOfflinePlayer(player).getName(), server), 1L);
    }

    @Override
    public boolean executeAs(UUID player, String command) {
        if (isOnThisServer(player)) return Objects.requireNonNull(Bukkit.getPlayer(player)).performCommand(command);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("executeAs");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(command);
        api.forwardToPlayer(Bukkit.getOfflinePlayer(player).getName(), "mythicparties", out.toByteArray());

        return true;
    }

    @Override
    public void playSound(UUID player, String sound) {
        if (isOnThisServer(player)) {
            config.playSound(sound, Bukkit.getPlayer(player));
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("playSound");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(sound);

        api.forwardToPlayer(Bukkit.getOfflinePlayer(player).getName(), "mythicparties", out.toByteArray());
    }

    @Override
    public boolean sendMessage(UUID player, String msgname) {
        if (isOnThisServer(player)) return config.sendMessage(Bukkit.getPlayer(player), msgname);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("sendMessage");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(msgname);

        api.forwardToPlayer(Bukkit.getOfflinePlayer(player).getName(), "mythicparties", out.toByteArray());
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

        api.forwardToPlayer(Bukkit.getOfflinePlayer(player).getName(), "mythicparties", out.toByteArray());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if(tpmap.asMap().containsKey(e.getPlayer().getUniqueId())){
            e.getPlayer().teleportAsync(tpmap.asMap().get(e.getPlayer().getUniqueId()));
        }
    }
}

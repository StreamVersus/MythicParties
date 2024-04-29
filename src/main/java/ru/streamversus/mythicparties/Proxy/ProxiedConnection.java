package ru.streamversus.mythicparties.Proxy;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Utilities.MessageSender;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.*;
import java.util.*;
@SuppressWarnings("UnstableApiUsage")
public class ProxiedConnection implements ProxyHandler,PluginMessageListener,Listener {
    private final Plugin plugin;
    private final Messenger messenger;
    private PartyService partyService;
    private final Map<UUID, Location> tpmap = new HashMap<>();
    private final ConfigParser config;
    @Getter
    private Connection connect;

    @SneakyThrows
    public ProxiedConnection(Plugin plugin, ConfigParser config){
        this.plugin = plugin;
        this.messenger = plugin.getServer().getMessenger();
        this.config = config;

        messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
        messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
        messenger.registerOutgoingPluginChannel(plugin, "mythicparties:channel");
        messenger.registerIncomingPluginChannel(plugin, "mythicparties:channel", this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        Class.forName("com.mysql.cj.jdbc.Driver");
        connect = DriverManager.getConnection(
                "jdbc:mysql://" + config.getUrl() + "/" + config.getName() + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true",
                config.getUsername(), config.getPassword());

    }
    public void disable(){
        messenger.unregisterOutgoingPluginChannel(plugin);
        messenger.unregisterIncomingPluginChannel(plugin);
    }
    private Player getPlayer(){
        Collection<? extends Player> p = Bukkit.getOnlinePlayers();
        if(p.isEmpty()){
            try {
                wait(10);
            }catch (Exception ignored) {}
        }
        return p.toArray(new Player[0])[0];
    }
    public String getServerName(){
        ByteArrayDataInput array = new MessageSender(plugin, "BungeeCord", "GetServer", plugin.getServer()).getResult();
        if(array == null){
            return "local";
        }else{
            return array.readUTF();
        }
    }
    @Override
    public List<String> getPlayerList(){
        ByteArrayDataInput input = new MessageSender(plugin, "BungeeCord", "PlayerCount", getPlayer(),"ALL").getResult();
        List<String> proxylist;
        if(input != null) {
            input.readUTF();
            proxylist = new ArrayList<>(List.of(input.readUTF().split(", ")));
        } else {
            proxylist = new ArrayList<>();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if(!proxylist.contains(player.getName())) proxylist.add(player.getName());
        }
        return proxylist;
    }

    @Override
    public List<String> getServerList() {
        try {
            return List.of(new MessageSender(plugin, "BungeeCord", "GetServers", getPlayer()).getResult().readUTF().split(", "));
        } catch(Exception e){
            return new ArrayList<>();
        }
    }
    public boolean isOnAnotherServer(UUID player){
        return Bukkit.getPlayer(player) == null;
    }
    @Override
    public void teleportTo(UUID name, String server, Location location) {
        if(Objects.equals(server, getServerName())) Bukkit.getPlayer(name).teleport(location);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("teleport");
        out.writeUTF(String.valueOf(name));
        out.writeUTF(util.serializeLoc(location));
        forward(out);
        new MessageSender(plugin, "BungeeCord", "ConnectOther", getPlayer(), Bukkit.getOfflinePlayer(name).getName(), server);
    }

    @Override
    public boolean executeAs(UUID player, String command) {
        if(!isOnAnotherServer(player)) return Objects.requireNonNull(Bukkit.getPlayer(player)).performCommand(command);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("executeAs");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(command);
        forward(out);
        return true;
    }

    @Override
    public void playSound(UUID player, String sound) {
        if(!isOnAnotherServer(player)) config.playSound(sound, Bukkit.getPlayer(player));
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("playSound");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(sound);
        forward(out);
    }

    @Override
    public boolean sendMessage(UUID player, String msgname) {
        if(!isOnAnotherServer(player)) return config.sendMessage(Bukkit.getPlayer(player), msgname);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("sendMessage");
        out.writeUTF(String.valueOf(player));
        out.writeUTF(msgname);
        forward(out);
        return true;
    }

    @Override
    public void sendWithReplacer(UUID player, String msgname, String replacer) {
        if(!isOnAnotherServer(player)) {
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

    @Override
    public void connectService(PartyService service) {
        partyService = service;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] bytes) {
        if(channel.equals("BungeeCord")){
            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);
            ByteArrayDataInput msg = ByteStreams.newDataInput(msgbytes);
            String forwardchannel = msg.readUTF();

            switch (forwardchannel) {
                case "teleport" -> {
                    if (msg.readUTF().equals(getServerName())){
                        UUID id = UUID.fromString(msg.readUTF());
                        Player p = Bukkit.getOfflinePlayer(id).getPlayer();
                        Location loc = util.unserializeLoc(msg.readUTF());
                        if (p != null) {
                            p.teleport(loc);
                        } else {
                            tpmap.put(id, loc);
                        }
                    }
                }
                case "executeAs" -> {
                    UUID id = UUID.fromString(msg.readUTF());
                    String command = msg.readUTF();
                    if (isOnAnotherServer(id)) return;
                    Objects.requireNonNull(Bukkit.getPlayer(id)).performCommand(command);
                }
                case "playSound" -> {
                    UUID id = UUID.fromString(msg.readUTF());
                    String sound = msg.readUTF();
                    if (isOnAnotherServer(id)) return;
                    Player p = Bukkit.getPlayer(id);
                    config.playSound(sound, p);
                }
                case "sendMessage" -> {
                    UUID id = UUID.fromString(msg.readUTF());
                    String msgname = msg.readUTF();
                    if (isOnAnotherServer(id)) return;
                    Player p = Bukkit.getPlayer(id);
                    config.sendMessage(p, msgname);
                }
                case "sendWithReplacers" -> {
                    UUID id = UUID.fromString(msg.readUTF());
                    UUID replacer = UUID.fromString(msg.readUTF());
                    String msgname = msg.readUTF();
                    if (isOnAnotherServer(id)) return;
                    Player p = Bukkit.getPlayer(id);
                    String raw = Bukkit.getOfflinePlayer(replacer).getName();
                    config.sendWithReplacer(p, msgname, raw);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if(tpmap.containsKey(e.getPlayer().getUniqueId())){
            e.getPlayer().teleport(tpmap.get(e.getPlayer().getUniqueId()));
        }
    }
    private void forward(ByteArrayDataOutput message){
        byte[] raw = message.toByteArray();
        new MessageSender(plugin,"BungeeCord", "Forward", getPlayer(), "ALL", "mythicparties:channel", raw.length, raw).getResult();
    }
}

package ru.streamversus.mythicparties.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Parsers.ConfigParser;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Local implements ProxyHandler{
    private final ConfigParser config;

    public Local(ConfigParser config){
        this.config = config;
    }
    @Override
    public AtomicReference<List<String>> getPlayerList() {
        List<String> retval = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            retval.add(onlinePlayer.getName());
        }
        return new AtomicReference<>(retval);
    }

    @Override
    public AtomicReference<List<String>> getServerList() {
        return new AtomicReference<>(new ArrayList<>(){{
            add("local");
        }});
    }

    @Override
    public void teleportTo(UUID player, String server, Location location) {
        Player p = Bukkit.getPlayer(player);
        if(p == null) return;
        p.teleport(location);
    }

    @Override
    public boolean executeAs(UUID player, String command) {
        Player p = Bukkit.getPlayer(player);
        if(p == null) return false;
        return p.performCommand(command);
    }

    @Override
    public void playSound(UUID player, String sound) {
        Player p = Bukkit.getPlayer(player);
        if(p == null) return;
        config.playSound(sound, p);
    }

    @Override
    public boolean sendMessage(UUID player, String msgname) {
        Player p = Bukkit.getPlayer(player);
        if(p == null) return false;
        return config.sendMessage(p, msgname);
    }

    @Override
    public void sendWithReplacer(UUID player, String msgname, String replacer) {
        Player p = Bukkit.getPlayer(player);
        if(p == null) return;
        config.sendWithReplacer(p, replacer, msgname);
    }

    @Override
    public Connection getConnect() {
        return null;
    }

    @Override
    public void disable() {}
}

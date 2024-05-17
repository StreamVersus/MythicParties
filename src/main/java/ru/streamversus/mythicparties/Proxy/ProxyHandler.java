package ru.streamversus.mythicparties.Proxy;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public interface ProxyHandler {
    List<String> getPlayerList();
    List<String> getServerList();
    void teleportTo(OfflinePlayer name, String server, Location location);
    boolean executeAs(UUID player, String command);
    void playSound(UUID player, String sound);
    boolean sendMessage(UUID player, String msgname);
    void sendWithReplacer(UUID player, String msgname, String replacer);
    Connection getConnect();
    void disable();
}

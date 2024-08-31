package ru.streamversus.mythicparties.Proxy;

import org.bukkit.Location;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public interface ProxyHandler {
    AtomicReference<List<String>> getPlayerList();
    AtomicReference<List<String>> getServerList();
    void teleportTo(UUID player, String server, Location location);
    boolean executeAs(UUID player, String command);
    void playSound(UUID player, String sound);
    boolean sendMessage(UUID player, String msgname);
    void sendWithReplacer(UUID player, String msgname, String replacer);
    Connection getConnect();
    void disable();
}

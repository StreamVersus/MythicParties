package ru.streamversus.mythicparties.Database;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class tpMap implements dbMap<UUID, Location>, Listener {
    private static Connection connect = null;
    private static boolean local = false;
    @SneakyThrows
    public tpMap(ProxyHandler proxy){
        connect = proxy.getConnect();
        if(connect == null){
            local = true;
        }
        else {
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS tp(" +
                        "player varchar(8)," +
                        "loc TEXT NOT NULL" +
                        ")");
            }
            MythicParties.getPlugin().getServer().getPluginManager().registerEvents(this, MythicParties.getPlugin());
        }
    }
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS tp";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    @Override
    public void add(UUID uuid, Location location) {
        if(local){
            Objects.requireNonNull(Bukkit.getPlayer(uuid)).teleport(location);
            return;
        }
        String add = "INSERT INTO tp(player, loc) VALUES (?, ?)";
        try(PreparedStatement prep = connect.prepareStatement(add)){
            prep.setString(1, util.getCrc32Hash(uuid));
            prep.setString(2, util.serializeLoc(location));
            prep.executeUpdate();
        }
    }

    @Override
    public void update(UUID uuid, Location location) {
        throw new IllegalStateException("unsupported");
    }
    @SneakyThrows
    @Override
    public Location remove(UUID uuid) {
        if(local){
            return null;
        }
        Location loc = get(uuid);
        String remove = "DELETE FROM tp WHERE player = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)) {
            prep.setString(1, util.getCrc32Hash(uuid));
            prep.executeUpdate();
        }
        return loc;
    }
    @SneakyThrows
    @Override
    public Location get(UUID uuid) {
        if(local){
            return null;
        }
        String get = "SELECT * FROM tp WHERE player = ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, util.getCrc32Hash(uuid));
            try (ResultSet result = prep.executeQuery()) {
                if (result.next()){
                    return util.deserializeLoc(result.getString("loc"));
                }
                return null;
            }
        }
    }

    @Override
    public Set<UUID> idSet() {
        throw new IllegalStateException("unsupported");
    }

    @Override
    public boolean contains(UUID uuid) {
        throw new IllegalStateException("unsupported");
    }

    @Override
    public void replace(UUID uuid, Location location) {
        throw new IllegalStateException("unsupported");
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e){
        Location tp = remove(e.getPlayer().getUniqueId());
        if(tp == null) return;
        e.getPlayer().teleport(tp);
    }
}

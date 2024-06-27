package ru.streamversus.mythicparties.Database;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class connectedPlayers implements Listener {
    private static Connection connect = null;
    private static Boolean local = false;
    private final List<UUID> playerList = new ArrayList<>();

    @SneakyThrows
    public connectedPlayers(ProxyHandler proxy){
        connect = proxy.getConnect();
        if(connect == null){
            local = true;
        }
        else{
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS players(" +
                        "id TEXT NOT NULL" +
                        ")");
            }
        }
        MythicParties.getPlugin().getServer().getPluginManager().registerEvents(this, MythicParties.getPlugin());
    }
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS players";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public void add(UUID a){
        if(local) {
            playerList.add(a);
            return;
        }
        String add = "INSERT INTO players(id) VALUES (?)";

        try(PreparedStatement prep = connect.prepareStatement(add)){
            prep.setString(1, String.valueOf(a));
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public void remove(UUID a){
        if(local) {
            playerList.remove(a);
            return;
        }
        String remove = "DELETE FROM players WHERE id = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)){
            prep.setString(1, String.valueOf(a));
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public List<UUID> get(){
        if(local) {
            return playerList;
        }
        String get = "SELECT * FROM players";

        try(PreparedStatement prep = connect.prepareStatement(get)){
            try(ResultSet result = prep.executeQuery()){
                if(result.next()) {
                    List<UUID> retval = new ArrayList<>();
                    do {
                        retval.add(UUID.fromString(result.getString("id")));
                    } while (result.next());
                    return retval;
                }
                else return new ArrayList<>();
            }
        }
    }
    public List<String> getNames(){
        List<UUID> uuids = get();
        List<String> retval = new ArrayList<>();
        for (UUID uuid : uuids) {
            retval.add(Bukkit.getOfflinePlayer(uuid).getName());
        }
        return retval;
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent e){
        MythicParties.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(MythicParties.getPlugin(), () -> add(e.getPlayer().getUniqueId()), 1L);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLeave(PlayerQuitEvent e){
        remove(e.getPlayer().getUniqueId());
    }
}

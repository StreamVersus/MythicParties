package ru.streamversus.mythicparties.database;

import lombok.SneakyThrows;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static ru.streamversus.mythicparties.Utilities.util.*;

public class leaderMap implements dbMap<UUID, Party> {
    private static Connection connect;
    private static boolean local = false;
    private final Map<UUID, Party> localMap = new HashMap<>();

    @SneakyThrows
    public leaderMap(){
        connect = MythicParties.getHandler().getConnect();
        if(connect == null){
            local = true;
        }
        else {
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS leaders(" +
                        "uuid varchar(8) NOT NULL," +
                        "party TEXT NOT NULL" +
                        ")");
            }
        }
    }
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS leaders";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public void add(UUID uuid, Party party) {
        if(local){
            localMap.put(uuid, party);
            return;
        }
        String add = "INSERT INTO leaders(uuid, party) VALUES (?, ?)";

        try(PreparedStatement prep = connect.prepareStatement(add)) {
            prep.setString(1, getCrc32Hash(uuid));
            prep.setString(2, util.serializeParty(party));
            prep.executeUpdate();
        }
    }

    @SneakyThrows
    public void update(UUID uuid, Party party) {
        if(local) return;
        String update = "UPDATE leaders SET party = ? WHERE uuid = ?";
        if(party.getLeader().getUniqueId() == uuid) {
            try (PreparedStatement prep = connect.prepareStatement(update)) {
                prep.setString(2, getCrc32Hash(uuid));
                prep.setString(1,  util.serializeParty(party));
                prep.executeUpdate();
            }
        }
    }

    @SneakyThrows
    public Party remove(UUID uuid) {
        if(local){
            return localMap.remove(uuid);
        }
        String remove = "DELETE FROM leaders WHERE uuid = ?";
        Party party = get(uuid);
        try(PreparedStatement prep = connect.prepareStatement(remove)) {
            prep.setString(1, getCrc32Hash(uuid));
            prep.executeUpdate();
        }
        return party;
    }

    @SneakyThrows
    public Party get(UUID uuid) {
        if(local){
            return localMap.get(uuid);
        }

        String get = "SELECT * FROM leaders WHERE uuid = ?";
        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, getCrc32Hash(uuid));
            try (ResultSet result = prep.executeQuery()) {
                if (result.next()){
                    return util.deserializeParty(result.getString("party"));
                }
                return null;
            }
        }
    }

    public Set<UUID> idSet() {
        throw new IllegalStateException("idSet shouldn't be called on this map type");
    }

    @SneakyThrows
    public boolean contains(UUID uuid) {
        if(local) {
            return localMap.containsKey(uuid);
        }
        String get = "SELECT uuid FROM leaders WHERE uuid = ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, getCrc32Hash(uuid));
            try (ResultSet result = prep.executeQuery()){
                return result.next();
            }
        }
    }

    @Override
    public void replace(UUID uuid, Party party) {
        if(local){
            localMap.replace(uuid, party);
        }
    }
}

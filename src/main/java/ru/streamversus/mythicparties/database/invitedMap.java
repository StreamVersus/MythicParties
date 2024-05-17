package ru.streamversus.mythicparties.database;

import lombok.SneakyThrows;
import ru.streamversus.mythicparties.entrypoints.MythicPartiesBukkit;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ru.streamversus.mythicparties.Utilities.util.getCrc32Hash;

public class invitedMap implements dbMap<UUID, Party>{
    private static Connection connect = null;
    private static boolean local = false;
    private final Map<UUID, Party> localMap = new HashMap<>();

    @SneakyThrows
    public invitedMap(){
        connect = MythicPartiesBukkit.getHandler().getConnect();
        if(connect == null){
            local = true;
        }
        else {
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS invited(" +
                        "uuid varchar(8)," +
                        "party TEXT NOT NULL" +
                        ")");
            }
        }
    }
    @SneakyThrows
    @Override
    public void add(UUID uuid, Party party) {
        if(local){
            localMap.put(uuid, party);
            return;
        }

        String add = "INSERT INTO invited(uuid, party) VALUES (?, ?)";

        try(PreparedStatement prep = connect.prepareStatement(add)) {
            prep.setString(1, util.getCrc32Hash(uuid));
            prep.setString(2, util.serializeParty(party));
            prep.executeUpdate();
        }
    }

    @Override
    public void update(UUID uuid, Party party) {
        throw new IllegalStateException("unsupported");
    }
    @SneakyThrows
    @Override
    public Party remove(UUID uuid) {
        if(local){
            return localMap.remove(uuid);
        }

        String remove = "DELETE FROM invited WHERE uuid = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)) {
            prep.setString(1, util.getCrc32Hash(uuid));
            prep.executeUpdate();
        }
        return null;
    }
    @SneakyThrows
    @Override
    public Party get(UUID uuid) {
        if(local){
            return localMap.get(uuid);
        }
        String get = "SELECT * FROM invited WHERE uuid = ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, util.getCrc32Hash(uuid));
            try (ResultSet result = prep.executeQuery()) {
                if (result.next()){
                    return util.deserializeParty(result.getString("party"));
                }
                return null;
            }
        }
    }

    @Override
    public Set<UUID> idSet() {
        throw new IllegalStateException("unsupported");
    }
    @SneakyThrows
    @Override
    public boolean contains(UUID uuid) {
        if(local) {
            return localMap.containsKey(uuid);
        }
        String get = "SELECT uuid FROM invited WHERE uuid = ?";

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
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS invited";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }
}

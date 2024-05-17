package ru.streamversus.mythicparties.database;

import lombok.SneakyThrows;
import ru.streamversus.mythicparties.entrypoints.MythicPartiesBukkit;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.*;
import java.util.*;

public class partyMap implements dbMap<Integer, Party>{
    private static Connection connect = null;
    private static boolean local = false;
    private final Map<Integer, Party> localMap = new HashMap<>();

    @SneakyThrows
    public partyMap(){
        connect = MythicPartiesBukkit.getHandler().getConnect();
        if(connect == null){
            local = true;
        }
        else {
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS parties(" +
                        "id integer," +
                        "party TEXT NOT NULL" +
                        ")");
            }
        }
    }
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS parties";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    @Override
    public void add(Integer id, Party party){
        if(local){
            localMap.put(id, party);
            return;
        }
        String add = "INSERT INTO parties(id, party) VALUES (?, ?)";

        try(PreparedStatement prep = connect.prepareStatement(add)) {
            prep.setInt(1, id);
            prep.setString(2, util.serializeParty(party));
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public void update(Integer id, Party party){
        if(local) return;
        String update = "UPDATE parties SET party = ? WHERE id = ?";

        try(PreparedStatement prep = connect.prepareStatement(update)) {
            prep.setInt(2, id);
            prep.setString(1, util.serializeParty(party));
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public Party remove(Integer id){
        if(local){
            localMap.remove(id);
            return null;
        }
        String remove = "DELETE FROM parties WHERE id = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)) {
            prep.setInt(1, id);
            prep.executeUpdate();
        }
        return null;
    }
    @SneakyThrows
    public Party get(Integer id){
        if(local){
            return localMap.get(id);
        }
        String get = "SELECT * FROM parties WHERE id = ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setInt(1, id);
            try (ResultSet result = prep.executeQuery()) {
                if (result.next()){
                    return util.deserializeParty(result.getString("party"));
                }
                return null;
            }
        }
    }
    @SneakyThrows
    public Set<Integer> idSet(){
        if(local) {
            return localMap.keySet();
        }
        String get = "SELECT id FROM parties";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            try (ResultSet result = prep.executeQuery()){
                if(result.next()) {
                    Set<Integer> set = new HashSet<>();
                    do {
                        set.add(result.getInt("id"));
                    } while (result.next());
                    return set;
                }
                else return new HashSet<>();
            }
        }
    }
    public boolean contains(Integer id){
        if(local) {
            return localMap.containsKey(id);
        }
        return idSet().contains(id);
    }

    @Override
    public void replace(Integer integer, Party party) {
        if(local){
            localMap.replace(integer, party);
        }
    }

}

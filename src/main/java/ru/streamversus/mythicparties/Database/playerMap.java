package ru.streamversus.mythicparties.Database;

import lombok.SneakyThrows;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.Utilities.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class playerMap implements dbMap<UUID, Party> {
    private static Connection connect = null;
    private static boolean local = false;
    private final Map<UUID, Party> localMap = new HashMap<>();

    @SneakyThrows
    public playerMap(){
        connect = MythicParties.getHandler().getConnect();
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
    @Override
    @SneakyThrows
    public void add(UUID uuid, Party party) {
        if(local){
            localMap.put(uuid, party);
        }
        String add = "INSERT INTO parties(id, party) VALUES (?, ?)";

        try(PreparedStatement prep = connect.prepareStatement(add)) {
            prep.setInt(1, party.getId());
            prep.setString(2, util.serializeParty(party));
            prep.executeUpdate();
        }
    }

    @Override
    public void update(UUID uuid, Party party) {
        throw new IllegalStateException("unsupported");
    }

    @Override
    @SneakyThrows
    public Party remove(UUID uuid) {
        if(local){
            return localMap.remove(uuid);
        }
        Party retval = get(uuid);

        String remove = "DELETE FROM parties WHERE id = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)) {
            prep.setInt(1, retval.getId());
            prep.executeUpdate();
        }

        return retval;
    }
    @SneakyThrows
    @Override
    public Party get(UUID uuid) {
        if(local){
            return localMap.get(uuid);
        }
        String get = "SELECT * FROM parties WHERE party like ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, "%"+uuid+"%");
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
        if(local){
            return localMap.containsKey(uuid);
        }
        String get = "SELECT * FROM parties WHERE party like ?";

        try(PreparedStatement prep = connect.prepareStatement(get)) {
            prep.setString(1, "%"+uuid+"%");
            try (ResultSet result = prep.executeQuery()) {
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

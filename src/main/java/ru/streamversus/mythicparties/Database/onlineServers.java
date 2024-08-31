package ru.streamversus.mythicparties.Database;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.streamversus.mythicparties.Proxy.ProxiedConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class onlineServers {
    private static Connection connect = null;
    private static boolean local = false;
    private boolean set = false;
    @Getter
    private String name;

    @SneakyThrows
    public onlineServers(ProxiedConnection con){
        connect = con.getConnect();
        if(connect == null){
            local = true;
        }
        else {
            try (Statement state = connect.createStatement()) {
                state.execute("CREATE TABLE IF NOT EXISTS servers(" +
                        "server TEXT NOT NULL" +
                        ")");
            }
        }
    }
    @SneakyThrows
    public void add(String server){
        if(local) return;
        String add = "INSERT INTO servers(server) VALUES (?)";

        try(PreparedStatement prep = connect.prepareStatement(add)){
            prep.setString(1, server);
            prep.executeUpdate();
        }
        set = true;
        name = server;
    }
    @SneakyThrows
    public void remove(){
        if(local) return;
        String remove = "DELETE FROM servers WHERE server = ?";

        try(PreparedStatement prep = connect.prepareStatement(remove)){
            prep.setString(1, name);
            prep.executeUpdate();
        }
    }
    @SneakyThrows
    public boolean isOnline(){
        if(local) return false;
        String get = "SELECT * FROM servers";

        try(PreparedStatement prep = connect.prepareStatement(get)){
            try(ResultSet result = prep.executeQuery()){
                return result.next();
            }
        }
    }
    @SneakyThrows
    public static void drop(){
        if(local) return;
        String drop = "DROP TABLE IF EXISTS servers";

        try(PreparedStatement prep = connect.prepareStatement(drop)){
            prep.executeUpdate();
        }
    }

    public boolean isSetup(){
        return set;
    }
}

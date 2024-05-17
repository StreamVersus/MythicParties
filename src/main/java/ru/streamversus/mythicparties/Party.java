package ru.streamversus.mythicparties;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;
import ru.streamversus.mythicparties.database.dbMap;
import ru.streamversus.mythicparties.database.partyMap;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;

public class Party {
    private final ConfigParser config;
    private final ProxyHandler proxyhandle;
    private int maxPlayer;
    private static Class<?> clazz;
    private static Boolean compatStatus;
    private UUID leaderUUID;
    private final List<UUID> playerUUIDs = new ArrayList<>();
    @Getter
    private int id;
    public static final dbMap<Integer, Party> idMap = new partyMap();
    public Party(UUID leader, Plugin plugin, ConfigParser config, ProxyHandler proxyhandle){
        this(leader, plugin, config, proxyhandle, true);
    }

    public Party(UUID leader, Plugin plugin, ConfigParser config, ProxyHandler proxyhandle, boolean map) {
        this.config = config;
        this.proxyhandle = proxyhandle;
        updateLimit();
        for (Integer i : idMap.idSet()) {
            if(!idMap.contains(i+1)) {
                id = i+1;
                break;
            }
            else {
                id = idMap.idSet().size();
            }
        }
        this.leaderUUID = leader;
        this.playerUUIDs.add(leader);
        if(compatStatus == null) {
            if (Bukkit.getPluginManager().isPluginEnabled("MythicDungeons")) {
                compatStatus = false;
            }else{
                try {
                    Class<?> mythic = Class.forName("net.playavalon.mythicdungeons.MythicDungeons");
                    Field f = mythic.getDeclaredField("plugin");
                    f.setAccessible(true);
                    compatStatus = ((String) mythic.getMethod("getPartyPluginName").invoke(f.get(null))).equalsIgnoreCase(plugin.getName());
                    //CommandAPIVelocity.unregister("p", true, true);
                } catch(Exception ignored){}
            }
            if(compatStatus == null) compatStatus = false;
        }
        if(compatStatus) {
            try {
                if (!Party.clazz.getName().equals("IDungeonParty"))
                    Party.clazz = Class.forName("net.playavalon.mythicdungeons.api.party.IDungeonParty");
            } catch(Exception ignored){}
            Object party = Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[] {Party.clazz},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "addPlayer" -> addPlayer((OfflinePlayer) args[0]);
                            case "removePlayer" -> removePlayer((OfflinePlayer) args[0]);
                            case "getPlayers" -> getPlayers();
                            case "getLeader" -> getLeader();
                            default -> throw new IllegalArgumentException("Proxy error!!!");
                        }
                        return null;
                    });
            try {
                clazz.getMethod("initDungeonParty", Plugin.class).invoke(party, plugin);
            }catch(Exception ignored){}
        }
        if(map) idMap.add(id, this);
    }
    private void updateLimit(){
        maxPlayer = FlagHandler.getLimitMap().get(leaderUUID) == null ? config.getLimit() : FlagHandler.getLimitMap().get(leaderUUID);
    }
    public static Party getPartyByID(Integer id){return idMap.get(id);}
    public static Integer getPartyCount(){return idMap.idSet().size();}
    public void addPlayer(OfflinePlayer player) {
        playerUUIDs.add(player.getUniqueId());
        idMap.update(id, this);
    }
    //Can cause problems, if unchecked
    public void addPlayer(UUID player) {
        playerUUIDs.add(Bukkit.getOfflinePlayer(player).getUniqueId());
        idMap.update(id, this);
    }
    public void forEach(Consumer<OfflinePlayer> cons) {
        for (UUID p : playerUUIDs) {
            cons.accept(Bukkit.getOfflinePlayer(p));
        }
    }
    public int getPlayerID(UUID p){
        int i;
        try{
            i = playerUUIDs.indexOf(p)+1;
        } catch (Exception e){
            return -1;
        }
        return i;
    }
    public int getLimit(){
        updateLimit();
        return maxPlayer;
    }
    public void removePlayer(OfflinePlayer player) {
        playerUUIDs.remove(player.getUniqueId());
        idMap.update(id, this);
    }
    public void removePlayer(UUID player) {
        playerUUIDs.remove(player);
        idMap.update(id, this);
    }
    public List<OfflinePlayer> getPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        playerUUIDs.forEach(uuid -> players.add(Bukkit.getOfflinePlayer(uuid)));
        return players;
    }
    public OfflinePlayer getPlayer(int id){
        UUID uuid;
        try {
         uuid = playerUUIDs.get(id - 1);
        } catch(IndexOutOfBoundsException e){
            uuid = null;
        }
        if(uuid == null) return null;
        return Bukkit.getOfflinePlayer(uuid);
    }
    public void swapPlayers(OfflinePlayer p, OfflinePlayer m){
        int firstindex = playerUUIDs.indexOf(p.getUniqueId());
        int secondindex = playerUUIDs.indexOf(m.getUniqueId());
        proxyhandle.sendMessage(p.getUniqueId(), "swap_wrong_args");
        if(firstindex == -1 || secondindex == -1) return;
        playerUUIDs.remove(firstindex);
        playerUUIDs.remove(secondindex);
        playerUUIDs.add(secondindex, p.getUniqueId());
        playerUUIDs.add(firstindex, m.getUniqueId());
        idMap.update(id, this);
    }
    public int getPlayerCount(){
        return playerUUIDs.toArray().length;
    }
    public void changeLeaderUUID(UUID p) {
        leaderUUID = p;
        idMap.update(id, this);
    }
    public @NotNull OfflinePlayer getLeader() {
        return Bukkit.getOfflinePlayer(leaderUUID);
    }
    public void destroy(){
        idMap.remove(id);
    }
}

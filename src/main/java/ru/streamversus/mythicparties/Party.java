package ru.streamversus.mythicparties;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.Database.partyMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Party {
    private final ConfigParser config;
    private final ProxyHandler proxyhandle;
    private int maxPlayer;
    private static Boolean compatStatus;
    private UUID leaderUUID;
    private final List<UUID> playerUUIDs = new ArrayList<>();
    @Getter
    private int id;
    public static final dbMap<Integer, Party> idMap = new partyMap();
    public Party(UUID leader, ConfigParser config, ProxyHandler proxyhandle){
        this(leader, config, proxyhandle, null);
    }

    public Party(UUID leader, ConfigParser config, ProxyHandler proxyhandle, Integer id) {
        this.config = config;
        this.proxyhandle = proxyhandle;
        updateLimit();

        //Это менять страшно, и благо не надо :D
        if(id == null) {
            for (Integer i : idMap.idSet()) {
                if (!idMap.contains(i + 1)) {
                    this.id = i + 1;
                    break;
                } else {
                    this.id = idMap.idSet().size();
                }
            }
        }
        else{
            this.id = id;
        }

        this.leaderUUID = leader;
        this.playerUUIDs.add(leader);

        if(compatStatus == null) {
            compatStatus = config.isMDSupport();
            if (!Bukkit.getPluginManager().isPluginEnabled("MythicDungeons")) compatStatus = false;
        }

        if(compatStatus) {
            new ru.streamversus.mythicparties.Utilities.PartyMDWrapper(this).initDungeonParty();
        }

        if(id == null) idMap.add(this.id, this);
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
    public void addPlayer(UUID player) {
        playerUUIDs.add(player);
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
    public AtomicReference<List<OfflinePlayer>> getPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        playerUUIDs.forEach(uuid -> players.add(Bukkit.getOfflinePlayer(uuid)));
        return new AtomicReference<>(players);
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

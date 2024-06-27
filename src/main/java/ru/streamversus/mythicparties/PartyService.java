package ru.streamversus.mythicparties;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.streamversus.mythicparties.Database.leaderMap;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.Database.invitedMap;
import ru.streamversus.mythicparties.Database.playerMap;

import java.util.*;

public class PartyService {
    private final Plugin plugin;
    @Getter
    private final Map<UUID, BukkitTask> disbandTask = new HashMap<>(), kickTask = new HashMap<>(), inviteTask = new HashMap<>();
    @Getter
    private final ConfigParser config;
    @Getter
    private final dbMap<UUID, Party> partyMap, invitedMap, leaderMap;
    private final ProxyHandler proxy;

    PartyService(Plugin plugin, ConfigParser config, ProxyHandler proxy) {
        this.config = config;
        this.plugin = plugin;
        this.proxy = proxy;

        this.partyMap = new playerMap();
        this.invitedMap = new invitedMap();
        this.leaderMap = new leaderMap();
    }
    public void scheduler(OfflinePlayer p){
        if(leaderMap.contains(p.getUniqueId())){
            scheduleDisband(p, false);
        } else {
            scheduleKick(p, false);
        }
    }
    public void scheduleDisband(OfflinePlayer p, boolean disable){
        if(!config.isDisband()) return;
        Party party = leaderMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {
            BukkitTask t = disbandTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else {
            disbandTask.put(p.getUniqueId(), new BukkitRunnable() {
                @Override
                public void run() {
                    leaveDisband(p);
                }
            }.runTaskLater(this.plugin, config.getLeaderDisband()));
            party.forEach(player -> proxy.sendMessage(player.getUniqueId(), "leader_offline"));
        }
    }
    private void leaveDisband(OfflinePlayer p){
        Party party = leaderMap.remove(p.getUniqueId());
        disband(p);
        party.destroy();
        scheduleDisband(p, true);
    }
    public void scheduleKick(OfflinePlayer p, boolean disable){
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {BukkitTask t = kickTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else {
            kickTask.put(p.getUniqueId(), new BukkitRunnable() {
                @Override
                public void run() {
                    scheduledkick(p);
                }
            }.runTaskLater(this.plugin, config.getLeaderDisband()));
            party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(),"player_offline", p.getName()));
        }
    }
    public void createParty(Player p) {
        createParty(p.getUniqueId());
    }
    public void createParty(UUID id) {
        if(leaderMap.get(id) != null || partyMap.get(id) != null) return;
        if(leaderMap.get(id) != null) return;
        Party party = new Party(id, config, proxy);
        leaderMap.add(id, party);
        partyMap.add(id, party);
    }

    public boolean isntLeader(Player p) {
        return leaderMap.get(p.getUniqueId()) == null;
    }
    public boolean isntLeader(OfflinePlayer p) {
        return leaderMap.get(p.getUniqueId()) == null;
    }

    public void kick(OfflinePlayer p){
        Party party = partyMap.remove(p.getUniqueId());
        party.removePlayer(p.getUniqueId());
        createParty(p.getUniqueId());
        proxy.sendMessage(p.getUniqueId(), "kick_alert");
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), p.getName(), "kick_selfalert"));
    }
    public void scheduledkick(OfflinePlayer p){
        Party party = partyMap.get(p.getUniqueId());
        party.removePlayer(p.getUniqueId());
        createParty(p.getUniqueId());
        proxy.sendMessage(p.getUniqueId(), "kick_alert");
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), p.getName(), "kick_selfalert"));
        scheduleKick(p, true);
    }

    public boolean disband(OfflinePlayer p) {
        if (isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "disband_non_leader");
        Party party = leaderMap.get(p.getUniqueId());
        List<OfflinePlayer> kickList = party.getPlayers();
        kickList.forEach(p1 -> {
            if(p1 == p) return;
            party.removePlayer(p1.getUniqueId());
            createParty(p1.getUniqueId());
        });
        kickList.forEach((player) -> proxy.sendMessage(player.getUniqueId(), "disband_alert"));
        return true;
    }


    public Integer getPartyID(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getId();
    }
    public String getLeaderName(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getLeader().getName();
    }
    public String getPlayerName(OfflinePlayer p, Integer i){
        OfflinePlayer arg = partyMap.get(p.getUniqueId()).getPlayer(i);
        if(arg == null) return "";
        return arg.getName();
    }
    public Integer getPartySize(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getPlayerCount();
    }
    public Integer getFreeSlots(OfflinePlayer p){
        Party party = partyMap.get(p.getUniqueId());
        return party.getLimit() - party.getPlayers().toArray().length;
    }
    public Integer getPlayerID(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getPlayerID(p.getUniqueId());
    }
    public Integer getPartyLimit(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getLimit();
    }
    public Boolean isSlotBusy(OfflinePlayer p, Integer i){
        return partyMap.get(p.getUniqueId()).getPlayer(i) != null;
    }
    public Boolean isPlayerLeader(OfflinePlayer p){
        return leaderMap.get(p.getUniqueId()) != null;
    }
    public Boolean isPlayerParticipant(OfflinePlayer p){
        if(leaderMap.get(p.getUniqueId()) != null) return false;
        return partyMap.get(p.getUniqueId()) != null;
    }
    public Player getmember(OfflinePlayer p, Integer i){
        OfflinePlayer p1 = partyMap.get(p.getUniqueId()).getPlayer(i);
        return Bukkit.getPlayer(p1.getUniqueId());
    }
    public Party getParty(UUID id){
        return partyMap.get(id);
    }
    public Party getParty(Player p){
        return getParty(p.getUniqueId());
    }
}

package ru.streamversus.mythicparties.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;

import java.util.List;
import java.util.UUID;

public class util {
    public static String serializeLoc(Location loc){
        String retval = "";

        retval += loc.getX();
        retval += "/";
        retval += loc.getY();
        retval += "/";
        retval += loc.getZ();
        retval += "/";
        retval += loc.getPitch();
        retval += "/";
        retval += loc.getYaw();
        retval += "/";
        retval += loc.getWorld().getName();

        return retval;
    }
    public static Location unserializeLoc(String loc){
        String[] raw = loc.split("/");
        return new Location(Bukkit.getWorld(raw[5]), Double.parseDouble(raw[0]), Double.parseDouble(raw[1]), Double.parseDouble(raw[2]), Float.parseFloat(raw[3]), Float.parseFloat(raw[4]));
    }
    public static String serializeParty(Party party){
        StringBuilder retval = new StringBuilder();

        List<OfflinePlayer> players = party.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            OfflinePlayer p = players.get(i);
            String raw = String.valueOf(p.getUniqueId());

            if(i != players.size()-1) raw += ":";

            retval.append(raw);
        }

        return retval.toString();
    }
    public static Party deserializeParty(int id, String raw){
        if(Party.idMap.contains(id)) return Party.idMap.get(id);

        String[] splited = raw.split(":");
        UUID leader = UUID.fromString(splited[0]);
        Party party = new Party(leader, MythicParties.getPlugin(), MythicParties.getConfigParser(), MythicParties.getHandler());
        for(int i = 1; i < splited.length; i++){
            party.addPlayer(UUID.fromString(splited[1]));
        }
        return party;
    }
}

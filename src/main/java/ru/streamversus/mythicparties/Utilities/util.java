package ru.streamversus.mythicparties.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;

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
    public static Location deserializeLoc(String loc){
        String[] raw = loc.split("/");
        return new Location(Bukkit.getWorld(raw[5]), Double.parseDouble(raw[0]), Double.parseDouble(raw[1]), Double.parseDouble(raw[2]), Float.parseFloat(raw[3]), Float.parseFloat(raw[4]));
    }

    public static String serializeParty(Party party){
        StringBuilder retval = new StringBuilder();

        retval.append(party.getId()).append(";");

        List<OfflinePlayer> players = party.getPlayers().get();
        for (int i = 0; i < players.size(); i++) {
            OfflinePlayer p = players.get(i);
            String raw = String.valueOf(p.getUniqueId());

            if(i != players.size()-1) raw += ":";

            retval.append(raw);
        }

        return retval.toString();
    }
    public static Party deserializeParty(String raw){
        Integer id = Integer.valueOf(raw.split(";")[0]);
        raw = raw.split(";")[1];
        String[] splited = raw.split(":");
        UUID leader = UUID.fromString(splited[0]);
        Party party = new Party(leader, MythicParties.getConfigParser(), MythicParties.getHandler(), id);
        for(int i = 1; i < splited.length; i++){
            party.addPlayer(UUID.fromString(splited[1]));
        }
        return party;
    }

    public static String getCrc32Hash(UUID uuid){
        CRC32 crc32 = new CRC32(){{
            update(String.valueOf(uuid).getBytes(StandardCharsets.UTF_8));
        }};
        return String.format(Locale.US,"%08X", crc32.getValue());
    }
    public static String getCrc32Hash(String str){
        CRC32 crc32 = new CRC32(){{
            update(str.getBytes(StandardCharsets.UTF_8));
        }};
        return String.format(Locale.US,"%08X", crc32.getValue());
    }
}

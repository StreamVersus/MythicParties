package ru.streamversus.mythicparties;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceHolderExpansion extends PlaceholderExpansion {
    private final PartyService partyService;
    public PlaceHolderExpansion(PartyService partyService){
        this.partyService = partyService;
    }
    @Override
    public @NotNull String getIdentifier() {
        return "party";
    }
    @Override
    public @NotNull String getAuthor() {
        return "StreamVersus";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public boolean canRegister() {
        return true;
    }
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Player p = (Player) player;
        if(params.equalsIgnoreCase("id")){return partyService.getPartyID(p).toString();}
        else if(params.equalsIgnoreCase("leader_name")){return partyService.getLeaderName(p);}
        else if(params.equalsIgnoreCase("size")){return partyService.getPartySize(p).toString();}
        else if(params.equalsIgnoreCase("size_free")){return partyService.getFreeSlots(p).toString();}
        else if(params.equalsIgnoreCase("slot")){return partyService.getPlayerID(p).toString();}
        else if(params.equalsIgnoreCase("max_limit")){return partyService.getPartyLimit(p).toString();}
        else if(params.equalsIgnoreCase("leader_stats")){return partyService.isPlayerLeader(p) ? "Yes" : "No";}
        else if(params.equalsIgnoreCase("participant_stats")){return partyService.isPlayerParticipant(p) ? "Yes" : "No";}
        else{
            if(params.matches("member_name_\\[.*]")){
                String raw = partyService.getPlayerName(p, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
                return raw.isEmpty() ? "%party_" + params + "%" : raw;
            } else if (params.matches("slot_\\[.*]_busy")){
                return partyService.isSlotBusy(p, Integer.valueOf(params.split("\\[")[1].split("]")[0])) ? "Yes" : "No";
            } else if (params.matches("member_papi_\\[.*]_\\{.*}")){
                Player p1 = partyService.getmember(player, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
                if(p1 == null) return "%party_" + params + "%";
                String placeholder = params.split("\\{")[1].split("}")[0];
                return PlaceholderAPI.setPlaceholders(p1, "%"+placeholder+"%");
            } else return null;
        }
    }
}

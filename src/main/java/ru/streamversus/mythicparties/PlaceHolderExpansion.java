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
        if(params.equalsIgnoreCase("party_id")){return partyService.getPartyID(p).toString();}
        else if(params.equalsIgnoreCase("party_leader_name")){return partyService.getLeaderName(p);}
        else if(params.equalsIgnoreCase("party_size")){return partyService.getPartySize(p).toString();}
        else if(params.equalsIgnoreCase("party_size_free")){return partyService.getFreeSlots(p).toString();}
        else if(params.equalsIgnoreCase("party_slot")){return partyService.getPlayerID(p).toString();}
        else if(params.equalsIgnoreCase("party_max_limit")){return partyService.getPartyLimit(p).toString();}
        else if(params.equalsIgnoreCase("party_leader_stats")){return partyService.isPlayerLeader(p) ? "Yes" : "No";}
        else if(params.equalsIgnoreCase("party_participant_stats")){return partyService.isPlayerParticipant(p) ? "Yes" : "No";}
        else{
            if(params.matches("party_member_name_\\[.*]")){
                return partyService.getPlayerName(p, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
            } else if (params.matches("party_slot_\\[.*]_busy")){
                return partyService.isSlotBusy(p, Integer.valueOf(params.split("\\[")[1].split("]")[0])) ? "Yes" : "No";
            } else if (params.matches("party_member_papi_\\[.*]_\\{.*}")){
                Player p1 = partyService.getmember((Player) player, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
                String placeholder = params.split("\\{")[1].split("}")[0] + params.split("\\[")[1].split("]")[0];
                return PlaceholderAPI.setPlaceholders(p1, "%"+placeholder+"%");
            } else return null;
        }
    }
}

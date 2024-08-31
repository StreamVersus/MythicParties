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
        String retval;
        switch (params.toLowerCase()){
            case "id" -> retval = partyService.getPartyID(player).toString();
            case "leader_name" -> retval = partyService.getLeaderName(player);
            case "size" -> retval = partyService.getPartySize(player).toString();
            case "size_free" -> retval = partyService.getFreeSlots(player).toString();
            case "slot" -> retval = partyService.getPlayerID(player).toString();
            case "max_limit" -> retval = partyService.getPartyLimit(player).toString();
            case "leader_stats" -> retval = partyService.isPlayerLeader(player) ? "Yes" : "No";
            case "participant_stats" -> retval = partyService.isPlayerParticipant(player) ? "Yes" : "No";
            case "size_serverleader" -> retval = partyService.getPartySizeLeaderServer(player);
            case "listname" -> retval = partyService.playerList(player);
            case "listname_l" -> retval = partyService.playerListl(player);
            case "listname_sl" -> retval = partyService.playerListsl(player);

            default -> {
                //переписать парсинг, это пиздец, мне стыдно за сплиты
                if(params.matches("member_name_\\[.*]")){
                    String raw = partyService.getPlayerName(player, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
                    return raw.isEmpty() ? "%party_" + params + "%" : raw;
                } else if (params.matches("slot_\\[.*]_busy")){
                    return partyService.isSlotBusy(player, Integer.valueOf(params.split("\\[")[1].split("]")[0])) ? "Yes" : "No";
                } else if (params.matches("member_papi_\\[.*]_\\{.*}")){
                    Player p1 = partyService.getmember(player, Integer.valueOf(params.split("\\[")[1].split("]")[0]));
                    if(p1 == null) return "%party_" + params + "%";
                    String placeholder = params.split("\\{")[1].split("}")[0];
                    return PlaceholderAPI.setPlaceholders(p1, "%"+placeholder+"%");
                } else return null;
            }
        }

        return retval;
    }
}

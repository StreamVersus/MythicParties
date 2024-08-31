package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.arguments.GlobalTeamPlayerArgument;
import ru.streamversus.mythicparties.Commands.arguments.TeamSlotArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.UUID;

public class slotplayer extends SubCommandImpl {

    public slotplayer(CommandImpl main){
        super(main, "slotplayer");
        withArguments(GlobalTeamPlayerArgument.get("playerArg"));
        withArguments(TeamSlotArgument.get("slots"));
    }

    @Override
    public boolean exec(Player send, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        dbMap<UUID, Party> leaderMap = service.getLeaderMap();
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        if(service.isntLeader(send)) return proxy.sendMessage(send.getUniqueId(), "swap_non_leader");

        OfflinePlayer p = (OfflinePlayer) args.get("playerArg");
        Integer slot = (Integer) args.get("slots");
        Party party = leaderMap.get(send.getUniqueId());

        if(slot == null || p == null) return proxy.sendMessage(send.getUniqueId(), "swap_wrong_args");
        if(slot == 1) return proxy.sendMessage(send.getUniqueId(), "swap_leader");

        int i = party.getPlayerID(p.getUniqueId());

        if(i == -1) return proxy.sendMessage(send.getUniqueId(), "swap_empty_slot");
        if(i == slot) return proxy.sendMessage(send.getUniqueId(), "swap_already");

        OfflinePlayer m;
        try {
            m = party.getPlayer(slot);
        } catch(Exception e){
            return proxy.sendMessage(send.getUniqueId(), "swap_empty_slot");
        }

        party.swapPlayers(p, m);

        return true;
    }
}

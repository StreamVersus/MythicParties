package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.arguments.GlobalTeamPlayerArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.UUID;

public class givelead extends SubCommandImpl {

    public givelead(CommandImpl main){
        super(main, "givelead");
        withArguments(GlobalTeamPlayerArgument.get("playerArg"));
    }

    @Override
    public boolean exec(Player p, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        dbMap<UUID, Party> leaderMap = service.getLeaderMap();
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        if (service.isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "givelead_non_leader");
        OfflinePlayer leadingPlayer = (OfflinePlayer) args.get("playerArg");
        if (leadingPlayer == null) return proxy.sendMessage(p.getUniqueId(), "givelead_no_player");
        Party party = leaderMap.remove(p.getUniqueId());
        leaderMap.add(leadingPlayer.getUniqueId(), party);
        party.changeLeaderUUID(leadingPlayer.getUniqueId());
        proxy.sendMessage(leadingPlayer.getUniqueId(), "givelead_alert");
        proxy.sendWithReplacer(p.getUniqueId(),"givelead_selfalert", leadingPlayer.getName());
        return true;
    }
}

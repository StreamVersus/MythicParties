package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.UUID;

public class refuse extends SubCommandImpl {

    public refuse(CommandImpl main){
        super(main, "refuse");
    }

    @Override
    public boolean exec(Player sender, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        dbMap<UUID, Party> invitedMap = service.getInvitedMap();
        dbMap<UUID, Party> leaderMap = service.getLeaderMap();
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        if(!invitedMap.contains(sender.getUniqueId())) return proxy.sendMessage(sender.getUniqueId(), "accept_no_invites");

        OfflinePlayer invitesender = invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return proxy.sendMessage(sender.getUniqueId(), "accept_wrong_args");

        invitedMap.remove(sender.getUniqueId()).forEach(player -> proxy.sendWithReplacer(player.getUniqueId(),"invite_refused", sender.getName()));

        proxy.sendWithReplacer(sender.getUniqueId(),"invite_self_refused", invitesender.getName());

        return true;
    }
}

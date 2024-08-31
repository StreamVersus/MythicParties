package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.Map;
import java.util.UUID;

public class accept extends SubCommandImpl {

    public accept(CommandImpl main){
        super(main, "accept");
    }

    @Override
    public boolean exec(Player sender, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        dbMap<Integer, Party> partyMap = Party.idMap;
        dbMap<UUID, Party> invitedMap = service.getInvitedMap();
        dbMap<UUID, Party> leaderMap = service.getLeaderMap();
        ProxyHandler proxy = MythicParties.getHandler();
        Map<UUID, BukkitTask> inviteTask = service.getInviteTask();
        //end

        if(!invitedMap.contains(sender.getUniqueId())) return proxy.sendMessage(sender.getUniqueId(), "accept_no_invites");

        OfflinePlayer invitesender = invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return proxy.sendMessage(sender.getUniqueId(), "accept_wrong_args");

        Party party = leaderMap.get(invitesender.getUniqueId());
        party.addPlayer(sender);
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), "invite_accepted", sender.getName()));

        Party remove = leaderMap.remove(sender.getUniqueId());
        partyMap.remove(remove.getId()).destroy();

        partyMap.update(party.getId(), party);
        leaderMap.update(invitesender.getUniqueId(), party);
        inviteTask.remove(sender.getUniqueId()).cancel();

        return true;
    }
}

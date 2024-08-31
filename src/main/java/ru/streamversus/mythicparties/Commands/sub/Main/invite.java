package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.streamversus.mythicparties.Commands.arguments.GlobalPlayerArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.Map;
import java.util.UUID;

public class invite extends SubCommandImpl {

    public invite(CommandImpl main){
        super(main, "invite");
        withArguments(GlobalPlayerArgument.get("playerArg"));
    }

    @Override
    public boolean exec(Player p, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        dbMap<UUID, Party> invitedMap = service.getInvitedMap();
        dbMap<UUID, Party> leaderMap = service.getLeaderMap();
        ProxyHandler proxy = MythicParties.getHandler();
        Map<UUID, BukkitTask> inviteTask = service.getInviteTask();
        //end

        if (service.isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "invite_non_leader");

        OfflinePlayer invitedPlayer = (OfflinePlayer) args.get("playerArg");
        assert invitedPlayer != null;
        if(p == invitedPlayer) return proxy.sendMessage(p.getUniqueId(), "invite_self");

        Party party = leaderMap.get(p.getUniqueId());
        if(party.getLimit() <= party.getPlayerCount()) return proxy.sendMessage(p.getUniqueId(), "invite_party_full");

        if(invitedMap.get(invitedPlayer.getUniqueId()) != null) return proxy.sendMessage(p.getUniqueId(), "invite_already_invited");

        invitedMap.add(invitedPlayer.getUniqueId(), party);
        proxy.sendWithReplacer(invitedPlayer.getUniqueId(),"invite_alert", p.getName());
        party.forEach((player) -> proxy.sendWithReplacer(player.getUniqueId(), "invite_sended", invitedPlayer.getName()));

        inviteTask.put(invitedPlayer.getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                invitedMap.remove(invitedPlayer.getUniqueId());
                proxy.sendMessage(p.getUniqueId(), "invite_self_dismiss");
                proxy.sendWithReplacer(invitedPlayer.getUniqueId(), "invite_dismiss", p.getName());
            }
        }.runTaskLater(MythicParties.getPlugin(), MythicParties.getConfigParser().getInviteTimer()));

        return true;
    }
}

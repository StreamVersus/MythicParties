package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.Database.dbMap;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.UUID;

public class leave extends SubCommandImpl {

    public leave(CommandImpl main){
        super(main, "leave");
    }

    @Override
    public boolean exec(CommandSender sender, CommandArguments args) {
        //compatibility block
        Player p = (Player) sender;
        PartyService service = MythicParties.getPartyService();
        dbMap<UUID, Party> partyMap = service.getPartyMap();
        dbMap<UUID, Party> leaderMap = service.getPartyMap();
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        if(leaderMap.contains(p.getUniqueId())) return proxy.sendMessage(p.getUniqueId(), "leave_leader");
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return proxy.sendMessage(p.getUniqueId(), "leave_no_party");

        party.removePlayer(p.getUniqueId());
        service.createParty(p.getUniqueId());

        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), "leave_alert", p.getName()));
        proxy.sendMessage(p.getUniqueId(), "leave_selfalert");

        return true;
    }
}

package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.arguments.GlobalTeamPlayerArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

public class kick extends SubCommandImpl {

    public kick(CommandImpl main){
        super(main, "kick");
        withArguments(GlobalTeamPlayerArgument.get("playerArg"));
    }

    @Override
    public boolean exec(Player p, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        ProxyHandler proxy = MythicParties.getHandler();
        //end


        if (service.isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "kick_non_leader");
        OfflinePlayer kickedPlayer = (OfflinePlayer) args.get("playerArg");
        if (kickedPlayer == null) return proxy.sendMessage(p.getUniqueId(), "kick_no_player");

        service.kick(kickedPlayer);

        proxy.sendWithReplacer(p.getUniqueId(),"kick_selfalert", kickedPlayer.getName());

        return true;
    }
}

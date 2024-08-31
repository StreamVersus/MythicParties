package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SubTagArgument extends CustomArgument<Party, String> {
    private SubTagArgument(String nodeName) {
        super(new StringArgument(nodeName), new SubTagParser());
    }

    public static Argument<Party> get(String nodename) {
        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            String currentArg = info.currentArg();
            ProxyHandler proxy = MythicParties.getHandler();
            if(currentArg.startsWith("i")) {
                int id = Party.idMap.idSet().size();
                if (id > 9) id = 9;

                List<String> retval = new ArrayList<>();
                for (int i = 0; i < id; i++) {
                    retval.add("id_" + i);
                }

                if(retval.contains(currentArg)) return new String[]{currentArg};

                return retval.toArray(String[]::new);
            }

            else if(currentArg.startsWith("p")){
                List<String> retval = new ArrayList<>();

                var playerList = proxy.getPlayerList().get();
                int l = playerList.size();
                if(l > 9) l = 9;

                for (int i = 0; i < l; i++) {
                    retval.add("player_" + playerList.get(i));
                }

                return retval.toArray(String[]::new);
            }

            else if(currentArg.startsWith("t")){
                return new String[]{"trugger_party"};
            }

            String[] retval = new String[3];
            retval[0] = "trugger_party";
            retval[1] = "id_*";
            retval[2] = "player_*";
            return retval;
        }));

        return new SubTagArgument(nodename).includeSuggestions(defaultSuggestor);
    }
}
class SubTagParser implements CustomArgument.CustomArgumentInfoParser<Party, String>{

    @Override
    public Party apply(CustomArgument.CustomArgumentInfo<String> customArgumentInfo) {
        String subtag = customArgumentInfo.input();
        PartyService service = MythicParties.getPartyService();
        ProxyHandler proxy = MythicParties.getHandler();
        Player sender = (Player) customArgumentInfo.sender();

        if(subtag.equals("trugger_party")) return service.getParty(sender);

        else if(subtag.startsWith("id_")) {
            int partyId = Integer.parseInt(subtag.replace("id_", ""));
            if(partyId > Party.getPartyCount()-1) proxy.sendMessage(sender.getUniqueId(), "wrong_subtag");

            return Party.getPartyByID(partyId);

        } else if (subtag.startsWith("player_")) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(subtag.replace("player_", ""));

            return service.getParty(p.getUniqueId());
        } else throw new NullPointerException();
    }
}
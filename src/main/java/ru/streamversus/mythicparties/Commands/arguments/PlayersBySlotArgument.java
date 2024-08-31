package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Party;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayersBySlotArgument extends CustomArgument<List<OfflinePlayer>, String> {
    private PlayersBySlotArgument(String nodeName){
        super(new StringArgument(nodeName), new PlayersBySlotParser());
    }

    public static Argument<List<OfflinePlayer>> get(String nodename) {
        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            Party party = (Party) info.previousArgs().get("subtag");
            assert party != null;
            String raw = info.currentInput();

            if(raw.equals("all")) return new String[]{"all"};

            List<String> retval = new ArrayList<>();

            if(raw.isEmpty()) retval.add("all");

            StringBuilder ret = new StringBuilder();
            for (int i = 1; i < party.getPlayerCount()+1; i++) {
                if(raw.contains(String.valueOf(i))) continue;

                ret.append(i);
                if(!(i == party.getPlayerCount())) ret.append("/");
            }

            retval.add(ret.toString());

            return retval.toArray(String[]::new);
        }));

        return new PlayersBySlotArgument(nodename).includeSuggestions(defaultSuggestor);
    }
}
class PlayersBySlotParser implements CustomArgument.CustomArgumentInfoParser<List<OfflinePlayer>, String>{

    @Override
    public List<OfflinePlayer> apply(CustomArgument.CustomArgumentInfo<String> customArgumentInfo) {
        Party party = (Party) customArgumentInfo.previousArgs().get("subtag");
        assert party != null;
        ProxyHandler proxy = MythicParties.getHandler();
        Player p = (Player) customArgumentInfo.sender();
        List<OfflinePlayer> retval = new ArrayList<>();

        String raw = customArgumentInfo.input();

        if(raw.equals("all")) {
            retval = party.getPlayers().get();
        }else{
            String[] slots = (raw).split("/");
            for (String s : slots) {
                int id;

                try {
                    id = Integer.parseInt(s);
                } catch (Exception e){
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }

                OfflinePlayer p1 = party.getPlayer(id);
                if(p1 == null) {
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }
                retval.add(p1);
            }
        }
        return retval;
    }
}
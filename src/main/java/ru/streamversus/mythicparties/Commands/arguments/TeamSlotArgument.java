package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.PartyService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TeamSlotArgument extends IntegerArgument {
    public TeamSlotArgument(String nodeName) {
        super(nodeName);

        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            PartyService service = MythicParties.getPartyService();

            int i = service.getPartySize((Player) info.sender());
            if(i>10) i = 10;

            List<String> retval = new ArrayList<>();
            for(int f = 1; f<=i; f++){
                retval.add(String.valueOf(f));
            }

            return retval.toArray(String[]::new);
        }));
        includeSuggestions(defaultSuggestor);
    }
}

package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.MythicParties;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GlobalPlayerArgument extends CustomArgument<OfflinePlayer, Player> {
    public GlobalPlayerArgument(String nodeName) {
        super(new PlayerArgument(nodeName), new GlobalPlayerParser());

        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            List<String> playerList = MythicParties.getHandler().getPlayerList();
            //default return
            if (info.currentArg().isEmpty()) {
                return playerList.toArray(String[]::new);
            }

            //get possible Player
            return playerList.stream().filter(name -> name.startsWith(info.currentArg())).toArray(String[]::new);
        }));

        replaceSuggestions(defaultSuggestor);
    }
}
class GlobalPlayerParser implements CustomArgument.CustomArgumentInfoParser<OfflinePlayer, Player> {
    @Override
    public OfflinePlayer apply(CustomArgument.CustomArgumentInfo<Player> customArgumentInfo) {
        return Bukkit.getOfflinePlayer(customArgumentInfo.input());
    }
}

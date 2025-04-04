package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.MythicParties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GlobalTeamPlayerArgument extends CustomArgument<OfflinePlayer, OfflinePlayer> {
    private GlobalTeamPlayerArgument(String nodeName) {
        super(new OfflinePlayerArgument(nodeName), new GlobalTeamPlayerParser());
    }

    public static Argument<OfflinePlayer> get(String nodename) {
        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            Player p = (Player) info.sender();
            List<String> playerList = new ArrayList<>(MythicParties.getPartyService().getParty(p).getPlayers().get().stream().map(OfflinePlayer::getName).toList());
            playerList.remove(info.sender().getName());
            //default return
            if (info.currentArg().isEmpty()) {
                return playerList.toArray(String[]::new);
            }

            //get possible Player
            return playerList.stream().filter(name -> name.startsWith(info.currentArg())).toArray(String[]::new);
        }));

        return new GlobalTeamPlayerArgument(nodename).replaceSuggestions(defaultSuggestor);
    }
}
class GlobalTeamPlayerParser implements CustomArgument.CustomArgumentInfoParser<OfflinePlayer, OfflinePlayer> {
    @Override
    public OfflinePlayer apply(CustomArgument.CustomArgumentInfo<OfflinePlayer> customArgumentInfo) {
        return Bukkit.getOfflinePlayer(customArgumentInfo.input());
    }
}

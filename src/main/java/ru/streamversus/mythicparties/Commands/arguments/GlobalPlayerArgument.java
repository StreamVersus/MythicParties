package ru.streamversus.mythicparties.Commands.arguments;

import dev.jorel.commandapi.arguments.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import ru.streamversus.mythicparties.MythicParties;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GlobalPlayerArgument extends CustomArgument<OfflinePlayer, OfflinePlayer> {
    private GlobalPlayerArgument(String nodeName) {
        super(new OfflinePlayerArgument(nodeName), new GlobalPlayerParser());
    }

    public static Argument<OfflinePlayer> get(String nodename) {
        ArgumentSuggestions<CommandSender> defaultSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            var playerList = MythicParties.getHandler().getPlayerList().get();
            return playerList.stream().filter(name -> {
                if(Objects.equals(info.currentArg(), "")) {
                    return !Objects.equals(name, info.sender().getName());
                }
                else return !Objects.equals(name, info.sender().getName()) && !name.startsWith(info.currentArg());
            }).toArray(String[]::new);
        }));

        return new GlobalPlayerArgument(nodename).replaceSuggestions(defaultSuggestor);
    }
}
class GlobalPlayerParser implements CustomArgument.CustomArgumentInfoParser<OfflinePlayer, OfflinePlayer> {
    @Override
    public OfflinePlayer apply(CustomArgument.CustomArgumentInfo<OfflinePlayer> customArgumentInfo) {
        return Bukkit.getOfflinePlayer(customArgumentInfo.input());
    }
}

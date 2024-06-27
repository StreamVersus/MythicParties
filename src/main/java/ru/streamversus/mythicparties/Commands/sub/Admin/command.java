package ru.streamversus.mythicparties.Commands.sub.Admin;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.arguments.PlayersBySlotArgument;
import ru.streamversus.mythicparties.Commands.arguments.SubTagArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.List;
import java.util.UUID;

public class command extends SubCommandImpl {
    private final ProxyHandler proxy = MythicParties.getHandler();

    public command(CommandImpl main){
        super(main, "command");
        withArguments(new SubTagArgument("subtag").combineWith(new PlayersBySlotArgument("slots")));
        withArguments(new GreedyStringArgument("command"));
    }

    @Override
    public boolean exec(CommandSender sender, CommandArguments args) {
        UUID id = ((Player) sender).getUniqueId();
        List<OfflinePlayer> executeList = args.getUnchecked("slots");
        if(executeList == null){
            proxy.sendMessage(id, "wrong_subtag");
            return false;
        }

        String command = (String) args.get("command");
        if(command == null){
            proxy.sendMessage(id, "command_wrong_args");
            return false;
        }

        boolean buffer = true;

        for (OfflinePlayer player : executeList) {
            buffer = proxy.executeAs(player.getUniqueId(), command);
        }
        if(!buffer){
            proxy.sendMessage(id, "command_fail");
        }

        return true;
    }
}

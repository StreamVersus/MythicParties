package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;

public class disband extends SubCommandImpl {

    public disband(CommandImpl main){
        super(main, "disband");
    }

    @Override
    public boolean exec(CommandSender sender, CommandArguments args) {
        return MythicParties.getPartyService().disband((OfflinePlayer) sender);
    }
}

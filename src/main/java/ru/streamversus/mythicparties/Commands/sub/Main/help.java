package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

public class help extends SubCommandImpl {

    public help(CommandImpl main){
        super(main, "help");
    }

    @Override
    public boolean exec(CommandSender sender, CommandArguments args) {
        //compatibility block
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        return proxy.sendMessage(((Player) sender).getUniqueId(), "help");
    }
}

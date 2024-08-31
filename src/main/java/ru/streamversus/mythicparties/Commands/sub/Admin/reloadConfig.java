package ru.streamversus.mythicparties.Commands.sub.Admin;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.entity.Player;

import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;

public class reloadConfig extends SubCommandImpl {
    public reloadConfig(CommandImpl main){
        super(main, "reloadConfig");
    }
    @Override
    public boolean exec(Player sender, CommandArguments args) {
        MythicParties.getConfigParser().reloadConfig();
        return true;
    }
}

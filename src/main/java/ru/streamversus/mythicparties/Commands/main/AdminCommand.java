package ru.streamversus.mythicparties.Commands.main;

import lombok.Getter;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.sub.Admin.command;
import ru.streamversus.mythicparties.Commands.sub.Admin.reloadConfig;
import ru.streamversus.mythicparties.Commands.sub.Admin.teleport;

@Getter
public class AdminCommand extends CommandImpl {
    public AdminCommand(String name){
        super(name);
        withSubcommand(new command(this));
        withSubcommand(new teleport(this));
        withSubcommand(new reloadConfig(this));
    }
}

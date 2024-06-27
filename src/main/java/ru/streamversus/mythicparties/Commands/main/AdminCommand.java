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

        new command(this);
        new teleport(this);
        new reloadConfig(this);

        register();
    }
}

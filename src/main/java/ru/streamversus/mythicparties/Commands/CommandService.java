package ru.streamversus.mythicparties.Commands;

import ru.streamversus.mythicparties.Commands.main.AdminCommand;
import ru.streamversus.mythicparties.Commands.main.MainCommand;
import ru.streamversus.mythicparties.Parsers.ConfigParser;

public class CommandService {
    public CommandService(ConfigParser config){
        new MainCommand(config.getCommandNameList().get(0));
        new AdminCommand(config.getCommandNameList().get(1));
    }
}

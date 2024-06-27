package ru.streamversus.mythicparties.Commands.main;

import lombok.Getter;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.sub.Main.*;

@Getter
public class MainCommand extends CommandImpl {
    public MainCommand(String name){
        super(name);

        withSubcommand(new accept(this));
        withSubcommand(new disband(this));
        withSubcommand(new givelead(this));
        withSubcommand(new help(this));
        withSubcommand(new invite(this));
        withSubcommand(new kick(this));
        withSubcommand(new leave(this));
        withSubcommand(new refuse(this));
        withSubcommand(new slotplayer(this));

        register();
    }
}

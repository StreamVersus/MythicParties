package ru.streamversus.mythicparties.Commands.main;

import lombok.Getter;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.sub.Main.*;

@Getter
public class MainCommand extends CommandImpl {
    public MainCommand(String name){
        super(name);

        new accept(this);
        new disband(this);
        new givelead(this);
        new help(this);
        new invite(this);
        new kick(this);
        new leave(this);
        new refuse(this);
        new slotplayer(this);

        register();
    }
}

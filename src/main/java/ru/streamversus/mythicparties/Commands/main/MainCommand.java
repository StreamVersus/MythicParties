package ru.streamversus.mythicparties.Commands.main;

import lombok.Getter;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;

@Getter
public class MainCommand extends CommandImpl {
    public MainCommand(String name){
        super(name);
    }
}

package ru.streamversus.mythicparties.Commands.implementations;

import dev.jorel.commandapi.CommandAPICommand;

import java.util.ArrayList;
import java.util.List;

public class CommandImpl extends CommandAPICommand {
    public String name;
    private final List<SubCommandImpl> subcommands = new ArrayList<>();

    public CommandImpl(String name) {
        super(name);
        this.name = name;
    }

    public void withSubcommand(SubCommandImpl command){
        subcommands.add(command);
        super.withSubcommand(command);
    }

    @Override
    public void register(){
        for (SubCommandImpl subcommand : subcommands) {
            subcommand.regPerms();
        }
        super.register();
    }
}

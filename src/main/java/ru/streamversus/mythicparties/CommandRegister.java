package ru.streamversus.mythicparties;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import ru.streamversus.mythicparties.Parsers.ConfigParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;


public class CommandRegister {
    private final List<String> nameList = new ArrayList<>();
    private final String commandName;
    @Getter
    private final CommandAPICommand Command;
    private final ConfigParser config;
    private final PartyService service;
    private final ArgumentSuggestions<CommandSender> playerSugg, teamPlaySugg;

    CommandRegister(String name, ConfigParser config, PartyService service, ArgumentSuggestions<CommandSender> playerSuggestor, ArgumentSuggestions<CommandSender> teamplaySuggestor) {
        this.config = config;
        this.service = service;
        this.playerSugg = playerSuggestor;
        this.teamPlaySugg = teamplaySuggestor;
        try{
            Bukkit.getServer().getPluginManager().addPermission(new Permission("MythicParties." + name));
        } catch (IllegalArgumentException ignored) {}
        this.Command = new CommandAPICommand(name){{
            withPermission("MythicParties." + name);
        }};
        commandName = name;
    }
    public CommandAPICommand addSubcommand(String name, BiFunction<CommandSender, CommandArguments, Boolean> consumer, boolean playerArg, boolean slotArg, boolean teamArg){
        CommandAPICommand c = new CommandAPICommand(name)
                .withPermission("MysticParties." + commandName + "." + name)
                .executes((sender, args) -> {commandHandler(consumer, name, sender, args);});
        if(playerArg) c.withArguments(new StringArgument("playerArg").replaceSuggestions(playerSugg));
        if(slotArg) c.withArguments(new IntegerArgument("slotArg").includeSuggestions(ArgumentSuggestions.strings(info -> {
            int i = service.getPartySize((Player) info.sender());
            if(i>10) i = 10;
            List<String> retval = new ArrayList<>();
            for(int f = 1; f<=i; f++){
                retval.add(String.valueOf(f));
            }
            return retval.toArray(String[]::new);
        })));
        if(teamArg) c.withArguments(new StringArgument("playerArg").replaceSuggestions(teamPlaySugg));
        nameList.add(name);
        return c;
    }
    public void commandHandler(BiFunction<CommandSender, CommandArguments, Boolean> consumer, String name, CommandSender sender, CommandArguments args){
        if(!(sender instanceof Player p)) {
            MythicParties.getPlugin().getLogger().info("Plugin doesn't support executing commands from Console");
            return;
        }
        if(!(name == null)) MythicParties.getHandler().playSound(p.getUniqueId(), name);
        boolean success = consumer.apply(sender, args);
        String command = config.getCommand(success, commandName+ "_" + name);
        if(Objects.equals(command, null)) return;
        p.performCommand(command);
    }

    public void addSubPermissions() {
        try{
            nameList.forEach((name) -> Bukkit.getServer().getPluginManager().addPermission(new Permission("MysticParties." + commandName + "." + name)));
        } catch (IllegalArgumentException ignored) {}
    }
    public void registerCommand() {Command.register();}
}

package ru.streamversus.mythicparties.Commands.implementations;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import ru.streamversus.mythicparties.MythicParties;

import java.util.Objects;

public abstract class SubCommandImpl extends CommandImpl{
    private final CommandImpl mainCommand;
    public SubCommandImpl(CommandImpl main, String name) {
        super(name);
        main.withSubcommand(this);
        mainCommand = main;
        executes(this::execute);
    }
    void regPerms(){
        Bukkit.getServer().getPluginManager().addPermission(new Permission("MysticParties." + mainCommand.name + "." + name));
    }
    private void execute(CommandSender sender, CommandArguments args){
        if(!(sender instanceof Player p)) {
            MythicParties.getPlugin().getLogger().info("Plugin doesn't support executing commands from Console");
            return;
        }
        if(!(name == null)) MythicParties.getHandler().playSound(p.getUniqueId(), name);
        boolean success = exec(sender, args);
        String command = MythicParties.getConfigParser().getCommand(success, name+ "_" + name);
        if(Objects.equals(command, null)) return;
        p.performCommand(command);
    }
    public abstract boolean exec(CommandSender sender, CommandArguments args);
}

package ru.streamversus.mythicparties.Commands.implementations;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import ru.streamversus.mythicparties.MythicParties;

import java.util.List;

public abstract class SubCommandImpl extends CommandImpl{
    private final CommandImpl mainCommand;
    private final String subName;
    public SubCommandImpl(CommandImpl main, String name) {
        super(name);
        subName = name;
        main.withSubcommand(this);
        mainCommand = main;
        executes((sender, args) -> {
            var thread = new Thread(() -> execute(sender, args));
            thread.setDaemon(true);
            thread.start();
        });
        withPermission("MythicParties."+ main.name + "." + name);
    }

    void regPerms(){
        Bukkit.getServer().getPluginManager().addPermission(new Permission("MythicParties." + mainCommand.name + "." + subName));
    }

    private void execute(CommandSender sender, CommandArguments args){
        if(!(sender instanceof Player p)) {
            MythicParties.getPlugin().getLogger().info("Plugin doesn't support executing commands from Console");
            return;
        }

        if(!(subName == null)) MythicParties.getHandler().playSound(p.getUniqueId(), subName);

        boolean success = exec(p, args);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MythicParties.getPlugin(), () -> {
            List<String> commandList = MythicParties.getConfigParser().getCommand(success, mainCommand.name+ "_" + subName);
            if(commandList == null) return;
            commandList.forEach(p::performCommand);
        }, 0L);

    }
    public abstract boolean exec(Player sender, CommandArguments args);
}

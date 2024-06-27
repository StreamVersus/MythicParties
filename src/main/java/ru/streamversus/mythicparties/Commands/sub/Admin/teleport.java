package ru.streamversus.mythicparties.Commands.sub.Admin;

import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.Rotation;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.streamversus.mythicparties.Commands.arguments.PlayersBySlotArgument;
import ru.streamversus.mythicparties.Commands.arguments.SubTagArgument;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class teleport extends SubCommandImpl {
    private final ProxyHandler proxy = MythicParties.getHandler();
    //TODO: Server Argument
    public teleport(CommandImpl main){
        super(main, "teleport");
        withArguments(new SubTagArgument("subtag").combineWith(new PlayersBySlotArgument("slots")));
        withOptionalArguments(new LocationArgument("location", LocationType.PRECISE_POSITION));
        withOptionalArguments(new RotationArgument("rotation"));
        withOptionalArguments(new WorldArgument("world"));
        if(MythicParties.getConfigParser().isProxy()){
            withOptionalArguments(new StringArgument("server").replaceSuggestions(ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> proxy.getServerList().toArray(new String[0])))));
        }
    }

    @Override
    public boolean exec(CommandSender sender, CommandArguments args) {
        UUID id = ((Player) sender).getUniqueId();
        Player player = (Player) sender;
        List<OfflinePlayer> teleportList = args.getUnchecked("slots");
        if(teleportList == null){
            proxy.sendMessage(id, "wrong_slots");
            return false;
        }

        for (OfflinePlayer offlinePlayer : teleportList) {
            Location tploc = (Location) args.getOptional("location").orElse((player.getLocation()));
            Rotation rot = (Rotation) args.getOptional("rotation").orElse(new Rotation(player.getYaw(), player.getPitch()));
            World world = (World) args.getOptional("world").orElse(player.getWorld());
            String server = (String) args.getOptional("server").orElse("local");

            tploc.setWorld(world);

            tploc.setPitch(rot.getPitch());
            tploc.setYaw(rot.getYaw());

            proxy.teleportTo(offlinePlayer, server, tploc);
        }
        return true;
    }
}

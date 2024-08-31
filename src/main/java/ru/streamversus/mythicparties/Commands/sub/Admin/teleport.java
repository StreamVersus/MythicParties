package ru.streamversus.mythicparties.Commands.sub.Admin;

import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.Rotation;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
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
    public teleport(CommandImpl main){
        super(main, "teleport");
        withArguments(SubTagArgument.get("subtag").combineWith(PlayersBySlotArgument.get("slots")));
        withArguments(new LocationArgument("location", LocationType.PRECISE_POSITION));
        withArguments(new RotationArgument("rotation"));
        withOptionalArguments(new WorldArgument("world"));
        if(MythicParties.getConfigParser().isProxy()){
            withOptionalArguments(new StringArgument("server").replaceSuggestions(ArgumentSuggestions.stringsAsync(info ->
                    CompletableFuture.supplyAsync(() -> proxy.getServerList().get().toArray(String[]::new)))));
        }
    }

    @Override
    public boolean exec(Player sender, CommandArguments args) {
        UUID id = (sender).getUniqueId();
        List<OfflinePlayer> teleportList = args.getUnchecked("slots");
        if(teleportList == null){
            proxy.sendMessage(id, "wrong_slots");
            return false;
        }

        for (OfflinePlayer offlinePlayer : teleportList) {
            Location tploc = (Location) args.get("location");
            Rotation rot = (Rotation) args.get("rotation");
            World world = (World) args.get("world");
            String server = (String) args.getOptional("server").orElse("local");

            assert tploc != null;
            tploc.setWorld(world);

            assert rot != null;
            tploc.setPitch(rot.getPitch());
            tploc.setYaw(rot.getYaw());

            proxy.teleportTo(offlinePlayer.getUniqueId(), server, tploc);
        }
        return true;
    }
}

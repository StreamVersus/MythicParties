package ru.streamversus.mythicparties.Commands.sub.Main;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import ru.streamversus.mythicparties.Commands.implementations.CommandImpl;
import ru.streamversus.mythicparties.Commands.implementations.SubCommandImpl;
import ru.streamversus.mythicparties.MythicParties;
import ru.streamversus.mythicparties.PartyService;
import ru.streamversus.mythicparties.Proxy.ProxiedConnection;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;

import java.util.ArrayList;
import java.util.List;

public class warp extends SubCommandImpl implements Listener {
    private final List<OfflinePlayer> conlist = new ArrayList<>();
    public warp(CommandImpl main){
        super(main, "warp");
        Plugin plugin = MythicParties.getPlugin();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean exec(Player p, CommandArguments args) {
        //compatibility block
        PartyService service = MythicParties.getPartyService();
        ProxyHandler proxy = MythicParties.getHandler();
        //end

        if(proxy instanceof ProxiedConnection con){
            var party = service.getParty(p).getPlayers().get();

            if(party.size() == 1) {
                proxy.sendMessage(p.getUniqueId(), "warp_no_players");
                return false;
            }

            for (OfflinePlayer player : party) {
                if(player == p) continue;
                if(!con.isOnThisServer(player.getUniqueId())) {
                    con.connectHere(player);
                }
            }
        }

        proxy.sendMessage(p.getUniqueId(), "warp_success");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(conlist.contains(event.getPlayer())){
            MythicParties.getConfigParser().sendMessage(event.getPlayer(), "warp_player_success");
            conlist.remove(event.getPlayer());
        }
    }
}

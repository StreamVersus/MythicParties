package ru.streamversus.mythicparties.Utilities;

import net.playavalon.mythicdungeons.api.party.IDungeonParty;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Party;

import java.util.ArrayList;
import java.util.List;

public class PartyMDWrapper implements IDungeonParty {
    private final Party party;
    public PartyMDWrapper(Party party){
        this.party = party;
    }
    @Override
    public void addPlayer(Player player) {
        party.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        party.removePlayer(player);
    }
    //TODO: connection from other servers
    @Override
    public List<Player> getPlayers() {
        List<OfflinePlayer> op = party.getPlayers();
        List<Player> retval = new ArrayList<>();
        op.forEach(p -> {
            Player p1 = Bukkit.getPlayer(p.getUniqueId());
            retval.add(p1);
        });
        return retval;
    }

    @NotNull
    @Override
    public OfflinePlayer getLeader() {
        return party.getLeader();
    }
}

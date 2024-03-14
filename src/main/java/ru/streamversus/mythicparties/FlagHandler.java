package ru.streamversus.mythicparties;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class FlagHandler extends FlagValueChangeHandler<Integer> {
    public static final Factory FACTORY = new Factory();
    public static class Factory extends Handler.Factory<FlagHandler> {
        @Override
        public FlagHandler create(Session session) {
            return new FlagHandler(session);
        }
    }
    private final Map<UUID, Integer> limit = new HashMap<>();
    public FlagHandler(Session session){
        super(session, MythicParties.getLimitFlag());
    }
    @Override
    protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Integer value) {
        limit.remove(player.getUniqueId());
        if(value == 0) limit.put(player.getUniqueId(), MythicParties.getPlugin().getConfigParser().getLimit());
        else limit.put(player.getUniqueId(), value);
    }

    @Override
    protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Integer currentValue, Integer lastValue, MoveType moveType) {
        limit.remove(player.getUniqueId());
        if(currentValue == 0) limit.put(player.getUniqueId(), MythicParties.getPlugin().getConfigParser().getLimit());
        else limit.put(player.getUniqueId(), currentValue);
        return true;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Integer lastValue, MoveType moveType) {
        limit.remove(player.getUniqueId());
        limit.put(player.getUniqueId(), MythicParties.getPlugin().getConfigParser().getLimit());
        return true;
    }
    public Integer getLimit(UUID player){return limit.get(player);}
}

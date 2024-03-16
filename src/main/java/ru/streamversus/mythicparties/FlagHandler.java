package ru.streamversus.mythicparties;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;

import java.util.*;

public class FlagHandler extends Handler {
    public static final Factory FACTORY = new Factory();
    private static final Set<UUID> FFOffSet = new HashSet<>();
    private static final Map<UUID, Integer> limitMap = new HashMap<>();

    public FlagHandler(Session session) {
        super(session);
    }

    public static class Factory extends Handler.Factory<FlagHandler> {
        @Override
        public FlagHandler create(Session session) {
            return new FlagHandler(session);
        }
    }
    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        entered.forEach((region) -> MythicParties.getPlugin().getLogger().info(region.toString()));
        return true;
    }
}

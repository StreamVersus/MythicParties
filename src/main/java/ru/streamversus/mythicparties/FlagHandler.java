package ru.streamversus.mythicparties;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import lombok.Getter;

import java.util.*;

public class FlagHandler extends Handler {
    public static final Factory FACTORY = new Factory();
    @Getter
    private static final Set<UUID> FFOffSet = new HashSet<>();
    @Getter
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
        limitUtil(player, entered, exited);
        FFUtil(player, entered, exited);
        return true;
    }
    private void limitUtil(LocalPlayer player, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited){
        if(entered.isEmpty() && exited.isEmpty()) return;
        int buffer = 0;
        if(entered.isEmpty()) {limitMap.remove(player.getUniqueId()); return; }
        for(ProtectedRegion region : entered) {
            Integer limit = region.getFlag(MythicParties.getLimitFlag());
            if(limit == null || limit == 0) continue;

            if(buffer < limit) buffer = limit;
        }
        if(buffer != 0) limitMap.put(player.getUniqueId(), buffer);
    }
    private void FFUtil(LocalPlayer player, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited){
        if(entered.isEmpty() && exited.isEmpty()) return;
        boolean buffer = false;
        if(entered.isEmpty()) {
            FFOffSet.remove(player.getUniqueId());
            return;
        }

        for(ProtectedRegion region : entered) {
            StateFlag.State status = region.getFlag(MythicParties.getFFFlag());
            if(status == null) continue;
            if(status == StateFlag.State.DENY) buffer = true;
        }

        if(buffer) FFOffSet.add(player.getUniqueId());
        else FFOffSet.remove(player.getUniqueId());
    }
}

package ru.streamversus.mythicparties;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
    public void tick(LocalPlayer player, ApplicableRegionSet set) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(player.getWorld());
        assert regions != null;
        Set<ProtectedRegion> set1 = new HashSet<>(set.getRegions());
        set1.add(regions.getRegions().get("__global__"));
        limitUtil(player, set1);
        FFUtil(player, set1);
    }
    private void limitUtil(LocalPlayer player, Set<ProtectedRegion> set){
        int limitBuffer = 0;

        for (ProtectedRegion protectedRegion : set) {
            if(protectedRegion == null) continue;
            Integer limit = protectedRegion.getFlag(MythicParties.getLimitFlag());

            if(!(limit == null)) limitBuffer = limitBuffer < limit ? limit : limitBuffer;
        }
        if(limitBuffer == 0) limitBuffer = MythicParties.getConfigParser().getLimit();
        if(limitBuffer != MythicParties.getConfigParser().getLimit()){

            if(limitMap.containsKey(player.getUniqueId())){
                limitMap.replace(player.getUniqueId(), limitBuffer);
            }
            else limitMap.put(player.getUniqueId(), limitBuffer);

        }
        else limitMap.remove(player.getUniqueId());
    }
    private void FFUtil(LocalPlayer player, Set<ProtectedRegion> set){
        boolean buffer = false;

        for (ProtectedRegion protectedRegion : set) {
            StateFlag.State state = protectedRegion.getFlag(MythicParties.getFFFlag());

            if(state == StateFlag.State.DENY) buffer = true;
        }

        if(buffer) FFOffSet.add(player.getUniqueId());
        else FFOffSet.remove(player.getUniqueId());
    }
}

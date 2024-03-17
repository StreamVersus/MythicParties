package ru.streamversus.mythicparties;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.UUID;

public class CompatibilityHandler implements Listener {
    @Getter
    private static IntegerFlag limitFlag;
    @Getter
    private static StateFlag FFFlag;
    private final Plugin plugin;
    private PartyService partyService = null;
    public CompatibilityHandler(Plugin plugin){
        this.plugin = plugin;
        if(Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        IntegerFlag flag = new IntegerFlag("party-limit-flag");
        registry.register(flag);
        limitFlag = flag;

        StateFlag flag2 = new StateFlag("party-friendly-fire", true);
        registry.register(flag2);
        FFFlag = flag2;
    }
    public void onEnable(PartyService service){
        if(!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) plugin.getLogger().info("load");
        partyService = service;
        SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
        sessionManager.registerHandler(FlagHandler.FACTORY, null);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPVP(EntityDamageByEntityEvent event){
        if(event.getDamager().getType() != EntityType.PLAYER && event.getEntity().getType() != EntityType.PLAYER) return;
        Player damager = (Player) event.getDamager();
        Player damaged = (Player) event.getEntity();
        if(Objects.equals(partyService.getPartyID(damager), partyService.getPartyID(damaged))) event.setCancelled(FlagHandler.getFFOffSet().contains(event.getEntity().getUniqueId()));
    }
    public Integer getLimit(UUID p){
       return FlagHandler.getLimitMap().get(p);
    }
}

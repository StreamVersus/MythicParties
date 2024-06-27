package ru.streamversus.mythicparties;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Proxy.*;

import java.io.File;
import java.util.Objects;


public final class MythicParties extends JavaPlugin implements Listener{
    @Getter
    private static PartyService partyService;
    @Getter
    private static ConfigParser configParser;
    @Getter
    private static MythicParties plugin;
    @Getter
    private static IntegerFlag limitFlag;
    @Getter
    private static StateFlag FFFlag;
    @Getter
    private static ProxyHandler handler;

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        limitFlag = (IntegerFlag) registry.get("party-limit-flag");
        FFFlag = (StateFlag) registry.get("party-friendly-fire");

        if (limitFlag == null) {
            IntegerFlag flag = new IntegerFlag("party-limit-flag");
            registry.register(flag);
            limitFlag = flag;
        }

        if (FFFlag == null) {
            StateFlag flag2 = new StateFlag("party-friendly-fire", true);
            registry.register(flag2);
            FFFlag = flag2;
        }
    }

    @Override
    public void onEnable() {
        plugin = this;

        File f = new File(getDataFolder(), "language.yml");
        if (!f.exists()) {
            this.saveResource("language.yml", false);
        }

        configParser = new ConfigParser(this, YamlConfiguration.loadConfiguration(f));

        handler = new Local(configParser);

        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .shouldHookPaperReload(true)
                .silentLogs(configParser.getVerbose()));
        CommandAPI.onEnable();

        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(0), true, true);
        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(1), true, true);
        /*
        if (configParser.isProxy()) {
            if (!Objects.requireNonNull(getServer().spigot().getConfig().getConfigurationSection("settings")).getBoolean("bungeecord")) {
                getLogger().severe("Для корректной работы плагина с прокси, требуется включить поддержку Messaging BungeeCord(Даже для Velocity)");
                getLogger().severe("для этого в spigot.yml поставьте значение bungeecord на true");
                getLogger().severe("Производится отключение плагина");
                getServer().getPluginManager().disablePlugin(this);
            }
        }
        */
        partyService = new PartyService(this, configParser, handler);

        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getOnlinePlayers().forEach(partyService::createParty);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceHolderExpansion(partyService).register();
        }

        WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(FlagHandler.FACTORY, null);
    }

    @SneakyThrows
    @Override
    public void onDisable() {
        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(0), true, true);
        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(1), true, true);
        CommandAPI.onDisable();

        handler.disable();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            partyService.createParty(p);
            partyService.scheduleKick(p, true);
            partyService.scheduleDisband(p, true);
        }, 5);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent event) {
        partyService.scheduler(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPVP(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() != EntityType.PLAYER || event.getEntity().getType() != EntityType.PLAYER) return;
        Player damager = (Player) event.getDamager();
        Player damaged = (Player) event.getEntity();
        if (Objects.equals(partyService.getPartyID(damager), partyService.getPartyID(damaged)))
            event.setCancelled(FlagHandler.getFFOffSet().contains(event.getEntity().getUniqueId()));
    }

}
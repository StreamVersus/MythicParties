package ru.streamversus.mythicparties;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.streamversus.mythicparties.Parsers.ConfigParser;

import java.io.File;


public final class MythicParties extends JavaPlugin implements Listener {
    private PartyService partyService;
    @Getter
    private ConfigParser configParser;
    @Getter
    private static IntegerFlag limitFlag;
    @Getter
    private static FlagHandler handler;
    @Getter
    private static MythicParties plugin;
    @Override
    public void onLoad(){
        if(!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        IntegerFlag flag = new IntegerFlag("party-limit-flag");
        registry.register(flag);
        limitFlag = flag;

        SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
        sessionManager.registerHandler(FlagHandler.FACTORY, null);
    }
    @Override
    public void onEnable() {
        plugin = this;
        File f = new File(getDataFolder(), "language.yml");
        if (!f.exists()) {
            this.saveResource("language.yml", false);
        }
        this.configParser = new ConfigParser(this, YamlConfiguration.loadConfiguration(f));
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .shouldHookPaperReload(true)
                .silentLogs(configParser.getVerbose()));
        CommandAPI.onEnable();
        partyService = new PartyService(this, configParser);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getOnlinePlayers().forEach((player) -> partyService.createParty(player));
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceHolderExpansion(partyService).register();
        }
    }

    @Override
    public void onDisable() {
        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(0), true, true);
        CommandAPIBukkit.unregister(configParser.getCommandNameList().get(1), true, true);
        CommandAPI.onDisable();
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event){
        Player p = event.getPlayer();
        partyService.createParty(p);
        partyService.scheduleKick(p, true);
        partyService.scheduleDisband(p, true);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeave(PlayerQuitEvent event){
        Player p = event.getPlayer();
        partyService.scheduleDisband(p, false);
        partyService.scheduleKick(p, false);
    }
}
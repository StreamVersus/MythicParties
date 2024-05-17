package ru.streamversus.mythicparties.Parsers;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigParser {
    private final YamlConfiguration langconfig;
    private FileConfiguration config;
    private final Plugin plugin;
    private final Map<String, String> commandSuccessMap = new HashMap<>(), commandFailMap = new HashMap<>(), soundMap = new HashMap<>(), langmap = new HashMap<>();
    @Getter
    private List<String> commandNameList;
    @Getter
    private boolean disband;
    @Getter
    private long leaderDisband, playerKick;
    @Getter
    private boolean proxy;
    @Getter
    private String username, password, url, name;

    public ConfigParser(Plugin plugin, YamlConfiguration langconf) {
        this.langconfig = langconf;
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        precompileConfig();
        precompileLanguage();
    }

    private void precompileConfig() {
        proxy = config.getBoolean("proxy_support");
        ConfigurationSection commandSection = config.getConfigurationSection("command_trigger");
        assert commandSection != null;
        commandSection.getKeys(false).forEach((string) -> {
            ConfigurationSection subsection = commandSection.getConfigurationSection(string);
            assert subsection != null;
            String success = subsection.getString("commands");
            String fail = subsection.getString("commands_fail");
            if (success != null) commandSuccessMap.put(string, success);
            if (fail != null) commandFailMap.put(string, fail);
        });

        ConfigurationSection soundSection = config.getConfigurationSection("sound_notification");
        assert soundSection != null;
        soundSection.getKeys(false).forEach((string) -> soundMap.put(string, soundSection.getString(string)));
        this.commandNameList = config.getStringList("commands_name");

        ConfigurationSection disbandSection = config.getConfigurationSection("timer_to_kick");
        assert disbandSection != null;
        disband = disbandSection.getBoolean("disband");
        leaderDisband = disbandSection.getLong("leader");
        playerKick = disbandSection.getLong("participant");

        ConfigurationSection database = config.getConfigurationSection("mysql");
        assert database != null;
        username = database.getString("username");
        password = database.getString("password");
        url = database.getString("url");
        name = database.getString("name");
    }
    private void precompileLanguage(){
        langconfig.getKeys(false).forEach((key) -> {
            String v = langconfig.getString(key);
            if(!(v == null)) langmap.put(key, v);
        });
        langmap.forEach((name, content) -> {
            String acceptTag = "<click:run_command:" + "/" + getCommandNameList().get(0) + " accept" + ">";
            String rawAccept = acceptTag + langconfig.getString("button_accept") + "</click>";
            langmap.replace(name, content.replaceAll("\\$button_accept\\$", rawAccept));
        });
        langmap.forEach((name, content) -> {
            String refuseTag = "<click:run_command:" + "/" + getCommandNameList().get(0) + " refuse" + ">";
            String rawRefuse = refuseTag + langconfig.getString("button_refuse") + "</click>";
            langmap.replace(name, content.replaceAll("\\$button_refuse\\$", rawRefuse));
        });
        langmap.forEach((name, content) -> {
            float raw = getLeaderDisband() / 20f;
            String s = String.valueOf(raw);
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            langmap.replace(name, content.replaceAll("\\$disband_seconds\\$", s));
        });
        langmap.forEach((name, content) -> {
            float raw = getLeaderDisband() / 20f / 60f;
            String s = String.valueOf(raw);
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            langmap.replace(name, content.replaceAll("\\$disband_minutes\\$", s));
        });
        langmap.forEach((name, content) -> {
            float raw = getPlayerKick() / 20f;
            String s = String.valueOf(raw);
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            langmap.replace(name, content.replaceAll("\\$kick_seconds\\$", s));
        });
        langmap.forEach((name, content) -> {
            float raw = getPlayerKick() / 20f / 60f;
            String s = String.valueOf(raw);
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            langmap.replace(name, content.replaceAll("\\$kick_minutes\\$", s));
        });
    }

    public String getCommand(boolean status, String name) {
        return status ? commandSuccessMap.get(name) : commandFailMap.get(name);
    }

    public void playSound(String name, Player p) {
        String soundName = soundMap.get(name);
        if (soundName == null) return;
        String[] split = soundName.split(" ");
        Sound sound = null;
        try {
            sound = Sound.valueOf(split[0]);
        }catch(Exception ignored){}
        if(sound == null) p.playSound(p, split[0], Float.parseFloat(split[1]), Float.parseFloat(split[2]));
        else p.playSound(p, sound, Float.parseFloat(split[1]), Float.parseFloat(split[2]));
    }

    public boolean getVerbose() {
        return config.getBoolean("verbose");
    }

    public int getLimit() {
        return config.getInt("global_max_limit");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        precompileConfig();
        precompileLanguage();
    }

    public boolean sendMessage(Player p, String key){
        String raw = langmap.get(key);
        if(raw == null) return false;
        Component replaced = MiniMessage.miniMessage().deserialize(raw.replaceAll("\\$player_sender\\$", p.getName()));
        p.sendMessage(replaced);
        return true;
    }

    public void sendWithReplacer(Player receiver, String replacer, String key){
        String raw = langmap.get(key);
        if(raw == null) return;
        Component replaced = MiniMessage.miniMessage().deserialize(raw.replaceAll("\\$player_receiver\\$", receiver.getName()).replaceAll("\\$player_replacer\\$", replacer));
        receiver.sendMessage(replaced);
    }
}

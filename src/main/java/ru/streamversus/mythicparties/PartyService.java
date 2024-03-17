package ru.streamversus.mythicparties;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.Rotation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import ru.streamversus.mythicparties.Parsers.ConfigParser;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PartyService {
    private final Plugin plugin;
    @Getter
    private static ArgumentSuggestions<CommandSender> slotSuggestor = null;
    private final ArgumentSuggestions<CommandSender> subTagSuggestor;
    private final Map<UUID, BukkitTask> disbandTask = new HashMap<>(), kickTask = new HashMap<>();
    private final ConfigParser config;
    private final CommandRegister partycommandRegister, partyadmincommandRegister;
    private final Map<UUID, Party> leaderMap = new HashMap<>(), partyMap = new HashMap<>(), invitedMap = new HashMap<>();

    PartyService(Plugin plugin, ConfigParser config) {
        this.config = config;
        this.plugin = plugin;
        this.partycommandRegister = new CommandRegister(config.getCommandNameList().get(0), config);
        this.partyadmincommandRegister = new CommandRegister(config.getCommandNameList().get(1), config);
        slotSuggestor = ArgumentSuggestions.strings(info -> {
            if(Objects.equals(info.currentArg(), "all")) return new String[]{"all"};
            String subtag = (String) info.previousArgs().get("subtag");
            if(subtag == null) return null;
            Party party = parsesubTag(subtag, (Player) info.sender());
            String[] raw = {""};
            if(!info.currentArg().isEmpty()) raw[0] = "all/";
            String currentinput = info.currentArg();
            if(currentinput.startsWith("a")) return new String[]{"all"};
            party.forEach((player) -> {
                String id = String.valueOf(getPlayerID(player));
                if(id == null) return;
                if(currentinput.contains(id)) return;
                raw[0] = raw[0] + "/" + id;
            });
            return raw;}
        );
        subTagSuggestor = ArgumentSuggestions.strings(info -> {
            String currentArg = info.currentArg();
            if(currentArg.contains("@i")) {
                int id = Party.idList.toArray().length;
                if (id > 9) id = 9;
                List<String> retval = new ArrayList<>();
                for (int i = 0; i < id; i++) {
                    retval.add("@id_" + i);
                }
                return retval.toArray(String[]::new);
            }
            if(currentArg.contains("@p")){
                List<String> retval = new ArrayList<>();
                Collection<? extends Player> playerList = Bukkit.getOnlinePlayers();
                int l = playerList.size();
                if(l > 9) l = 9;
                for (int i = 0; i < l; i++) {
                    retval.add("@player_" + playerList.toArray()[i]);
                }
                return retval.toArray(String[]::new);
            }
            if(currentArg.contains("@t")){
                return new String[]{"@trugger_team"};
            }
            String[] retval = new String[3];
            retval[0] = "@trugger_team";
            retval[1] = "@id_*";
            retval[2] = "@player_*";
            return retval;
        });
        commandParty();
        commandParty_a();
    }
    public void scheduleDisband(Player p, boolean disable){
        if(!config.isDisband()) return;
        Party party = leaderMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {BukkitTask t = disbandTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else disbandTask.put(p.getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> disband(p), config.getLeaderDisband(), 0L));
    }
    public void scheduleKick(Player p, boolean disable){
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {BukkitTask t = kickTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else kickTask.put(p.getUniqueId(), plugin.getServer().getScheduler().runTaskTimer(plugin, () -> kick(p), config.getPlayerKick(), 0L));
    }
    public void createParty(Player p) {
        if(leaderMap.get(p.getUniqueId()) != null || partyMap.get(p.getUniqueId()) != null) return;
        UUID id = p.getUniqueId();
        if (leaderMap.get(id) != null) return;
        Party party = new Party(id, plugin, config);
        leaderMap.put(id, party);
        partyMap.put(id, party);
    }

    public boolean isntLeader(Player p) {
        return leaderMap.get(p.getUniqueId()) == null;
    }
    public boolean leave(Player p) {
        if(leaderMap.get(p.getUniqueId()) != null) return config.sendMessage(p, "leave_leader");
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return config.sendMessage(p, "leave_no_party");
        party.removePlayer(p);
        return true;
    }

    public boolean invite(Player p, CommandArguments args) {
        if (isntLeader(p)) return config.sendMessage(p, "invite_non_leader");
        Player invitedPlayer = (Player) args.get("playerArg");
        if (invitedPlayer == null) return config.sendMessage(p, "invite_no_player");
        Party party = leaderMap.get(p.getUniqueId());
        if(party.getLimit() < party.getPlayerCount()+2) return config.sendMessage(p, "invite_party_full");
        if(invitedMap.get(invitedPlayer.getUniqueId()) != null) return config.sendMessage(p, "invite_already_invited");
        invitedMap.put(invitedPlayer.getUniqueId(), party);
        config.sendInvite(p, "invite_alert", invitedPlayer);
        return true;
    }

    public boolean giveLead(Player p, CommandArguments args) {
        if (isntLeader(p)) return config.sendMessage(p, "givelead_non_leader");
        Player leadingPlayer = (Player) args.get("playerArg");
        if (leadingPlayer == null) return config.sendMessage(p, "givelead_no_player");
        Party party = leaderMap.remove(p.getUniqueId());
        leaderMap.put(leadingPlayer.getUniqueId(), party);
        party.changeLeaderUUID(leadingPlayer.getUniqueId());
        config.sendMessage(leadingPlayer, "givelead_alert");
        return true;
    }
    private void kick(Player p){
        Party party = partyMap.remove(p.getUniqueId());
        party.removePlayer(p);
        config.sendMessage(p, "kick_alert");
    }
    public boolean kickWrapper(Player p, CommandArguments args) {
        if (isntLeader(p)) return config.sendMessage(p, "kick_non_leader");
        Player kickedPlayer = (Player) args.get("playerArg");
        if (kickedPlayer == null) return config.sendMessage(p, "kick_no_player");
        kick(kickedPlayer);
        return true;
    }
    public boolean disband(Player p) {
        if (isntLeader(p)) return config.sendMessage(p, "disband_non_leader");
        Party party = leaderMap.remove(p.getUniqueId());
        party.forEach((player) -> {
            if(player != p) partyMap.remove(player.getUniqueId());
        });
        invitedMap.forEach((key, value) -> {
            if (value == party) invitedMap.remove(key);
        });
        party.forEach((player) -> config.sendMessage(player, "disband_alert"));
        return true;
    }
    public boolean swapSlots(Player send, CommandArguments args){
        if(isntLeader(send)) return config.sendMessage(send, "swap_non_leader");
        Player p = (Player) args.get("playerArg");
        Party party = leaderMap.get(send.getUniqueId());
        Integer slot = (Integer) args.get("slotArg");
        if(slot == null || p == null) return config.sendMessage(send, "swap_wrong_args");
        Player m = party.getPlayer(slot);
        party.swapPlayers(p, m);
        return true;
    }
    public boolean accept(Player sender){
        if(!invitedMap.containsKey(sender.getUniqueId())) return config.sendMessage(sender, "accept_no_invites");
        Player invitesender = (Player) invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return config.sendMessage(sender, "accept_wrong_args:");
        invitedMap.remove(sender.getUniqueId());
        leaderMap.get(invitesender.getUniqueId()).addPlayer(sender);
        config.sendInvite(invitesender, "invite_accepted", sender);
        return true;
    }
    public boolean refuse(Player sender){
        if(!invitedMap.containsKey(sender.getUniqueId())) return config.sendMessage(sender, "accept_no_invites");
        Player invitesender = (Player) invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return config.sendMessage(sender, "accept_wrong_args:");
        invitedMap.remove(sender.getUniqueId());
        config.sendInvite(invitesender, "invite_refused", sender);
        return true;
    }
    public boolean help(Player sender){
        return config.sendMessage(sender, "help");
    }
    private void commandParty(){
        Map<String, BiFunction<CommandSender, CommandArguments, Boolean>> submap = new HashMap<>(){{
            put("help", (player, args) -> help((Player) player));
            put("leave", (player, args) -> leave((Player) player));
            put("invite", (player, args) -> invite((Player) player, args));
            put("givelead", (player, args) -> giveLead((Player) player, args));
            put("kick", (player, args) -> kickWrapper((Player) player, args));
            put("disband", (player, args) -> disband((Player) player));
            put("slotplayer", (player, args) -> swapSlots((Player) player, args));
            put("accept", (player, args) -> accept((Player) player));
            put("refuse", (player, args) -> refuse((Player) player));
        }};
        Map<String, Boolean> playerArgmap = new HashMap<>(){{
            put("invite", true);
            put("givelead", true);
            put("kick", true);
        }};
        Map<String, Boolean> slotArgmap = new HashMap<>(){{
            put("slotplayer", true);
        }};
        partycommandRegister.addSubPermissions();
        CommandAPICommand command = partycommandRegister.getCommand();
        submap.forEach((name, executor)-> command.withSubcommand(partycommandRegister.addSubcommand(name, executor, playerArgmap.getOrDefault(name, false), slotArgmap.getOrDefault(name, false))));
        partycommandRegister.registerCommand();
    }
    private Party parsesubTag(String subtag, Player sender){
        Party party;
        if(subtag.equals("trugger_party")) party = partyMap.get(sender.getUniqueId());
        else if(subtag.matches("id_.*")) {
            Integer partyId = Integer.parseInt(subtag.substring(3));
            if(partyId > Party.getPartyCount()) config.sendMessage(sender, "wrong_subtag");
            party = Party.getPartyByID(partyId);
        } else if (subtag.matches("player_.*")) {
            Player p = Bukkit.getPlayer(subtag.substring(7));
            if(p == null) {config.sendMessage(sender, "wrong_subtag"); throw new NullPointerException();}
            party = partyMap.get(p.getUniqueId());
        } else throw new NullPointerException();
        return party;
    }
    private void commandParty_a(){
        CommandAPICommand command = partyadmincommandRegister.getCommand();
        CommandAPICommand teleport = new CommandAPICommand("teleport") {{
            withPermission("MysticParties.party_a.teleport");
            withArguments(new StringArgument("subtag").includeSuggestions(subTagSuggestor));
            withArguments(new StringArgument("slots").includeSuggestions(slotSuggestor));
            withArguments(new LocationArgument("location", LocationType.PRECISE_POSITION));
            withArguments(new RotationArgument("rotation"));
            withArguments(new WorldArgument("world"));
            withArguments(new StringArgument("server"));
            executes((sender, args) -> {
                Party party;
                List<Player> teleportList = new ArrayList<>();
                String subtag = (String) args.get("subtag");
                assert subtag != null;
                try{
                    party = parsesubTag(subtag, (Player) sender);
                }catch(Exception e){
                    throw CommandAPI.failWithMessage(config.getMessage("wrong_subtag"));
                }
                String slot = (String) args.get("slots");
                assert slot != null;
                if(slot.equals("all")) {
                    teleportList = party.getPlayers();
                }else{
                    String[] slots = (slot).split("/");
                    for (String s : slots) {teleportList.add(party.getPlayer(Integer.parseInt(s)));}
                }
                Location tploc = (Location) args.get("location");
                Rotation rot = (Rotation) args.get("rotation");
                assert tploc != null;
                assert rot != null;
                tploc.setYaw(rot.getYaw());
                tploc.setPitch(rot.getPitch());
                System.out.println(teleportList);
                teleportList.forEach((Player) -> Player.teleport(tploc));
            });
        }};
        CommandAPICommand execute = new CommandAPICommand("command"){{
            withPermission("MysticParties.party_a.command");
            withArguments(new StringArgument("subtag").includeSuggestions(subTagSuggestor));
            withArguments(new StringArgument("slots").includeSuggestions(slotSuggestor));
            withArguments(new GreedyStringArgument("command"));
            executes((sender, args) -> {
                List<Player> executeList = new ArrayList<>();
                String command = (String) args.get("command");
                if(command == null || !command.matches("/.*")) throw CommandAPI.failWithMessage(config.getMessage("command_wrong_args"));
                Party party = parsesubTag((String) Objects.requireNonNull(args.get("subtag")), (Player) sender);
                String slot = (String) args.get("slots");
                assert slot != null;
                if(slot.equals("all")) {
                    executeList = party.getPlayers();
                }else{
                    String[] slots = (slot).split("/");
                    for (String s : slots) {executeList.add(party.getPlayer(Integer.parseInt(s)));}
                }
                List<Boolean> buffer = new ArrayList<>();
                for (Player player : executeList) {
                    buffer = Collections.singletonList(player.performCommand(command));
                }
                if(buffer.contains(false)) throw CommandAPI.failWithMessage(config.getMessage("command_fail"));
            });
        }};
        CommandAPICommand reloadConfig = new CommandAPICommand("reloadConfig")
                .withPermission("MysticParties.party_a.reloadconfig")
                .executes((sender, args) -> {config.reloadConfig();});
        command.withSubcommand(teleport);
        command.withSubcommand(execute);
        command.withSubcommand(reloadConfig);
        try{
            Bukkit.getServer().getPluginManager().addPermission(new Permission("MysticParties.party_a.teleport"));
            Bukkit.getServer().getPluginManager().addPermission(new Permission("MysticParties.party_a.command"));
            Bukkit.getServer().getPluginManager().addPermission(new Permission("MysticParties.party_a.reloadconfig"));
        } catch (IllegalArgumentException ignored) {}
        command.register();
    }
    public Integer getPartyID(Player p){
        return partyMap.get(p.getUniqueId()).getId();
    }
    public String getLeaderName(Player p){
        return partyMap.get(p.getUniqueId()).getLeader().getName();
    }
    public String getPlayerName(Player p, Integer i){
        Player arg = partyMap.get(p.getUniqueId()).getPlayer(i);
        if(arg == null) return "";
        return arg.getName();
    }
    public Integer getPartySize(Player p){
        return partyMap.get(p.getUniqueId()).getPlayerCount();
    }
    public Integer getFreeSlots(Player p){
        Party party = partyMap.get(p.getUniqueId());
        return party.getLimit() - party.getPlayers().toArray().length;
    }
    public Integer getPlayerID(Player p){
        return partyMap.get(p.getUniqueId()).getPlayerID(p);
    }
    public Integer getPartyLimit(Player p){
        return partyMap.get(p.getUniqueId()).getLimit();
    }
    public Boolean isSlotBusy(Player p, Integer i){
        return partyMap.get(p.getUniqueId()).getPlayer(i) != null;
    }
    public Boolean isPlayerLeader(Player p){
        return leaderMap.get(p.getUniqueId()) != null;
    }
    public Boolean isPlayerParticipant(Player p){
        if(leaderMap.get(p.getUniqueId()) != null) return false;
        return partyMap.get(p.getUniqueId()) != null;
    }
    public Player getmember(Player p, Integer i){
        return partyMap.get(p.getUniqueId()).getPlayer(i);
    }
}
class Party {
    private final ConfigParser config;
    private int maxPlayer;
    private static Class<?> clazz;
    private static Boolean compatStatus;
    private UUID leaderUUID;
    private final List<UUID> playerUUIDs = new ArrayList<>();
    @Getter
    private final int id;
    public static final List<Party> idList = new ArrayList<>();
    Party(UUID leader, Plugin plugin, ConfigParser config) {
        this.config = config;
        updateLimit();
        idList.add(this);
        this.id = idList.indexOf(this);
        this.leaderUUID = leader;
        this.playerUUIDs.add(leader);
        if(compatStatus == null) {
            if (Bukkit.getPluginManager().isPluginEnabled("MythicDungeons")) {
                compatStatus = false;
            }else{
                try {
                    Class<?> mythic = Class.forName("net.playavalon.mythicdungeons.MythicDungeons");
                    plugin.getLogger().info("test");
                    Field f = mythic.getDeclaredField("plugin");
                    f.setAccessible(true);
                    compatStatus = ((String) mythic.getMethod("getPartyPluginName").invoke(f.get(null))).equalsIgnoreCase(plugin.getName());
                } catch(Exception ignored){}
            }
            if(compatStatus == null) compatStatus = false;
        }
        if(compatStatus) {
            try {
                if (!Party.clazz.getName().equals("IDungeonParty"))
                    Party.clazz = Class.forName("net.playavalon.mythicdungeons.api.party.IDungeonParty");
            } catch(Exception ignored){}
            Object party = Proxy.newProxyInstance(clazz.getClassLoader(),
                    new Class[] {Party.clazz},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "addPlayer" -> addPlayer((Player) args[0]);
                            case "removePlayer" -> removePlayer((Player) args[0]);
                            case "getPlayers" -> {return getPlayers();}
                            case "getLeader" -> getLeader();
                            default -> throw new IllegalArgumentException("Proxy error!!!");
                        }
                        return null;
                    });
            try {
                clazz.getMethod("initDungeonParty", Plugin.class).invoke(party, plugin);
            }catch(Exception ignored){}
        }
    }
    private void updateLimit(){
        Integer limit = FlagHandler.getLimitMap().get(leaderUUID);
        System.out.println(limit == null ? config.getLimit() : limit);
        maxPlayer = limit == null ? config.getLimit() : limit;
    }
    public static Party getPartyByID(Integer id){return idList.get(id);}
    public static Integer getPartyCount(){return idList.toArray().length-1;}
    public void addPlayer(Player player) {
        playerUUIDs.add(player.getUniqueId());
    }
    public void forEach(Consumer<Player> cons) {
        for (UUID p : playerUUIDs) {
            cons.accept(Bukkit.getPlayer(p));
        }
    }
    public int getPlayerID(Player p){
        return playerUUIDs.indexOf(p.getUniqueId());
    }
    public int getLimit(){
        updateLimit();
        return maxPlayer;
    }
    public void removePlayer(Player player) {
        playerUUIDs.remove(player.getUniqueId());
    }
    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        playerUUIDs.forEach(uuid -> players.add(Bukkit.getPlayer(uuid)));
        return players;
    }
    public Player getPlayer(int id){return Bukkit.getPlayer(playerUUIDs.get(id));}
    public void swapPlayers(Player p, Player m){
        int firstindex = playerUUIDs.indexOf(p.getUniqueId());
        int secondindex = playerUUIDs.indexOf(m.getUniqueId());
        config.sendMessage(p, "swap_wrong_args");
        if(firstindex == -1 || secondindex == -1) return;
        playerUUIDs.remove(firstindex);
        playerUUIDs.remove(secondindex);
        playerUUIDs.add(secondindex, p.getUniqueId());
        playerUUIDs.add(firstindex, m.getUniqueId());
    }
    public int getPlayerCount(){
        return playerUUIDs.toArray().length;
    }
    public void changeLeaderUUID(UUID p) {leaderUUID = p;}
    public @NotNull OfflinePlayer getLeader() {
        Player p = Bukkit.getPlayer(leaderUUID);
        assert p != null;
        return p;
    }
}

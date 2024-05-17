package ru.streamversus.mythicparties;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.Rotation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.streamversus.mythicparties.Parsers.ConfigParser;
import ru.streamversus.mythicparties.Proxy.ProxyHandler;
import ru.streamversus.mythicparties.database.dbMap;
import ru.streamversus.mythicparties.database.invitedMap;
import ru.streamversus.mythicparties.database.leaderMap;
import ru.streamversus.mythicparties.database.playerMap;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class PartyService {
    private final Plugin plugin;
    private final ArgumentSuggestions<CommandSender> subTagSuggestor, slotSuggestor;
    private final Map<UUID, BukkitTask> disbandTask = new HashMap<>(), kickTask = new HashMap<>();
    private final ConfigParser config;
    private final CommandRegister partycommandRegister, partyadmincommandRegister;
    private final dbMap<UUID, Party> partyMap, invitedMap;
    public final static dbMap<UUID, Party> leaderMap = new leaderMap();
    private final ProxyHandler proxy;

    PartyService(Plugin plugin, ConfigParser config, ProxyHandler proxy) {
        this.config = config;
        this.plugin = plugin;
        this.proxy = proxy;

        this.partyMap = new playerMap();
        this.invitedMap = new invitedMap();

        ArgumentSuggestions<CommandSender> playerSuggestor = ArgumentSuggestions.strings(info -> {
            List<String> playerList = proxy.getPlayerList();
            playerList.remove(info.sender().getName());
            List<String> retval = new ArrayList<>();
            if (Objects.equals(info.currentArg(), "")) {
                int index = playerList.size();
                if (index > 10) index = 10;
                return playerList.subList(0, index).toArray(String[]::new);
            }
            playerList.forEach(string -> {
                if (string.matches(info.currentArg() + ".*")) {
                    retval.add(string);
                }
            });
            return retval.toArray(String[]::new);
        });
        ArgumentSuggestions<CommandSender> teamplayerSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            Player p = (Player) info.sender();
            List<String> retval = new ArrayList<>();
            for (OfflinePlayer player : partyMap.get(p.getUniqueId()).getPlayers()) {
                retval.add(player.getName());
            }
            retval.remove(p.getName());
            return retval.toArray(String[]::new);
        }));
        slotSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            if(Objects.equals(info.currentArg(), "all")) return new String[]{"all"};
            String subtag = (String) info.previousArgs().get("subtag");
            if(subtag == null) return null;
            Party party = parsesubTag(subtag, (Player) info.sender());
            String[] raw = {""};
            if(info.currentArg().isEmpty()) raw[0] = "all";
            String currentinput = info.currentArg();
            if(currentinput.startsWith("a")) return new String[]{"all"};
            party.forEach((player) -> {
                String id = String.valueOf(getPlayerID(player));
                if(id == null) return;
                if(currentinput.contains(id)) return;
                raw[0] = raw[0] + "/" + id;
            });
            return raw;
        }));
        subTagSuggestor = ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
            String currentArg = info.currentArg();
            if(currentArg.contains("i")) {
                int id = Party.idMap.idSet().size();
                if (id > 9) id = 9;
                List<String> retval = new ArrayList<>();
                for (int i = 0; i < id; i++) {
                    retval.add("id_" + i);
                }
                if(retval.contains(currentArg)) return new String[]{currentArg};
                return retval.toArray(String[]::new);
            }
            if(currentArg.contains("p")){
                List<String> retval = new ArrayList<>();
                Collection<? extends Player> playerList = Bukkit.getOnlinePlayers();
                int l = playerList.size();
                if(l > 9) l = 9;
                for (int i = 0; i < l; i++) {
                    Player p = (Player) playerList.toArray()[i];
                    retval.add("player_" + p.getName());
                }
                return retval.toArray(String[]::new);
            }
            if(currentArg.contains("t")){
                return new String[]{"trugger_party"};
            }
            String[] retval = new String[3];
            retval[0] = "trugger_party";
            retval[1] = "id_*";
            retval[2] = "player_*";
            return retval;
        }));
        this.partycommandRegister = new CommandRegister(config.getCommandNameList().get(0), config, this, playerSuggestor, teamplayerSuggestor);
        this.partyadmincommandRegister = new CommandRegister(config.getCommandNameList().get(1), config, this, playerSuggestor, teamplayerSuggestor);
        commandParty();
        commandParty_a();
    }
    public void scheduleDisband(OfflinePlayer p, boolean disable){
        if(!config.isDisband()) return;
        Party party = leaderMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {
            BukkitTask t = disbandTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else {
            disbandTask.put(p.getUniqueId(), new BukkitRunnable() {
                @Override
                public void run() {
                    leaveDisband(p);
                }
            }.runTaskLater(this.plugin, config.getLeaderDisband()));
            party.forEach(player -> proxy.sendMessage(player.getUniqueId(), "leader_offline"));
        }
    }
    private void leaveDisband(OfflinePlayer p){
        Party party = leaderMap.remove(p.getUniqueId());
        disband(p);
        party.destroy();
        scheduleDisband(p, true);
    }
    public void scheduleKick(OfflinePlayer p, boolean disable){
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return;
        if(disable) {BukkitTask t = kickTask.remove(p.getUniqueId());
            if(t == null) return;
            t.cancel();}
        else {
            kickTask.put(p.getUniqueId(), new BukkitRunnable() {
                @Override
                public void run() {
                    scheduledkick(p);
                }
            }.runTaskLater(this.plugin, config.getLeaderDisband()));
            party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(),"player_offline", p.getName()));
        }
    }
    public void createParty(Player p) {
        if(leaderMap.get(p.getUniqueId()) != null || partyMap.get(p.getUniqueId()) != null) return;
        UUID id = p.getUniqueId();
        if (leaderMap.get(id) != null) return;
        Party party = new Party(id, plugin, config, proxy);
        leaderMap.add(id, party);
        partyMap.add(id, party);
    }

    public void createParty(UUID id) {
        if(leaderMap.get(id) != null || partyMap.get(id) != null) return;
        if(leaderMap.get(id) != null) return;
        Party party = new Party(id, plugin, config, proxy);
        leaderMap.add(id, party);
        partyMap.add(id, party);
    }

    public boolean isntLeader(Player p) {
        return leaderMap.get(p.getUniqueId()) == null;
    }
    public boolean isntLeader(OfflinePlayer p) {
        return leaderMap.get(p.getUniqueId()) == null;
    }
    public boolean leave(OfflinePlayer p) {
        if(leaderMap.contains(p.getUniqueId())) return proxy.sendMessage(p.getUniqueId(), "leave_leader");
        Party party = partyMap.get(p.getUniqueId());
        if(party == null) return proxy.sendMessage(p.getUniqueId(), "leave_no_party");
        party.removePlayer(p.getUniqueId());
        createParty(p.getUniqueId());
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), "leave_alert", p.getName()));
        proxy.sendMessage(p.getUniqueId(), "leave_selfalert");
        return true;
    }

    public boolean invite(OfflinePlayer p, CommandArguments args) {
        if (isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "invite_non_leader");
        String inviteName = (String) args.get("playerArg");
        if(!proxy.getPlayerList().contains(inviteName)) return proxy.sendMessage(p.getUniqueId(), "invite_no_player");
        assert inviteName != null;
        OfflinePlayer invitedPlayer =  Bukkit.getOfflinePlayer(inviteName);
        if(p == invitedPlayer) return proxy.sendMessage(p.getUniqueId(), "invite_self");
        Party party = leaderMap.get(p.getUniqueId());
        if(party.getLimit() < party.getPlayerCount()+1) return proxy.sendMessage(p.getUniqueId(), "invite_party_full");
        if(invitedMap.get(invitedPlayer.getUniqueId()) != null) return proxy.sendMessage(p.getUniqueId(), "invite_already_invited");
        invitedMap.add(invitedPlayer.getUniqueId(), party);
        proxy.sendWithReplacer(invitedPlayer.getUniqueId(),"invite_alert", p.getName());
        party.forEach((player) -> proxy.sendWithReplacer(player.getUniqueId(), "invite_sended", invitedPlayer.getName()));
        return true;
    }

    public boolean giveLead(OfflinePlayer p, CommandArguments args) {
        if (isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "givelead_non_leader");
        OfflinePlayer leadingPlayer = (OfflinePlayer) args.get("playerArg");
        if (leadingPlayer == null) return proxy.sendMessage(p.getUniqueId(), "givelead_no_player");
        Party party = leaderMap.remove(p.getUniqueId());
        leaderMap.add(leadingPlayer.getUniqueId(), party);
        party.changeLeaderUUID(leadingPlayer.getUniqueId());
        proxy.sendMessage(leadingPlayer.getUniqueId(), "givelead_alert");
        proxy.sendWithReplacer(p.getUniqueId(),"givelead_selfalert", leadingPlayer.getName());
        return true;
    }
    public void kick(OfflinePlayer p){
        Party party = partyMap.remove(p.getUniqueId());
        party.removePlayer(p.getUniqueId());
        createParty(p.getUniqueId());
        proxy.sendMessage(p.getUniqueId(), "kick_alert");
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), p.getName(), "kick_selfalert"));
    }
    public void scheduledkick(OfflinePlayer p){
        Party party = partyMap.get(p.getUniqueId());
        party.removePlayer(p.getUniqueId());
        createParty(p.getUniqueId());
        proxy.sendMessage(p.getUniqueId(), "kick_alert");
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), p.getName(), "kick_selfalert"));
        scheduleKick(p, true);
    }
    public boolean kickWrapper(Player p, CommandArguments args) {
        if (isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "kick_non_leader");
        Player kickedPlayer = (Player) args.get("playerArg");
        if (kickedPlayer == null) return proxy.sendMessage(p.getUniqueId(), "kick_no_player");
        kick(kickedPlayer);
        proxy.sendWithReplacer(p.getUniqueId(),"kick_selfalert", kickedPlayer.getName());
        return true;
    }
    public boolean disband(OfflinePlayer p) {
        if (isntLeader(p)) return proxy.sendMessage(p.getUniqueId(), "disband_non_leader");
        Party party = leaderMap.get(p.getUniqueId());
        List<OfflinePlayer> kickList = party.getPlayers();
        kickList.forEach(p1 -> {
            if(p1 == p) return;
            party.removePlayer(p1.getUniqueId());
            createParty(p1.getUniqueId());
        });
        kickList.forEach((player) -> proxy.sendMessage(player.getUniqueId(), "disband_alert"));
        return true;
    }
    public boolean swapSlots(OfflinePlayer send, CommandArguments args){
        if(isntLeader(send)) return proxy.sendMessage(send.getUniqueId(), "swap_non_leader");
        OfflinePlayer p = (OfflinePlayer) args.get("playerArg");
        Party party = leaderMap.get(send.getUniqueId());
        Integer slot = (Integer) args.get("slotArg");
        if(slot == null || p == null) return proxy.sendMessage(send.getUniqueId(), "swap_wrong_args");
        if(slot == 1) return proxy.sendMessage(send.getUniqueId(), "swap_leader");
        int i = party.getPlayerID(p.getUniqueId());
        if(i == -1) return proxy.sendMessage(send.getUniqueId(), "swap_empty_slot");
        if(i == slot) return proxy.sendMessage(send.getUniqueId(), "swap_already");
        OfflinePlayer m;
        try {
            m = party.getPlayer(slot);
        } catch(Exception e){
            return proxy.sendMessage(send.getUniqueId(), "swap_empty_slot");
        }
        party.swapPlayers(p, m);
        return true;
    }
    public boolean accept(Player sender){
        if(!invitedMap.contains(sender.getUniqueId())) return proxy.sendMessage(sender.getUniqueId(), "accept_no_invites");
        OfflinePlayer invitesender = invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return proxy.sendMessage(sender.getUniqueId(), "accept_wrong_args:");
        invitedMap.remove(sender.getUniqueId());
        Party party = leaderMap.get(invitesender.getUniqueId());
        party.addPlayer(sender);
        party.forEach(player -> proxy.sendWithReplacer(player.getUniqueId(), "invite_accepted", sender.getName()));
        leaderMap.remove(sender.getUniqueId()).destroy();
        partyMap.replace(sender.getUniqueId(), party);
        return true;
    }
    public boolean refuse(Player sender){
        if(!invitedMap.contains(sender.getUniqueId())) return proxy.sendMessage(sender.getUniqueId(), "accept_no_invites");
        Player invitesender = (Player) invitedMap.get(sender.getUniqueId()).getLeader();
        if(leaderMap.get(invitesender.getUniqueId()) == null || invitedMap.get(sender.getUniqueId()).getLeader() != invitesender) return proxy.sendMessage(sender.getUniqueId(), "accept_wrong_args:");
        invitedMap.remove(sender.getUniqueId()).forEach(player -> proxy.sendWithReplacer(player.getUniqueId(),"invite_refused", sender.getName()));
        proxy.sendWithReplacer(sender.getUniqueId(),"invite_self_refused", invitesender.getName());
        return true;
    }
    public boolean help(Player sender){
        return proxy.sendMessage(sender.getUniqueId(), "help");
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
        }};
        Map<String, Boolean> teamArgmap = new HashMap<>(){{
            put("givelead", true);
            put("kick", true);
            put("slotplayer", true);
        }};
        Map<String, Boolean> slotArgmap = new HashMap<>(){{
            put("slotplayer", true);
        }};
        partycommandRegister.addSubPermissions();
        CommandAPICommand command = partycommandRegister.getCommand();
        submap.forEach((name, executor)-> command.withSubcommand(partycommandRegister.addSubcommand(name, executor, playerArgmap.getOrDefault(name, false), slotArgmap.getOrDefault(name, false), teamArgmap.getOrDefault(name, false))));
        partycommandRegister.registerCommand();
    }
    private Party parsesubTag(String subtag, OfflinePlayer sender){
        if(subtag.equals("trugger_party")) return partyMap.get(sender.getUniqueId());
        else if(subtag.matches("id_.*")) {
            int partyId = Integer.parseInt(subtag.substring(3));
            if(partyId > Party.getPartyCount()-1) proxy.sendMessage(sender.getUniqueId(), "wrong_subtag");
            return Party.getPartyByID(partyId);
        } else if (subtag.matches("player_.*")) {
            Player p = Bukkit.getPlayer(subtag.substring(7));
            if(p == null) {proxy.sendMessage(sender.getUniqueId(), "wrong_subtag"); throw new NullPointerException();}
            return partyMap.get(p.getUniqueId());
        } else throw new NullPointerException();
    }
    private List<OfflinePlayer> parseSlots(String raw, OfflinePlayer p){
        List<OfflinePlayer> retval = new ArrayList<>();
        Party party = partyMap.get(p.getUniqueId());
        if(raw.equals("all")) {
            retval = party.getPlayers();
        }else{
            String[] slots = (raw).split("/");
            for (String s : slots) {
                int id;
                try {
                 id = Integer.parseInt(s);
                } catch (Exception e){
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }
                OfflinePlayer p1 = party.getPlayer(id);
                if(p1 == null) {
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }
                retval.add(p1);
            }
        }
        return retval;
    }
    private List<OfflinePlayer> parseSlots(String raw, OfflinePlayer p, Party party){
        List<OfflinePlayer> retval = new ArrayList<>();

        if(raw.equals("all")) {
            retval = party.getPlayers();
        }else{
            String[] slots = (raw).split("/");
            for (String s : slots) {
                int id;
                try {
                    id = Integer.parseInt(s);
                } catch (Exception e){
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }
                OfflinePlayer p1 = party.getPlayer(id);
                if(p1 == null) {
                    proxy.sendMessage(p.getUniqueId(), "wrong_slots");
                    return null;
                }
                retval.add(p1);
            }
        }
        return retval;
    }
    private void commandParty_a(){
        CommandAPICommand command = partyadmincommandRegister.getCommand();
        CommandAPICommand teleport = new CommandAPICommand("teleport") {{
            withPermission("MysticParties.party_a.teleport");
            withArguments(new TextArgument("subtag").includeSuggestions(subTagSuggestor));
            withArguments(new TextArgument("slots").includeSuggestions(slotSuggestor));
            withArguments(new LocationArgument("location", LocationType.PRECISE_POSITION));
            withArguments(new RotationArgument("rotation"));
            withArguments(new WorldArgument("world"));
            executes((sender, args) -> {CommandWrapper2((Player) sender, args);});
        }};
        if(config.isProxy()){
            teleport.withArguments(new StringArgument("server").replaceSuggestions(ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> proxy.getServerList().toArray(new String[0])))));
        }
        CommandAPICommand execute = new CommandAPICommand("command"){{
            withPermission("MysticParties.party_a.command");
            withArguments(new TextArgument("subtag").includeSuggestions(subTagSuggestor));
            withArguments(new TextArgument("slots").includeSuggestions(slotSuggestor));
            withArguments(new GreedyStringArgument("command"));
            executes((sender, args) -> {CommandWrapper((OfflinePlayer) sender, args);});
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
    private void CommandWrapper(OfflinePlayer sender, CommandArguments args){
        String slot = (String) args.get("slots");
        assert slot != null;
        List<OfflinePlayer> executeList = parseSlots(slot, sender);
        if(executeList == null){
            proxy.sendMessage(sender.getUniqueId(), "wrong_subtag");
            return;
        }
        String command = (String) args.get("command");
        if(command == null){
            proxy.sendMessage(sender.getUniqueId(), "command_wrong_args");
            return;
        }
        List<Boolean> buffer = new ArrayList<>();
        for (OfflinePlayer player : executeList) {
            Player p = Bukkit.getPlayer(player.getUniqueId());

            if(p != null) buffer = Collections.singletonList(proxy.executeAs(player.getUniqueId(), command));
        }
        if(buffer.contains(false)){
            proxy.sendMessage(sender.getUniqueId(), "command_fail");
        }
    }
    public void CommandWrapper2(OfflinePlayer sender, CommandArguments args){
        Party party;
        List<OfflinePlayer> teleportList;
        String subtag = (String) args.get("subtag");
        assert subtag != null;
        try{
            party = parsesubTag(subtag, sender);
        }catch(Exception e){
            proxy.sendMessage(sender.getUniqueId(), "wrong_subtag");
            return;
        }
        String slot = (String) args.get("slots");
        assert slot != null;
        teleportList = parseSlots(slot, sender, party);
        if(teleportList == null){
            proxy.sendMessage(sender.getUniqueId(), "wrong_slots");
            return;
        }
        Location tploc = (Location) args.get("location");
        Rotation rot = (Rotation) args.get("rotation");
        World world = (World) args.get("world");
        String server = (String) args.get("server");
        assert tploc != null;
        assert rot != null;
        tploc.setWorld(world);
        tploc.setPitch(rot.getPitch());
        tploc.setYaw(rot.getYaw());
        if(server == null) server = "local";
        proxy.teleportTo(sender, server, tploc);
    }
    public Integer getPartyID(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getId();
    }
    public String getLeaderName(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getLeader().getName();
    }
    public String getPlayerName(OfflinePlayer p, Integer i){
        OfflinePlayer arg = partyMap.get(p.getUniqueId()).getPlayer(i);
        if(arg == null) return "";
        return arg.getName();
    }
    public Integer getPartySize(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getPlayerCount();
    }
    public Integer getFreeSlots(OfflinePlayer p){
        Party party = partyMap.get(p.getUniqueId());
        return party.getLimit() - party.getPlayers().toArray().length;
    }
    public Integer getPlayerID(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getPlayerID(p.getUniqueId());
    }
    public Integer getPartyLimit(OfflinePlayer p){
        return partyMap.get(p.getUniqueId()).getLimit();
    }
    public Boolean isSlotBusy(OfflinePlayer p, Integer i){
        return partyMap.get(p.getUniqueId()).getPlayer(i) != null;
    }
    public Boolean isPlayerLeader(OfflinePlayer p){
        return leaderMap.get(p.getUniqueId()) != null;
    }
    public Boolean isPlayerParticipant(OfflinePlayer p){
        if(leaderMap.get(p.getUniqueId()) != null) return false;
        return partyMap.get(p.getUniqueId()) != null;
    }
    public Player getmember(OfflinePlayer p, Integer i){
        OfflinePlayer p1 = partyMap.get(p.getUniqueId()).getPlayer(i);
        return Bukkit.getPlayer(p1.getUniqueId());
    }
}

package ca.innov8solutions.tntrun.game;

import ca.innov8solutions.tntrun.GameConfig;
import ca.innov8solutions.tntrun.TnTRun;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Game {


    private Random ran = new Random();
    private int time = 60;
    private GameConfig config;
    private List<GameMap> availableMaps;
    private GameMap currentMap;
    private Map<UUID, PlayerState> players;
    private Map<UUID, Long> timeAlive;

    private List<BlockState> blocks = new ArrayList<>();

    private GameState state;

    private BukkitTask timer;

    public Game(TnTRun plugin, GameConfig config) {
        this.config = config;
        this.availableMaps = config.getAvailableMaps();
        this.players = new HashMap<>();
        this.timeAlive = new HashMap<>();
        System.out.println(config.getAvailableMaps() == null);

        if (availableMaps.size() > 0) {
            this.currentMap = availableMaps.get(0);
        }

        if (currentMap != null) {
            state = GameState.LOBBY;
            timer = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 0L, 20L);
        }
        else {
            state = GameState.DISABLED;
        }
    }

    public void sendActionbar(Player player, String message) {
        try {
            Constructor<?> constructor = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"), getNMSClass("ChatMessageType"));

            Object icbc = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + message + "\"}");
            Object messageType = getNMSClass("ChatMessageType").getMethod("a", byte.class).invoke(null, (byte)2);
            Object packet = constructor.newInstance(icbc, messageType);
            Object entityPlayer= player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    private Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    private void tick() {
        if (state == GameState.DISABLED) {
            return;
        }

        if (state == GameState.LOBBY) {
            if (getTime() % 20 == 0 && getTime() > 0) {
                sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Game starting in: " + ChatColor.GRAY + getTime());
            }
            else if (getTime() <= 5 && getTime() > 0) {
                sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "Game starting in: " + getTime());
                config.getLobby().getWorld().playSound(config.getLobby(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            }
            else if (getTime() == 0) {
                //Can we start?
                if (getAlive().size() < 2) {
                    sendMessage(ChatColor.RED + "There are not enough players to begin the game!");
                    setTime(60);
                    return;
                }
                start();
            }
        }
        else if (state == GameState.IN_GAME) {
            if (getTime() % 30 == 0) {
                sendMessage(ChatColor.RED + "Game ending in: " + formatTime(getTime()));
            }

            if (getTime() % 30 == 0) {
                for (UUID uuid : getAlive()) {
                    int index = ran.nextInt(PotionEffectType.values().length - 1);
                    PotionEffectType type = PotionEffectType.values()[index];
                    Bukkit.getPlayer(uuid).addPotionEffect(new PotionEffect(type, 10, 1));
                }
            }

            if (getTime() == 0) {
                stop(false);

            }
        }
        else if (state == GameState.POST_GAME) {
            if (getTime() <= 0) {
                List<UUID> rem = new ArrayList<>(players.keySet());
                for (UUID uuid : rem) {
                    Player pl = Bukkit.getPlayer(uuid);
                    leave(pl);
                }
                rem.clear();
                timeAlive.clear();
                players.clear();
                setTime(60);
                this.state = GameState.LOBBY;
            }
        }
        setTime(getTime() - 1);
    }

    public void start() {
        this.state = GameState.IN_GAME;
        long milis = System.currentTimeMillis();
        setTime(5*60);
        for (UUID uuid : getAlive()) {
            timeAlive.put(uuid, milis);
            Player pl = Bukkit.getPlayer(uuid);
            pl.teleport(getRandomSpawn());
        }
    }

    public String formatTime(int time) {
        long minutes = (time%3600 - time%3600%60)/60;
        long seconds = time%3600%60;

        return String.format("%dm:%ds", minutes, seconds);
    }

    public Location getRandomSpawn() {
        Random ran = new Random();
        int index = ran.nextInt(currentMap.getSpawns().size() - 1);

        return currentMap.getSpawns().get(index);
    }

    public void join(Player pl) {
        if (players.containsKey(pl.getUniqueId())) {
            pl.sendMessage(ChatColor.RED + "You are already in a game!");
            return;
        }

        if (state == GameState.DISABLED) {
            pl.sendMessage(ChatColor.RED + "Game is currently disabled!");
            return;
        }

        if (state == GameState.IN_GAME) {
            addSpectator(pl);
            return;
        }
        players.put(pl.getUniqueId(), PlayerState.ALIVE);
        pl.setHealth(pl.getMaxHealth());
        pl.setGameMode(GameMode.SURVIVAL);
        pl.teleport(config.getLobby());
        sendMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "> " + ChatColor.GRAY + pl.getName());
    }

    public void addSpectator(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        Location spawn = getCurrentMap().getSpawns().get(0);
        player.teleport(spawn);
        player.setFireTicks(0);
        this.players.put(player.getUniqueId(), PlayerState.SPECTATING);
        sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "> " + ChatColor.GRAY + player.getName());
    }
    public void death(Player player) {
        long start = this.timeAlive.get(player.getUniqueId());
        long current = System.currentTimeMillis();

        long elapsed = current - start;
        this.timeAlive.put(player.getUniqueId(), elapsed);
        addSpectator(player);

        if (shouldStop()) {
            stop(false);
        }
    }

    private List<UUID> getAlive() {
        List<UUID> alive = new ArrayList<>();
        for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
            if (entry.getValue() == PlayerState.ALIVE) {
                alive.add(entry.getKey());
            }
        }
        return alive;
    }

    public void sendMessage(String msg) {
        for (UUID uuid : players.keySet()) {
            Player pl = Bukkit.getPlayer(uuid);
            if (pl == null)
                continue;
            pl.sendMessage(msg);
        }
    }

    public PlayerState getPlayerState(Player player) {
        return players.get(player.getUniqueId());
    }

    public void addMap(GameMap map) {
        this.availableMaps.add(map);
    }

    public GameState getState() {
        return state;
    }

    public void leave(Player pl) {
        if (!isInGame(pl)) {
            pl.sendMessage(ChatColor.RED + "You are not in a game!");
            return;
        }
        System.out.println(pl.getName());
        players.remove(pl.getUniqueId());
        pl.setGameMode(GameMode.SURVIVAL);
        pl.teleport(config.getLobby());
        pl.setFlying(false);
        pl.setAllowFlight(false);
    }

    public boolean shouldStop() {
        return getAlive().size() == 1;
    }

    public void stop(boolean forced) {
        if (forced) {
            List<UUID> temp = new ArrayList<>(players.keySet());
            for (UUID uuid : temp) {
                leave(Bukkit.getPlayer(uuid));
            }
            state = GameState.LOBBY;
            setTime(60);
            return;
        }
        setTime(10);
        state = GameState.POST_GAME;

        int place = 1;
        sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Top Runners:");
        for (UUID uuid : getTop3()) {
            Player pl = Bukkit.getPlayer(uuid);
            sendMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "" + place + ": " + pl.getName());
            sendActionbar(pl, ChatColor.GOLD + ChatColor.BOLD.toString() + "You placed: " + place + getEnding(place));
            place++;
        }
        restoreMap();
    }

    private String getEnding(int place) {
        if (place == 1) {
            return "st";
        }
        else if (place == 2) {
            return"nd";
        }
        else {
            return "rd";
        }
    }

    public List<UUID> getTop3() {
        List<UUID> uuid = new ArrayList<>();
        Object[] a = timeAlive.entrySet().toArray();
        Arrays.sort(a, (Comparator) (o1, o2) -> ((Map.Entry<UUID, Long>) o2).getValue().compareTo(
                ((Map.Entry<UUID, Long>) o1).getValue()));

        for (int i = 0; i < a.length; i++) {
            uuid.add(((Map.Entry<UUID, Long>)a[i]).getKey());
            if (uuid.size() >= 3) {
                break;
            }
        }
        return uuid;
    }

    public GameMap getMap(String name) {
        for (GameMap map : availableMaps) {
            if (map.getName().equalsIgnoreCase(name)) {
                return map;
            }
        }
        return null;
    }

    public boolean isInGame(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    public void addBlock(Block b) {
        this.blocks.add(b.getState());
    }

    public void restoreMap() {
        for (BlockState state : blocks) {
            state.update(true);
        }
    }

    public GameMap getCurrentMap() {
        return currentMap;
    }
}

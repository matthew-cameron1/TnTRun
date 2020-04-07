package ca.innov8solutions.tntrun.command;

import ca.innov8solutions.tntrun.TnTRun;
import ca.innov8solutions.tntrun.game.GameMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GameCommand implements CommandExecutor {

    private TnTRun plugin;

    public GameCommand(TnTRun plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("game")) {
            return true;
        }

        if (args.length == 0) {
            //TODO send help command
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Player only command!");
            return true;
        }

        Player player = (Player) sender;

        if (args[0].equalsIgnoreCase("add-spawn")) {
            // /game add-spawn <map>
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "/game add-spawn <map>");
                return true;
            }
            String mapName = args[1];
            GameMap map = plugin.getGame().getMap(mapName);

            if (map == null) {
                player.sendMessage(ChatColor.RED + "Cannot find map with name: " + mapName);
                return true;
            }

            Location pLoc = player.getLocation();
            map.addSpawn(pLoc);
            plugin.saveGameConfig();

            player.sendMessage(ChatColor.GREEN + "Added spawn to map " + mapName);
        }
        else if (args[0].equalsIgnoreCase("add-map")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "/game add-map <map>");
                return true;
            }
            String mapName = args[1];
            if (plugin.getGame().getMap(mapName) != null) {
                player.sendMessage(ChatColor.RED + "Map by that name already exists!");
                return true;
            }

            GameMap map = new GameMap(mapName, "", new ArrayList<>());
            plugin.getGameConfig().addMap(map);
            player.sendMessage(ChatColor.GREEN + "Created an empty map: " + mapName + "! Please set it up now!");
            plugin.saveGameConfig();
            return true;
        }
        else if (args[0].equalsIgnoreCase("setlobby")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "/game setlobby");
                return true;
            }
            plugin.getGameConfig().setLobby(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Set lobby to your current location!");
            plugin.saveGameConfig();
        }
        else if (args[0].equalsIgnoreCase("start")) {
            plugin.getGame().start();
        }
        else if (args[0].equalsIgnoreCase("stop")) {
            plugin.getGame().stop(true);
        }
        else if (args[0].equalsIgnoreCase("join")) {
            plugin.getGame().join(player);
        }
        else if (args[0].equalsIgnoreCase("leave")) {
            plugin.getGame().leave(player);
        }
        return false;
    }
}

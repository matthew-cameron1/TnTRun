package ca.innov8solutions.tntrun.listener;

import ca.innov8solutions.tntrun.TnTRun;
import ca.innov8solutions.tntrun.game.GameState;
import ca.innov8solutions.tntrun.game.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TNT;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private TnTRun plugin;

    public GameListener(TnTRun plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player pl = event.getPlayer();
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player pl = event.getPlayer();
        plugin.getGame().leave(pl);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player pl = event.getPlayer();
        if (!plugin.getGame().isInGame(pl)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if ((event.getFrom().getBlockX() == event.getTo().getBlockX()) && (event.getFrom().getBlockZ() == event.getTo().getBlockZ()) && (event.getFrom().getBlockY() == event.getTo().getBlockY())) {
            return;
        }

        if (plugin.getGame().getState() != GameState.IN_GAME) {
            return;
        }

        Block b = from.getBlock().getRelative(BlockFace.DOWN);

        if (b.getType() == Material.AIR) {
            return;
        }

        plugin.getGame().addBlock(b);
        b.setType(Material.AIR);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        if (!plugin.getGame().isInGame(player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }

        PlayerState pState = plugin.getGame().getPlayerState(player);
        if (pState != PlayerState.ALIVE) {
            event.setCancelled(true);
            event.setDamage(0);
            return;
        }

        event.setCancelled(true);
        event.setDamage(0);
        plugin.getGame().death(player);
    }
}

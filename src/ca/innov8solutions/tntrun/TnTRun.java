package ca.innov8solutions.tntrun;

import ca.innov8solutions.tntrun.command.GameCommand;
import ca.innov8solutions.tntrun.game.Game;
import ca.innov8solutions.tntrun.listener.GameListener;
import lombok.Data;
import org.bukkit.plugin.java.JavaPlugin;

public class TnTRun extends JavaPlugin {

    private ConfigManager<GameConfig> cfgManager = new ConfigManager<>("config.json", GameConfig.class);
    private GameConfig gameConfig;
    private Game game;

    @Override
    public void onEnable() {
        cfgManager.init();
        cfgManager.loadConfig();
        this.gameConfig = cfgManager.getConfig();
        this.game = new Game(this, this.gameConfig);

        getCommand("game").setExecutor(new GameCommand(this));
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
    }

    @Override
    public void onDisable() {
       saveGameConfig();
    }

    public void saveGameConfig() {
        cfgManager.saveConfig();
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public Game getGame() {
        return game;
    }
}

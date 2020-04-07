package ca.innov8solutions.tntrun;

import ca.innov8solutions.tntrun.game.GameMap;
import lombok.Data;
import org.bukkit.Location;

import java.util.List;

@Data
public class GameConfig {
    private Location lobby;
    private final List<GameMap> availableMaps;

    public void addMap(GameMap map) {
        this.availableMaps.add(map);
    }
}

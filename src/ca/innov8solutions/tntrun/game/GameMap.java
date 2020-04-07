package ca.innov8solutions.tntrun.game;

import lombok.Data;
import org.bukkit.Location;

import java.util.List;

@Data
public class GameMap {

    private final String name;
    private final String author;
    private final List<Location> spawns;

    public GameMap(String name, String author, List<Location> spawns) {
        this.name = name;
        this.author = author;
        this.spawns = spawns;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public void addSpawn(Location location) {
        this.spawns.add(location);
    }
}

package net.mcatlas.environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Handles elements of the MCAtlas world itself.
 */
public class EnvironmentPlugin extends JavaPlugin {

    private static EnvironmentPlugin plugin;

    private String apiKey;
    private double scaling;
    private boolean apiOffline = false;

    private Queue<Player> playerQueue;

    public static EnvironmentPlugin getEnvironmentPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        saveDefaultConfig();
        this.apiKey = getConfig().getString("apiKey", null);
        this.scaling = getConfig().getDouble("scaling");

        playerQueue = new LinkedList<>();

        boolean enableWeather = getConfig().getBoolean("enableWeather", true);
        if (enableWeather && apiKey != null) {
            getCommand("weatherhere").setExecutor(new TestingCommand());
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                handleWeatherCycle();
            }, 0L, 60L); // pull a new player to set weather every 3 seconds
            // speed of setting weather individually depends on how many people are on
        }
    }

    @Override
    public void onDisable() {
        plugin = null;
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new VoidChunkGenerator();
    }

    public String getAPIKey() {
        return apiKey;
    }


    // Handles all weather
    public void handleWeatherCycle() {
        Collection<? extends Player> players = Bukkit.getWorlds().get(0).getPlayers();
        if (players.size() == 0) return;

        if (playerQueue.size() == 0) refreshQueueWithPlayers(players);

        Player player = playerQueue.poll();

        // async!!!
        CompletableFuture.runAsync(() -> {
            setWeather(player);
        });
    }

    // RUN ASYNC or lag
    public void setWeather(Player player) {
        Condition condition = getCondition(player.getLocation());

        switch (condition) {
            case CLEAR: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.CLEAR);
                });
                return;
            }
            case STORM: // maybe one day add thunderstorm-per-user implementation? idk
            case RAIN: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });
                return;
            }
        }
    }

    public void refreshQueueWithPlayers(Collection<? extends Player> players) {
        playerQueue.clear();
        for (Player player : players) {
            playerQueue.add(player);
        }
    }

    public Condition getCondition(Location location) {
        String weatherDesc = getDetailedWeatherDescription(location);

        switch (weatherDesc) {
            case "Clear":
            case "Clouds":
            case "Haze":
            case "Fog":
            case "Mist":
            case "Smoke":
            case "Dust":
            case "Sand":
            case "Ash":
                return Condition.CLEAR;
            case "Drizzle":
            case "Snow":
            case "Rain":
                return Condition.RAIN;
            case "Squall":
            case "Thunderstorm":
            case "Tornado":
                return Condition.STORM;
            default:
                return Condition.CLEAR;
        }
    }

    public String getDetailedWeatherDescription(Location location) {
        String noWeather = "No weather. Maybe the weather satellite broke?";

        Coordinate coord = EnvironmentPlugin.getEnvironmentPlugin()
                .getLifeFromMC(location.getBlockX(), location.getBlockZ());

        String urlString = "http://api.openweathermap.org/data/2.5/weather?lat="
                + coord.y + "&lon=" + coord.x + "&appid="
                + EnvironmentPlugin.getEnvironmentPlugin().getAPIKey();
        URL url = null;
        HttpURLConnection connection = null;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
        } catch (Exception e) { // any issue with the connection, like an error code
            int code = 0;
            try {
                code = connection.getResponseCode();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (code == 500 && apiOffline) { // api is known to be offline
                return noWeather;
            } else if (code == 500 && !apiOffline) { // api is not known to be offline
                Bukkit.getLogger().warning("OpenWeatherMap went offline! No weather for now.");
                apiOffline = true;
                return noWeather;
            } else if (code == 400) { // bad request (happens occasionally)
                return noWeather;
            } else { // anything else
                e.printStackTrace();
                return noWeather;
            }
        }

        JsonParser jp = new JsonParser();
        JsonElement root;
        try {
            root = jp.parse(new InputStreamReader((InputStream) connection.getContent()));
        } catch (Exception e) {
            e.printStackTrace();
            return noWeather;
        }

        JsonObject rootobj = root.getAsJsonObject();
        JsonArray weather = rootobj.getAsJsonArray("weather");
        JsonObject main = weather.get(0).getAsJsonObject();
        String weatherDesc = main.get("main").getAsString();

        if (apiOffline) {
            Bukkit.getLogger().info("OpenWeatherMap is back online.");
            apiOffline = false;
        }

        return weatherDesc;
    }

    // returns real life coordinate!!
    public Coordinate getLifeFromMC(int mcX, int mcY) {
        int x = (int) (mcX / scaling);
        int y = (int) (mcY / scaling) * -1;
        return new Coordinate(x, y);
    }

    public static boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    public static boolean isNether(World world) {
        return world.getEnvironment() == World.Environment.NETHER;
    }

    public static boolean isEnd(World world) {
        return world.getEnvironment() == World.Environment.THE_END;
    }

    public class Coordinate {
        int x; // long
        int y; // lat

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public enum Condition {
        CLEAR,
        RAIN,
        STORM;
    }

}

package net.mcatlas.environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles elements of the MCAtlas world itself.
 */
public class EnvironmentPlugin extends JavaPlugin {

    private static EnvironmentPlugin plugin;

    private String apiKey;
    private double scaling;
    private boolean apiOffline = false;

    private Set<WeatherPlayer> allWeatherPlayers;
    private PriorityQueue<WeatherPlayer> playerQueue;

    private Map<UUID, Location> currentWeatherLocations;

    public static EnvironmentPlugin get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        saveDefaultConfig();
        this.apiKey = getConfig().getString("apiKey", null);
        this.scaling = getConfig().getDouble("scaling");

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            setupNetherBees();
        }, 200, 200);

        boolean enableWeather = getConfig().getBoolean("enableWeather", true);
        if (enableWeather && apiKey != null) {
            playerQueue = new PriorityQueue<>();
            currentWeatherLocations = new HashMap<UUID, Location>();
            allWeatherPlayers = new HashSet<>();

            getCommand("weatherhere").setExecutor(new TestingCommand());
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                handleWeatherCycle();
            }, 20L, 60L); // pull a new player to set weather every 3 seconds
            // speed of setting weather individually depends on how many people are on

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                checkPlayerLocations();
            }, 20 * 30L, 20 * 20L);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                sendPlayerWeatherMessages();
            }, 20 * 20L, 20 * 30L);
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

    public void setupNetherBees() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
                continue;
            }

            for (Entity entity : player.getNearbyEntities(60, 60, 60)) {
                if (entity.getType() != EntityType.BEE) {
                    continue;
                }

                Bee bee = (Bee) entity;
                bee.setTarget(player);
                bee.setAnger(999999);
                bee.setHasStung(false);
            }
        }
    }

    public void checkPlayerLocations() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (System.currentTimeMillis() - player.getLastLogin() > Duration.ofSeconds(60).toMillis()) {
                    WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());
                    if (weatherPlayer == null) {
                        addPlayerToQueue(player, WeatherPriority.JOIN);
                        continue;
                    }
                    Location prevLocation = weatherPlayer.getLastLocation();
                    if (prevLocation == null) {
                        addPlayerToQueue(player, WeatherPriority.JOIN);
                        continue;
                    }
                    Location currentLocation = player.getLocation();
                    currentLocation.setY(64);
                    if (prevLocation.distance(currentLocation) > 250) {
                        addPlayerToQueue(player, WeatherPriority.MOVE);
                    }
                }
            }
        }
    }

    public void sendPlayerWeatherMessages() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            for (Player player : world.getPlayers()) {
                WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());
                if (weatherPlayer == null) continue;

                WeatherData weatherData = weatherPlayer.getWeatherData();
                if (weatherData == null) continue;

                int fahrenheit = (int) EnvironmentUtil.kelvinToFahrenheit(weatherData.temperature);
                int celsius = (int) EnvironmentUtil.kelvinToCelsius(weatherData.temperature);
                Color color = EnvironmentUtil.getColorFromTemperature(fahrenheit);
                TextColor tempColor = TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
                String temperature = tempColor + "" + fahrenheit + "F" + ChatColor.GRAY + "/" + tempColor + "" + celsius + "C";
                player.sendActionBar(Component.text(fahrenheit + "F").color(tempColor)
                        .append(Component.text("/").color(NamedTextColor.GRAY)
                        .append(Component.text(celsius + "C").color(tempColor)
                        .append(Component.text(" - " + (int) weatherData.windSpeed + "mph Wind" + " - " + weatherData.weatherFullDesc)
                                .color(NamedTextColor.GRAY)
                        ))));
            }
        }
    }

    // Gets the new player to set weather for and handles it for them until they get their weather updated again
    public void handleWeatherCycle() {
        WeatherPlayer weatherPlayer = null;
        Player player = null;
        while (true) {
            weatherPlayer = playerQueue.poll();
            if (weatherPlayer == null || weatherPlayer.getUUID() == null || Bukkit.getPlayer(weatherPlayer.getUUID()) == null) {
                if (playerQueue.isEmpty()) return;
                continue;
            }
            player = Bukkit.getPlayer(weatherPlayer.getUUID());
            if (player == null) continue;
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;

            break;
        }

        weatherPlayer.update();

        addPlayerToQueue(player, WeatherPriority.ONLINE);

        Player thePlayer = player;

        // async!!!
        CompletableFuture.runAsync(() -> {
            setWeatherStatus(thePlayer);
        });
    }

    // RUN ASYNC or lag
    // actually does the work of setting weather characteristics for the player
    public void setWeatherStatus(Player player) {
        Location location = player.getLocation();
        WeatherData weatherData = getWeatherInformation(location);
        WeatherPlayer weatherPlayer = getWeatherPlayer(player.getUniqueId());

        weatherPlayer.setWeatherData(weatherData);

        Condition condition = Condition.CLEAR;
        if (weatherData != null) {
            condition = getCondition(weatherData.weatherDesc);
        }

        switch (condition) {
            case CLEAR: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.CLEAR);
                });
                break;
            }
            case STORM: // maybe one day add thunderstorm-per-user implementation? idk
            case RAIN: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });
                break;
            }
        }

        final long lastUpdated = weatherPlayer.getLastUpdated();

        if (weatherData.windSpeed > 10) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (!player.isOnline()) {
                    return;
                }

                if (lastUpdated != weatherPlayer.getLastUpdated()) {
                    return;
                }

                player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.AMBIENT, (float) (0.1F + ((weatherData.windSpeed - 10) / 50)), (float) 0.8);
            }, 0L, 8 * 20L);
        }

    }

    public void addPlayerToQueue(Player player, WeatherPriority priority) {
        WeatherPlayer weatherPlayer = null;
        for (WeatherPlayer otherPlayer : allWeatherPlayers) {
            if (otherPlayer.getUUID().equals(player.getUniqueId())) {
                weatherPlayer = otherPlayer;
                weatherPlayer.setScore(priority.score);
                break;
            }
        }

        if (weatherPlayer == null) {
            weatherPlayer = new WeatherPlayer(player.getUniqueId(), priority.score, player.getLocation());
            allWeatherPlayers.add(weatherPlayer);
        }

        Location location = player.getLocation();
        location.setY(64);
        weatherPlayer.setLastLocation(location);

        playerQueue.remove(weatherPlayer);
        playerQueue.add(weatherPlayer);
    }

    @NotNull
    public Condition getCondition(String weatherDesc) {
        switch (weatherDesc) {
            case "Drizzle":
            case "Snow":
            case "Rain":
                return Condition.RAIN;
            case "Squall":
            case "Thunderstorm":
            case "Tornado":
                return Condition.STORM;
            case "Clear":
            case "Clouds":
            case "Haze":
            case "Fog":
            case "Mist":
            case "Smoke":
            case "Dust":
            case "Sand":
            case "Ash":
            default:
                return Condition.CLEAR;
        }
    }

    @Nullable
    public WeatherPlayer getWeatherPlayer(UUID uuid) {
        for (WeatherPlayer weatherPlayer : this.allWeatherPlayers) {
            if (weatherPlayer.getUUID().equals(uuid)) {
                return weatherPlayer;
            }
        }
        return null;
    }

    @Nullable
    public WeatherData getWeatherInformation(Location location) {
        // used for weather obj
        int x = location.getBlockX();
        int z = location.getBlockZ();

        JsonElement root = getDataFromAPI(location);

        if (root == null || root.isJsonNull()) {
            return null;
        }


        JsonObject rootobj = root.getAsJsonObject();

        JsonArray weatherArray = rootobj.getAsJsonArray("weather");
        JsonObject weather = weatherArray.get(0).getAsJsonObject();
        // description used for deciding weather conditions ingame
        String weatherDesc = weather.get("main").getAsString();
        String weatherFullDesc = "";
        try { // full desc sometimes not listed
            weatherFullDesc = weather.get("description").getAsString();
        } catch (Exception e) { }
        weatherFullDesc = WordUtils.capitalizeFully(weatherFullDesc);

        JsonObject mainObject = rootobj.getAsJsonObject("main");
        double temp = mainObject.get("temp").getAsDouble();
        double pressure = mainObject.get("pressure").getAsDouble();
        double humidity = mainObject.get("humidity").getAsDouble();

        JsonObject windObject = rootobj.getAsJsonObject("wind");
        double windSpeed = windObject.get("speed").getAsDouble();
        double windDirection = windObject.get("deg").getAsDouble();
        double windGust = 0;
        try { // wind gust occasionally not listed
            windGust = windObject.get("gust").getAsDouble();
        } catch (Exception e) { }

        JsonObject cloudObject = rootobj.getAsJsonObject("clouds");
        double cloudy = cloudObject.get("all").getAsDouble();

        double visibility = rootobj.get("visibility").getAsDouble();

        String name = rootobj.get("name").getAsString();

        WeatherData data = new WeatherData(x, z, weatherDesc, weatherFullDesc, temp, pressure, humidity, windSpeed,
                windDirection, windGust, cloudy, visibility, name);

        if (apiOffline) {
            Bukkit.getLogger().info("OpenWeatherMap is back online.");
            apiOffline = false;
        }

        return data;
    }

    @Nullable
    public JsonElement getDataFromAPI(Location location) {
        Coordinate coord = EnvironmentPlugin.get()
                .getLifeFromMC(location.getBlockX(), location.getBlockZ());

        String urlString = "http://api.openweathermap.org/data/2.5/weather?lat="
                + coord.y + "&lon=" + coord.x + "&appid="
                + EnvironmentPlugin.get().getAPIKey();
        System.out.println(urlString);
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
                return null;
            } else if (code == 500 && !apiOffline) { // api is not known to be offline
                Bukkit.getLogger().warning("OpenWeatherMap went offline! No weather for now.");
                apiOffline = true;
                return null;
            } else if (code == 400) { // bad request (happens occasionally)
                return null;
            } else { // anything else
                e.printStackTrace();
                return null;
            }
        }

        JsonParser jp = new JsonParser();
        JsonElement root;
        try {
            root = jp.parse(new InputStreamReader((InputStream) connection.getContent()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return root;
    }

    // returns real life coordinate!
    public Coordinate getLifeFromMC(int mcX, int mcY) {
        double x = (mcX / scaling);
        double y = (mcY / scaling) * -1;
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
        double x; // long
        double y; // lat

        public Coordinate(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public enum Condition {
        CLEAR,
        RAIN,
        STORM;
    }

    public enum WeatherPriority {
        JOIN(0),
        MOVE(1),
        ONLINE(2);

        private int score;

        WeatherPriority(int score) {
            this.score = score;
        }
    }

}

package net.mcatlas.environment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.mcatlas.environment.EnvironmentUtil.*;

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

    private Set<Tornado> tornadoes;

    private BossBar tornadoBossBar;

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
            tornadoes = new HashSet<Tornado>();

            for (Player player : getServer().getOnlinePlayers()) {
                addPlayerToQueue(player, WeatherPriority.ONLINE);
            }

            getCommand("weatherhere").setExecutor(new WeatherHereCommand());
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

            tornadoBossBar = Bukkit.createBossBar(ChatColor.WHITE + "" + ChatColor.BOLD + "Tornado Warning", BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                tornadoBossBar.setTitle(ChatColor.WHITE + "" + ChatColor.BOLD + "Tornado Warning");
            }, 0L, 40L);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                tornadoBossBar.setTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Tornado Warning");
            }, 20L, 40L);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                launchEntitiesInTornado();
            }, 10 * 20L, 20L);
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

    public void launchEntitiesInTornado() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                removeFromBossBar(player);
                continue;
            }

            Location location = player.getLocation();
            int x = location.getBlockX();
            int z = location.getBlockZ();
            if (x > 0 || z > 0) {
                removeFromBossBar(player);
                continue; // southern hemisphere
            }
            // now only players in north and west hemisphere. USA general area
            int y = location.getWorld().getHighestBlockYAt(location);
            int playerY = location.getBlockY();

            boolean inBossBarZone = false;

            for (Tornado tornado : tornadoes) {
                Location tornadoLoc = tornado.getLocation();

                tornadoLoc.setY(y);
                location.setY(y);

                double dist = location.distance(tornadoLoc);
                if (dist < 0.1) dist = .1; // if its 0 or near 0 it will be an issue for the vector
                if (dist < 5 && playerY - y < 20 && playerY - y > -5) { // 5 blocks from middle of tornado; 20 blocks above or 5 blocks below bottom of tornado
                    inBossBarZone = true;

                    // send them flying
                    org.bukkit.util.Vector dir = location.getDirection().multiply(2);
                    org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 20 + (1 * (5 / dist)), dir.getZ());
                    player.setVelocity(vector);

                    for (int i = 1; i < RANDOM.nextInt(20); i++) {
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            player.setVelocity(vector.rotateAroundY(Math.PI / 6).multiply(1.1));
                        }, i * 2L);
                    }

                    if (!tornado.playerHasAlreadyReceivedNamedItem(player.getUniqueId())) {
                        nameItemInHand(player, tornado);
                    }

                    // elytra randomly falls off entering tornado
                    if (chance(5)) {
                        ItemStack chestplate = player.getInventory().getChestplate();
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            player.getInventory().setChestplate(null);
                            player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds tore your elytra into pieces!");
                        }
                    }
                    // random parts of inventory get blown away when entering tornado
                    if (chance(25)) {
                        int invSize = player.getInventory().getSize();
                        boolean someBlownAway = false;
                        for (int i = 0; i < RANDOM.nextInt(5); i++) {
                            int slot = RANDOM.nextInt(invSize);
                            ItemStack stack = player.getInventory().getItem(slot);
                            player.getInventory().setItem(slot, null);
                            if (stack != null) {
                                someBlownAway = true;
                                Bukkit.getScheduler().runTaskLater(this, () -> {
                                    Item item = player.getWorld().dropItem(player.getLocation(), stack);
                                    item.setVelocity(player.getVelocity());
                                }, 10L * RANDOM.nextInt(4));
                            }
                        }
                        if (someBlownAway) {
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                player.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "The winds blew away some of your inventory!");
                            }, 20L * 2);
                        }
                    }
                } else if (dist < 30) { // if less than 30 blocks from tornado, move around entities that are near the tornado
                    inBossBarZone = true;
                    for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
                        Location entityLocation = entity.getLocation();
                        double entDist = entityLocation.distance(tornadoLoc);
                        if (entDist < 7) {
                            org.bukkit.util.Vector dir = entity.getLocation().getDirection().multiply(2);
                            org.bukkit.util.Vector vector = new org.bukkit.util.Vector(dir.getX(), 5 + (1 * (5 / entDist)), dir.getZ());
                            vector.normalize().multiply(2);
                            for (int i = 1; i < RANDOM.nextInt(20); i++) {
                                Bukkit.getScheduler().runTaskLater(this, () -> {
                                    entity.setVelocity(vector.rotateAroundY(Math.PI / 6).multiply(1.1));
                                }, i * 2L);
                            }
                        }
                    }
                } else if (dist < 50) {
                    inBossBarZone = true;
                }
            }
            if (inBossBarZone) {
                addToBossBar(player);
            } else {
                removeFromBossBar(player);
            }
        }
    }

    public void addToBossBar(Player player) {
        tornadoBossBar.addPlayer(player);
    }

    public void removeFromBossBar(Player player) {
        tornadoBossBar.removePlayer(player);
    }

    public void nameItemInHand(Player player, Tornado tornado) {
        ItemStack mainItem = player.getInventory().getItemInMainHand();
        if (mainItem == null || mainItem.getType() == Material.AIR) {
            return;
        }

        if (mainItem.getAmount() > 1) {
            return;
        }

        ItemMeta meta = mainItem.getItemMeta();

        if (meta.hasLore()) {
            return;
        }

        int firstIndex = tornado.getArea().indexOf(";");
        if (firstIndex == -1) firstIndex = tornado.getArea().length();
        String firstArea = tornado.getArea().substring(0, firstIndex);
        String location = "From the tornado of " + firstArea;
        String time = LocalDate.now().toString().replace("-", "/");
        List<String> lore = new ArrayList<>();
        lore.add(location);
        lore.add(time);
        meta.setLore(lore);

        mainItem.setItemMeta(meta);
        tornado.addPlayerReceivedNamedItem(player.getUniqueId());
    }

    // ASYNC
    public void updateTornadoes() {
        Set<Tornado> updatedTornadoes = extractTornadoInformation();

        // this.tornadoes = updatedTornadoes.stream().filter(t -> this.tornadoes.contains(t)).collect(Collectors.toSet());
        Set<Tornado> deadTornadoes = new HashSet<>();
        Set<Tornado> newTornadoes = new HashSet<>(updatedTornadoes);
        for (Tornado tornado : this.tornadoes) {
            double minDist = 999999;
            Tornado newTornadoIsEqualToOld = null;
            for (Tornado updatedTornado : updatedTornadoes) {
                double distance = updatedTornado.getLocation().distance(tornado.getLocation());
                if (updatedTornado.similar(tornado) && distance < minDist) {
                    minDist = distance;
                    newTornadoIsEqualToOld = updatedTornado;
                }
            }
            if (newTornadoIsEqualToOld != null && minDist != 999999) {
                tornado.update(newTornadoIsEqualToOld);
                newTornadoes.remove(newTornadoIsEqualToOld);
                updatedTornadoes.remove(newTornadoIsEqualToOld);
            } else {
                getLogger().info(tornado.getArea() + " tornado dissipated");
                deadTornadoes.add(tornado);
            }
        }
        for (Tornado dead : deadTornadoes) {
            dead.cancel();
        }
        this.tornadoes.removeAll(deadTornadoes);
        this.tornadoes.addAll(newTornadoes);

        for (Tornado tornado : newTornadoes) {
            getLogger().info("Formed tornado at " + tornado.getLocation().getBlockX() + " " + tornado.getLocation().getBlockZ());
            tornado.spawn();
        }
        getLogger().info(tornadoes.size() + " tornadoes total");

        if (tornadoes.size() <= 0) return;

        /*
        Bukkit.getScheduler().runTask(this, () -> {
            for (Tornado tornado : this.tornadoes) {
                tornado.getLocation().getWorld().playSound(tornado.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.WEATHER, 10F, 0.65F);
            }
        });
         */

        String locations = "";
        for (Tornado tornado : this.tornadoes) {
            locations += tornado.getArea() + "; ";
        }
        locations = locations.substring(0, locations.length() - 2);
        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD +
                "Tornado Warning in effect for the following areas: " + ChatColor.RESET + "" + ChatColor.RED + locations);
        // TODO get alerts and alert things, create polygons, find towns in alert zones etc
    }

    // ASYNC
    public Set<Tornado> extractTornadoInformation() {
        Set<Tornado> tornadoes = new HashSet<>();

        JsonElement jsonElement = getAlertData();
        if (jsonElement == null || jsonElement.isJsonNull()) return tornadoes;

        JsonObject rootobj = jsonElement.getAsJsonObject();

        JsonArray alerts = rootobj.getAsJsonArray("features");
        for (JsonElement alert : alerts) {
            JsonObject alertObj = alert.getAsJsonObject();
            if (alertObj == null || alertObj.isJsonNull()) {
                getLogger().warning("Alert Null");
                continue;
            }
            JsonObject properties = alertObj.get("properties").getAsJsonObject();
            JsonElement eventObj = properties.get("event");
            if (eventObj == null || eventObj.isJsonNull()) {
                getLogger().warning("Event Null");
                continue;
            }
            String event = eventObj.getAsString();
            if (!event.equals("Tornado Warning")) continue;

            JsonElement areaObj = properties.get("areaDesc");
            String area = "";
            if (areaObj == null || areaObj.isJsonNull()) {
                getLogger().warning("Area Null");
                area = "Unknown";
            } else {
                area = areaObj.getAsString();
            }

            JsonObject parameters = properties.get("parameters").getAsJsonObject();

            JsonArray eventMotion = parameters.getAsJsonArray("eventMotionDescription");
            if (eventMotion == null || eventMotion.isJsonNull()) {
                getLogger().warning("Event Motion Null");
                continue;
            }

            JsonElement descElement = eventMotion.get(0);
            if (descElement == null || descElement.isJsonNull()) {
                getLogger().warning("Event Desc Null");
                continue;
            }
            String desc = descElement.getAsString();
            String coords = desc.substring(desc.lastIndexOf("...") + 3)
                    .replace("...", "");

            String latStr = coords.substring(0, coords.indexOf(","));
            double lat = Double.valueOf(latStr);
            String lonStr = coords.substring(coords.indexOf(",") + 1);
            double lon = Double.valueOf(lonStr);

            Coordinate coord = this.getMCFromLife(lat, lon);

            Location location = new Location(Bukkit.getWorlds().get(0), coord.x, 64, coord.y);
            Tornado tornado = new Tornado(location, area);
            tornadoes.add(tornado);
        }

        return tornadoes;
    }

    // THIS IS FOR TESTING ONLY
    public JsonElement getAlertData2() {
        JsonElement json = null;
        try (Reader reader = new InputStreamReader(new FileInputStream("plugins/mcatlas-environment/test.json"), "UTF-8")) {
            json = new JsonParser().parse(reader);
        } catch (Exception e) {
            // do something
            this.getLogger().warning("FILE NOT FOUND");
            e.printStackTrace();
            return null;
        }
        return json;
    }

    // THIS IS THE REAL ALERT DATA
    @Nullable
    public JsonElement getAlertData() {
        String urlString = "https://api.weather.gov/alerts/active";
        URL url = null;
        HttpURLConnection connection = null;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
        } catch (Exception e) { // any issue with the connection, like an error code
            if (connection == null) {
                Bukkit.getLogger().warning("Weather.gov API down?");
                return null;
            }
            int code = 0;
            try {
                code = connection.getResponseCode();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (code == 500 && apiOffline) { // api is known to be offline
                return null;
            } else if (code == 500 && !apiOffline) { // api is not known to be offline
                Bukkit.getLogger().warning("Weather.gov API down?");
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

                Component text = Component.text(fahrenheit + "F").color(tempColor)
                                    .append(Component.text("/").color(NamedTextColor.GRAY)
                                    .append(Component.text(celsius + "C").color(tempColor)
                                    .append(Component.text(" - " + (int) weatherData.windSpeed + "mph Wind" + " - " + weatherData.weatherFullDesc)
                                        .color(NamedTextColor.GRAY)
                                )));
                for (int tick = 0; tick <= 25; tick += 5) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        player.sendActionBar(text);
                    }, tick);
                }
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

        final long lastUpdated = weatherPlayer.getLastUpdated();

        switch (condition) {
            case CLEAR: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.CLEAR);
                });
                break;
            }
            case STORM: // maybe one day add thunderstorm-per-user implementation? idk
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        if (EnvironmentUtil.RANDOM.nextInt(4) < 3) {
                            return;
                        }

                        double radian = EnvironmentUtil.RANDOM.nextDouble() * (Math.PI * 2);

                        player.playSound(player.getLocation().clone().add(Math.sin(radian) * 25, 0, Math.cos(radian) * 25),
                                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2F, .9F);
                    }
                }.runTaskTimer(this, 0L, 5 * 20L);
                break;
            case SAND: {
                BlockData sandDustData = Material.SAND.createBlockData();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        player.spawnParticle(Particle.FALLING_DUST,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                1, sandDustData);
                    }
                }.runTaskTimer(this, 0L, 1);
                break;
            }
            case SMOKE: {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 2);

                        player.spawnParticle(Particle.SMOKE_LARGE,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                0);
                        player.spawnParticle(Particle.SMOKE_NORMAL,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                0);
                        player.spawnParticle(Particle.REDSTONE,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                1, dustOptions);
                    }
                }.runTaskTimer(this, 0L, 3);
                break;
            }
            case MIST: {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            this.cancel();
                            return;
                        }

                        if (lastUpdated != weatherPlayer.getLastUpdated()) {
                            this.cancel();
                            return;
                        }

                        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 200, 255), 1);

                        player.spawnParticle(Particle.DRIP_WATER,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                1);
                        player.spawnParticle(Particle.REDSTONE,
                                location.clone().add((EnvironmentUtil.RANDOM.nextDouble() - .5) * 30, (EnvironmentUtil.RANDOM.nextDouble() - .25) * 10, (EnvironmentUtil.RANDOM.nextDouble() - .5) * 30),
                                1, dustOptions);
                    }
                }.runTaskTimer(this, 0L, 4);
                break;
            }
            case RAIN: {
                Bukkit.getScheduler().runTask(this, () -> {
                    player.setPlayerWeather(WeatherType.DOWNFALL);
                });
                break;
            }
        }

        if (weatherData.windSpeed > 10 || condition == Condition.STORM || condition == Condition.SAND || condition == Condition.SMOKE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }

                    if (lastUpdated != weatherPlayer.getLastUpdated()) {
                        cancel();
                        return;
                    }

                    player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, (float) (0.15F + ((weatherData.windSpeed - 10) / 80)), (float) 0.8);
                }
            }.runTaskTimer(this, 10L, 8 * 20L);
        }
    }

    // adds player to the queue for their weather to be updated
    public void addPlayerToQueue(Player player, WeatherPriority priority) {
        WeatherPlayer weatherPlayer = null;
        for (WeatherPlayer otherPlayer : allWeatherPlayers) {
            if (otherPlayer.getUUID().equals(player.getUniqueId())) {
                weatherPlayer = otherPlayer;
                weatherPlayer.setScore(priority.score);
                break;
            }
        }

        if (player == null) {
            return;
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
            case "Haze":
            case "Fog":
            case "Mist":
                return Condition.MIST;
            case "Sand":
                return Condition.SAND;
            case "Smoke":
            case "Dust":
            case "Ash":
                return Condition.SMOKE;
            case "Clear":
            case "Clouds":
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

    public Coordinate getMCFromLife(double lat, double lon) {
        double x = (lon * scaling);
        double z = (lat * scaling) * -1;
        return new Coordinate(x, z);
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
        MIST,
        SAND,
        SMOKE,
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

package net.mcatlas.environment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.ChatColor;

public class EnvironmentPlugin extends JavaPlugin {

	private static EnvironmentPlugin plugin;

	private String apiKey;
	private double scaling;
	private boolean apiOffline = false;

	private Queue<Player> playerQueue;

	public static final String HEY = ChatColor.RED + "" + ChatColor.BOLD + "Hey! " + ChatColor.RESET;
	public static final String WHOOSH = ChatColor.RED + "" + ChatColor.BOLD + "WHOOOSH. " + ChatColor.RESET;

	public static EnvironmentPlugin getEnvironment() {
		return plugin;
	}

	@Override
	public void onEnable() {
		plugin = this;

		getCommand("weatherhere").setExecutor(new TestingCommand());
		getServer().getPluginManager().registerEvents(new WorldListener(), this);

		saveDefaultConfig();
		apiKey = getConfig().getString("apiKey");
		scaling = getConfig().getDouble("scaling");

		playerQueue = new LinkedList<>();

		Bukkit.getScheduler().runTaskTimer(this, () -> {
			Collection<? extends Player> players = Bukkit.getWorlds().get(0).getPlayers();
			if (players.size() == 0) return;

			if (playerQueue.size() == 0) { 
				refreshQueueWithPlayers(players); 
			}
			Player player = playerQueue.poll();
			Location location = player.getLocation();

			CompletableFuture.runAsync(() -> {
				Condition condition = getCondition(location);

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
			});
		}, 0L, 60L); // pull a new player to set weather every 3 seconds
	}

	@Override
	public void onDisable() {
		plugin = null;
	}

	public String getAPIKey() {
		return apiKey;
	}

	public void refreshQueueWithPlayers(Collection<? extends Player> players) {
		playerQueue.clear();
		for (Player player : players) {
			playerQueue.add(player);
		}
	}

	// returns real life coordinate!!
	public Coordinate getLifeFromMC(int mcX, int mcY) {
		int x = (int) (mcX / scaling);
		int y = (int) (mcY / scaling) * -1;
		return new Coordinate(x, y);
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

		Coordinate coord = EnvironmentPlugin.getEnvironment()
				.getLifeFromMC(location.getBlockX(), location.getBlockZ());

		String urlString = "http://api.openweathermap.org/data/2.5/weather?lat=" + coord.y 
				+ "&lon=" + coord.x + "&appid=" + EnvironmentPlugin.getEnvironment().getAPIKey();
		URL url = null;
		HttpURLConnection connection = null;

		try {
			url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
		} catch (Exception e) {
			int code = 0;
			try {
				code = connection.getResponseCode();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (code == 500 && apiOffline) return noWeather;
			if (code == 500 && !apiOffline) {
				Bukkit.getLogger().warning("OpenWeatherMap went offline! No rain for now.");
				apiOffline = true;
				return noWeather;
			}
			if (code == 400) return noWeather;
			e.printStackTrace();
			return noWeather;
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

	public static boolean isOverworld(String worldName) {
		return (!isNether(worldName) && !isEnd(worldName));
	}

	public static boolean isNether(String worldName) {
		return worldName.endsWith("_nether");
	}

	public static boolean isEnd(String worldName) {
		return worldName.endsWith("_the_end");
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

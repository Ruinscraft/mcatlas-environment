package net.mcatlas.environment;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.lang.reflect.Field;

/**
 *  This exists just to check if the API is working, and to check accuracy
 *
 */
public class TestingCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		WeatherPlayer weatherPlayer = EnvironmentPlugin.get().getWeatherPlayer(player.getUniqueId());
		if (weatherPlayer == null) {
			player.sendMessage("Error");
			return false;
		}
		WeatherData weatherData = weatherPlayer.getWeatherData();
		for (Field field : weatherData.getClass().getDeclaredFields()) {
			try {
				player.sendMessage(ChatColor.YELLOW + field.getName() + " - " + ChatColor.RESET + field.get(weatherData));
			} catch (IllegalAccessException e) { }
		}

		if (args.length > 0) {
			String arg = args[0];
			int temp = 0;
			try {
				temp = Integer.parseInt(arg);
			} catch (Exception e) {}
			Color colorTemp = EnvironmentUtil.getColorFromTemperature(temp);
			ChatColor chatColor = net.md_5.bungee.api.ChatColor.of(colorTemp);
			player.sendMessage(chatColor + "This is the color of " + temp + " Fahrenheit");
		}

		return true;
	}

}

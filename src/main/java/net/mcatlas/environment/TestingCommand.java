package net.mcatlas.environment;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *  This exists just to check if the API is working, and to check accuracy
 *
 */
public class TestingCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		String weatherDesc = EnvironmentPlugin.getEnvironmentPlugin()
				.getDetailedWeatherDescription(player.getLocation());
		player.sendMessage(weatherDesc);

		return true;
	}

}

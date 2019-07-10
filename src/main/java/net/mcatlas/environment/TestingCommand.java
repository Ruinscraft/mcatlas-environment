package net.mcatlas.environment;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestingCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		String weatherDesc = EnvironmentPlugin.getEnvironment()
				.getDetailedWeatherDescription(player.getLocation());
		player.sendMessage(weatherDesc);

		return true;
	}

}

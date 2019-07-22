package net.mcatlas.environment;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class WorldListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		String worldName = block.getWorld().getName();

		if (block.getType() == Material.ENDER_CHEST) {
			player.sendMessage(EnvironmentPlugin.HEY + "Ender chests are disabled for now.");
			event.setCancelled(true);
			return;
		}

		if (worldName.endsWith("_nether") || worldName.endsWith("_the_end")) {
			if (block.getState() instanceof Container && block.getType() != Material.ENDER_CHEST) {
				player.sendMessage(EnvironmentPlugin.HEY + "You can't put containers in the Nether or The End.");

				event.setCancelled(true);
				return;
			}

			if (block.getType() == Material.ITEM_FRAME) {
				player.sendMessage(EnvironmentPlugin.HEY + "You can't put item frames in the Nether or The End.");
				event.setCancelled(true);
				return;
			}
		} else {
			int x = block.getX();
			int z = block.getZ();
			if (x > 21503 || x < -21504 || z > 10751 || z < -10752) {
				player.sendMessage(EnvironmentPlugin.HEY + "You can't build off to the Moon.");

				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if ((event.getEntityType() != EntityType.PIG_ZOMBIE) && 
				!(event.getEntityType() == EntityType.DROWNED)) return;

		for (int i = 0; i < event.getDrops().size(); i++) {
			if (event.getDrops().get(i).getType().equals(Material.GOLD_INGOT) || 
					event.getDrops().get(i).getType().equals(Material.GOLD_NUGGET)) {
				event.getDrops().remove(i);
				i--;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerPortalEvent event) {
		Location from = event.getFrom();
		Player player = event.getPlayer();

		for (ItemStack item : player.getInventory().getContents()) {
			if (item == null) continue;
			if (item.getType() == null) continue;
			if (item.getType().name().contains("SHULKER_BOX")) {
				player.sendMessage(EnvironmentPlugin.HEY + "Shulker boxes can't go through dimensions.");
				event.setCancelled(true);
				return;
			}
		}

		if (EnvironmentPlugin.isNether(event.getFrom().getWorld().getName())) {
			if (from.getBlockX() > 2688 || from.getBlockX() < -2688 ||
					from.getBlockZ() > 1340 || from.getBlockZ() < -1340) {
				player.sendMessage(EnvironmentPlugin.HEY + "You can't take a portal to outer space. " + 
						"Try getting closer to the Earth.");
				event.setCancelled(true);
				return;
			}
		}

		if (EnvironmentPlugin.isOverworld(event.getFrom().getWorld().getName())) {
			player.sendMessage(EnvironmentPlugin.HEY + "You can only teleport back to the Overworld through a " 
					+ "portal, so don't lose track of where your portal is!");
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryOpen(InventoryOpenEvent event) {
		String worldName = event.getPlayer().getWorld().getName();
		if (EnvironmentPlugin.isNether(worldName) || EnvironmentPlugin.isEnd(worldName)) {
			if (!(event.getInventory().getHolder() instanceof Player)) {
				event.getPlayer().sendMessage(EnvironmentPlugin.HEY + "You can't use this here.");
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerMove(PlayerMoveEvent event) {
		double x = event.getTo().getX();
		double y = event.getTo().getY();
		double z = event.getTo().getZ();
		Player player = event.getPlayer();

		if (EnvironmentPlugin.isNether(player.getWorld().getName())) {
			if (y >= 127) {
				player.sendMessage(EnvironmentPlugin.HEY + "You can't come up here.");
				player.teleport(event.getFrom());
				return;
			}
		}

		if (EnvironmentPlugin.isOverworld(player.getWorld().getName())) {
			Location newLocation;
			if (x > 21500) {
				newLocation = getAir(new Location(player.getWorld(), -21499, y, z, 
						event.getTo().getYaw(), event.getTo().getPitch()));
				player.sendMessage(EnvironmentPlugin.WHOOSH + "You just traversed the International Date Line.");
			} else if (x < -21500) {
				newLocation = getAir(new Location(player.getWorld(), 21499, y, z, 
						event.getTo().getYaw(), event.getTo().getPitch()));
				player.sendMessage(EnvironmentPlugin.WHOOSH + "You just traversed the International Date Line.");
			} else if (z > 10748) {
				float yaw = 0;
				if (0 <= event.getTo().getYaw() && 180 >= event.getTo().getYaw()) {
					yaw = 180 - event.getTo().getYaw();
				} else if (180 <= event.getTo().getYaw() && 360 >= event.getTo().getYaw()) {
					yaw = 540 - event.getTo().getYaw();
				}

				int newx = (int) x + 21500;
				if (newx > 21499) {
					newx = newx - 42990;
				}

				newLocation = getAir(new Location(player.getWorld(), newx, y, 10746, 
						yaw, event.getTo().getPitch()));
				player.sendMessage(EnvironmentPlugin.WHOOSH + "You've crossed the South Pole. How did that work?");
			} else if (z < -10748) {
				float yaw = 0;
				if (0 <= event.getTo().getYaw() && 180 >= event.getTo().getYaw()) {
					yaw = 180 - event.getTo().getYaw();
				} else if (180 <= event.getTo().getYaw() && 360 >= event.getTo().getYaw()) {
					yaw = 540 - event.getTo().getYaw();
				}

				int newx = (int) x + 21500;
				if (newx > 21499) {
					newx = newx - 42990;
				}

				newLocation = getAir(new Location(player.getWorld(), newx, y, -10746, 
						yaw, event.getTo().getPitch()));
				player.sendMessage(EnvironmentPlugin.WHOOSH + "You've crossed the North Pole. How did that work?");
			} else {
				return;
			}

			if (player.isInsideVehicle()) {
				Entity vehicle = player.getVehicle();
				Entity newVehicle = player.getWorld().spawnEntity(newLocation, vehicle.getType());
				if (vehicle.getType() == EntityType.BOAT) {
					((Boat) newVehicle).setWoodType(((Boat) vehicle).getWoodType());
				}

				vehicle.remove();
				player.teleport(newLocation);
				newVehicle.addPassenger(player);
			} else {
				player.teleport(newLocation);
			}
		}
	}
	// x > 21503 || x < -21504 || z > 10751 || z < -10752

	private Location getAir(Location location) {
		if (location.getBlock().getType().equals(Material.AIR)) {
			return location;
		} else if (location.getBlockY() > 200) { // lose hope and give up
			return location;
		} else {
			return getAir(new Location(location.getWorld(), 
					location.getBlockX(), location.getBlockY() + 2, location.getBlockZ(),
					location.getYaw(), location.getPitch()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		if (EnvironmentPlugin.isNether(player.getWorld().getName())) {
			if (event.getTo().getY() >= 127) {
				if (EnvironmentPlugin.isOverworld(event.getTo().getWorld().getName())) return;
				player.sendMessage(EnvironmentPlugin.HEY + "You can't come up here.");
				event.setCancelled(true);
				return;
			}
		}
	}

}

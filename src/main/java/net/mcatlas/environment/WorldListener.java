package net.mcatlas.environment;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;

import static net.mcatlas.environment.EnvironmentUtil.*;

/**
 * Environment listener for various things
 *
 */
public class WorldListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onWorldInit(WorldInitEvent event) {
		Bukkit.getLogger().info("WORLD IS INIT OMG");
		World world = event.getWorld();

		Bukkit.getLogger().info("" + world.getEnvironment());
		if (world.getEnvironment() == World.Environment.NETHER) {
			Bukkit.getLogger().info("!!!! Ok its added!!!!!!!!!!!!!!!!!");
			world.getPopulators().add(new NetherPopulator());
		}
	}

	// When block is placed
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		String worldName = block.getWorld().getName();

		// No ender chests!!!
		if (block.getType() == Material.ENDER_CHEST) {
			player.sendMessage(MSG_HEY 
					+ "Ender chests are disabled. Store your stuff elsewhere!");
			event.setCancelled(true);
			return;
		}

		// No methods of storing items in not-Overworld
		if (!EnvironmentPlugin.isOverworld(block.getWorld())) {
			if (block.getState() instanceof Container 
					&& block.getType() != Material.ENDER_CHEST) {
				player.sendMessage(MSG_HEY
						+ "You can't put containers in the Nether or The End.");

				event.setCancelled(true);
				return;
			}

			if (block.getType() == Material.ITEM_FRAME) {
				player.sendMessage(MSG_HEY
						+ "You can't put item frames in the Nether or The End.");
				event.setCancelled(true);
				return;
			}
		} else {
			int x = block.getX();
			int z = block.getZ();
			// Can't build outside of world border
			if (x > 21503 || x < -21504 || z > 10751 || z < -10752) {
				player.sendMessage(MSG_HEY 
						+ "You can't build off to the Moon.");

				event.setCancelled(true);
				return;
			}
		}
	}

	// Remove (most) gold drops from pigmen
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntityType() == EntityType.BEE) {
			if (event.getEntity().getWorld().getEnvironment() != World.Environment.NETHER) {
				return;
			}

			event.getDrops().add(new ItemStack(Material.NAUTILUS_SHELL, EnvironmentUtil.RANDOM.nextInt(2) + 1));
			if (EnvironmentUtil.chance(50)) {
				event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, 1));
			}
			return;
		}

		if ((event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) &&
				!(event.getEntityType() == EntityType.DROWNED)) return;

		for (int i = 0; i < event.getDrops().size(); i++) {
			if (event.getDrops().get(i).getType().equals(Material.GOLD_INGOT) || 
					event.getDrops().get(i).getType().equals(Material.GOLD_NUGGET) ||
					event.getDrops().get(i).getType().equals(Material.GOLDEN_SWORD)) {
				event.getDrops().remove(i);
				i--;
			}
		}
	}

	// When a player enters a portal
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerPortalEvent event) {
		Location from = event.getFrom();
		Player player = event.getPlayer();

		// If not teleporting to the overworld
		if (!EnvironmentPlugin.isOverworld(event.getTo().getWorld())) {
			// No shulker boxes in other dimensions
			for (ItemStack item : player.getInventory().getContents()) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}

				if (item.getType().name().contains("SHULKER_BOX")) {
					player.sendMessage(MSG_HEY
							+ "Shulker boxes can't go to other dimensions.");
					event.setCancelled(true);
					return;
				}
			}
		}

		// Prevent users from teleporting outside of Overworld from the Nether
		if (EnvironmentPlugin.isNether(event.getFrom().getWorld())) {
			if (from.getBlockX() > 2688 || from.getBlockX() < -2688 ||
					from.getBlockZ() > 1340 || from.getBlockZ() < -1340) {
				player.sendMessage(MSG_HEY 
						+ "You can't take a portal to outer space. "
						+ "Try getting closer to the Earth.");
				event.setCancelled(true);
				return;
			}
		}

		// Alert when entering Nether
		if (EnvironmentPlugin.isNether(event.getTo().getWorld())) {
			player.sendMessage(MSG_HEY + "You can only teleport back to the " 
					+ "Overworld through a portal, so don't " 
					+ "lose track of where your portal is!");
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (event.getEntityType() == EntityType.BEE) {
			event.setCancelled(true);
		}
	}

	// No accessing of containers outside of Overworld
	// Maybe breakNaturally the container when opening it instead of not allowing at all
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (EnvironmentPlugin.isNether(event.getPlayer().getWorld())) {
			if (!(event.getInventory().getHolder() instanceof Player)) {
				event.getPlayer().sendMessage(MSG_HEY 
						+ "You can't use this here.");
				event.setCancelled(true);
				return;
			}
		} else if (EnvironmentPlugin.isEnd(event.getPlayer().getWorld())) {
			if (event.getInventory().getHolder() instanceof Player) {
				return;
			}
			if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
				event.getPlayer().sendMessage(MSG_HEY
						+ "You can't use this here.");
				event.setCancelled(true);
				return;
			}
		}
	}

	// Handle teleporting across borders
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMove(PlayerMoveEvent event) {
		double x = event.getTo().getX();
		double y = event.getTo().getY();
		double z = event.getTo().getZ();
		Player player = event.getPlayer();

		if (EnvironmentPlugin.isNether(player.getWorld())) {
			if (y >= 127) {
				player.sendMessage(MSG_HEY + "You can't come up here.");
				player.teleport(event.getFrom());
				return;
			}
		}

		if (EnvironmentPlugin.isOverworld(player.getWorld())) {
			Location newLocation;
			if (x > 21500) {
				newLocation = getAir(new Location(player.getWorld(), -21499, y, z, 
						event.getTo().getYaw(), event.getTo().getPitch()));
				player.sendMessage(MSG_WHOOSH 
						+ "You just traversed the International Date Line.");
			} else if (x < -21500) {
				newLocation = getAir(new Location(player.getWorld(), 21499, y, z, 
						event.getTo().getYaw(), event.getTo().getPitch()));
				player.sendMessage(MSG_WHOOSH 
						+ "You just traversed the International Date Line.");
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
				player.sendMessage(MSG_WHOOSH 
						+ "You've crossed the South Pole. How did that work?");
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
				player.sendMessage(MSG_WHOOSH 
						+ "You've crossed the North Pole. How did that work?");
			} else {
				return;
			}

			// TODO: Handle horses/etc. being carried across a border?
			// might need to make this a higher priority to avoid anti-cheat
			if (player.isInsideVehicle()) {
				Entity vehicle = player.getVehicle();
				Entity newVehicle = player.getWorld()
						.spawnEntity(newLocation, vehicle.getType());
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

	// TODO: fix an underground teleporting issue across the north/south
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

	// No nether roof access!
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		if (EnvironmentPlugin.isNether(player.getWorld())) {
			if (event.getTo().getY() >= 127) {
				if (EnvironmentPlugin.isOverworld(event.getTo().getWorld())) return;
				player.sendMessage(MSG_HEY + "You can't come up here.");
				event.setCancelled(true);
				return;
			}
		}
	}

}

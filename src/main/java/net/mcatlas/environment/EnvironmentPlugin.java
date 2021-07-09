package net.mcatlas.environment;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles elements of the MCAtlas world itself.
 */
public class EnvironmentPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            setupNetherBees();
        }, 20 * 20, 20 * 20);
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new VoidChunkGenerator();
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

    public static boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    public static boolean isNether(World world) {
        return world.getEnvironment() == World.Environment.NETHER;
    }

    public static boolean isEnd(World world) {
        return world.getEnvironment() == World.Environment.THE_END;
    }


}

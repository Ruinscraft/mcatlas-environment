package net.mcatlas.environment;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;

import static net.mcatlas.environment.EnvironmentUtil.*;

import java.util.Random;

public class NetherPopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        Bukkit.getLogger().info("CHUNK.");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 1; y < 24; y++) { // only y=23 and below
                    Block block = chunk.getBlock(x, y, z);
                    BlockData blockData = block.getBlockData();

                    if (blockData.getMaterial() == Material.AIR) {
                        continue;
                    }

                    switch (blockData.getMaterial()) {
                        case LAVA:
                            if (y <= 5) {
                                set(block, Material.NETHER_GOLD_ORE);
                                continue;
                            } else if (y == 6) {
                                if (chance(50)) {
                                    set(block, Material.NETHER_GOLD_ORE);
                                    continue;
                                } else {
                                    set(block, Material.NETHERRACK);
                                }
                            } else if (y >= 21) {
                                continue;
                            } else {
                                set(block, Material.NETHERRACK);
                            }
                            break;
                        case ANCIENT_DEBRIS:
                            if (chance(50)) {
                                set(block, Material.GOLD_BLOCK);
                            }
                            continue;
                        case BEDROCK:
                        case BLACKSTONE:
                        case NETHER_GOLD_ORE:
                        case NETHER_QUARTZ_ORE:
                            continue;
                    }

                    if (y == 23) {
                        if (chance(60)) {
                            continue;
                        }
                    } else if (y == 22) {
                        if (chance(25)) {
                            continue;
                        }
                    }

                    Biome biome = block.getBiome();
                    switch (biome) {
                        case NETHER_WASTES:
                            if (y <= 5) {
                                set(block, Material.BLACK_CONCRETE);
                            } else if (y <= 6) {
                                set(block, Material.BROWN_CONCRETE);
                            } else if (y <= 7) {
                                set(block, Material.GRAY_CONCRETE);
                            } else if (y <= 8) {
                                if (chance(50)) {
                                    set(block, Material.GRAY_CONCRETE);
                                } else {
                                    set(block, Material.LIGHT_GRAY_CONCRETE);
                                }
                            } else if (y <= 9) {
                                set(block, Material.BLUE_CONCRETE);
                            } else if (y <= 10) {
                                if (chance(66)) {
                                    set(block, Material.BLUE_CONCRETE);
                                } else {
                                    set(block, Material.PURPLE_CONCRETE);
                                }
                            } else if (y <= 11) {
                                if (chance(66)) {
                                    set(block, Material.PURPLE_CONCRETE);
                                } else {
                                    set(block, Material.MAGENTA_CONCRETE);
                                }
                            } else if (y <= 12) {
                                if (chance(75)) {
                                    set(block, Material.CYAN_CONCRETE);
                                } else if (chance(75)) {
                                    set(block, Material.MAGENTA_CONCRETE);
                                } else {
                                    set(block, Material.LIGHT_BLUE_CONCRETE);
                                }
                            } else if (y <= 13) {
                                set(block, Material.LIGHT_BLUE_CONCRETE);
                            } else if (y <= 14) {
                                set(block, Material.GREEN_CONCRETE);
                            } else if (y <= 15) {
                                if (chance(50)) {
                                    set(block, Material.GREEN_CONCRETE);
                                } else {
                                    set(block, Material.LIME_CONCRETE);
                                }
                            } else if (y <= 16) {
                                set(block, Material.YELLOW_CONCRETE);
                            } else if (y <= 17) {
                                if (chance(50)) {
                                    set(block, Material.YELLOW_CONCRETE);
                                } else {
                                    set(block, Material.WHITE_CONCRETE);
                                }
                            } else if (y <= 19) {
                                set(block, Material.WHITE_CONCRETE);
                            } else if (y <= 20) {
                                if (chance(80)) {
                                    set(block, Material.PINK_CONCRETE);
                                } else if (chance(50)) {
                                    set(block, Material.WHITE_CONCRETE);
                                } else {
                                    set(block, Material.RED_CONCRETE);
                                }
                            } else if (y <= 21) {
                                set(block, Material.RED_CONCRETE);
                            } else if (y <= 22) {
                                if (chance(50)) {
                                    set(block, Material.RED_CONCRETE);
                                } else {
                                    set(block, Material.ORANGE_CONCRETE);
                                }
                            } else {
                                set(block, Material.ORANGE_CONCRETE);
                            }
                            continue;
                        case BASALT_DELTAS:
                            if (y <= 5) {
                                set(block, Material.BLACK_TERRACOTTA);
                            } else if (y <= 6) {
                                set(block, Material.BROWN_TERRACOTTA);
                            } else if (y <= 7) {
                                set(block, Material.GRAY_TERRACOTTA);
                            } else if (y <= 8) {
                                if (chance(50)) {
                                    set(block, Material.GRAY_TERRACOTTA);
                                } else {
                                    set(block, Material.LIGHT_GRAY_TERRACOTTA);
                                }
                            } else if (y <= 9) {
                                set(block, Material.BLUE_TERRACOTTA);
                            } else if (y <= 10) {
                                if (chance(66)) {
                                    set(block, Material.BLUE_TERRACOTTA);
                                } else {
                                    set(block, Material.PURPLE_TERRACOTTA);
                                }
                            } else if (y <= 11) {
                                if (chance(66)) {
                                    set(block, Material.PURPLE_TERRACOTTA);
                                } else {
                                    set(block, Material.MAGENTA_TERRACOTTA);
                                }
                            } else if (y <= 12) {
                                if (chance(75)) {
                                    set(block, Material.CYAN_TERRACOTTA);
                                } else if (chance(75)) {
                                    set(block, Material.MAGENTA_TERRACOTTA);
                                } else {
                                    set(block, Material.LIGHT_BLUE_TERRACOTTA);
                                }
                            } else if (y <= 13) {
                                set(block, Material.LIGHT_BLUE_TERRACOTTA);
                            } else if (y <= 14) {
                                set(block, Material.GREEN_TERRACOTTA);
                            } else if (y <= 15) {
                                if (chance(50)) {
                                    set(block, Material.GREEN_TERRACOTTA);
                                } else {
                                    set(block, Material.LIME_TERRACOTTA);
                                }
                            } else if (y <= 16) {
                                set(block, Material.YELLOW_TERRACOTTA);
                            } else if (y <= 17) {
                                if (chance(50)) {
                                    set(block, Material.YELLOW_TERRACOTTA);
                                } else {
                                    set(block, Material.WHITE_TERRACOTTA);
                                }
                            } else if (y <= 19) {
                                set(block, Material.WHITE_TERRACOTTA);
                            } else if (y <= 20) {
                                if (chance(80)) {
                                    set(block, Material.PINK_TERRACOTTA);
                                } else if (chance(50)) {
                                    set(block, Material.WHITE_TERRACOTTA);
                                } else {
                                    set(block, Material.RED_TERRACOTTA);
                                }
                            } else if (y <= 21) {
                                set(block, Material.RED_TERRACOTTA);
                            } else if (y <= 22) {
                                if (chance(50)) {
                                    set(block, Material.RED_TERRACOTTA);
                                } else {
                                    set(block, Material.ORANGE_TERRACOTTA);
                                }
                            } else {
                                set(block, Material.ORANGE_TERRACOTTA);
                            }
                            continue;
                        case CRIMSON_FOREST:
                            if (y <= 13) {
                                set(block, Material.GRAVEL);
                            } else if (y <= 15) {
                                if (chance(50)) {
                                    set(block, Material.GRAVEL);
                                } else {
                                    set(block, Material.CLAY);
                                }
                            } else {
                                set(block, Material.CLAY);
                            }
                            continue;
                        case SOUL_SAND_VALLEY:
                            if (y <= 7) {
                                set(block, Material.RED_SAND);
                            } else if (y <= 10) {
                                if (chance(50)) {
                                    set(block, Material.RED_SAND);
                                } else {
                                    set(block, Material.SAND);
                                }
                            } else {
                                set(block, Material.SAND);
                            }
                            continue;
                        case WARPED_FOREST:
                            if (y <= 7) {
                                set(block, Material.RED_SANDSTONE);
                            } else if (y <= 10) {
                                if (chance(50)) {
                                    set(block, Material.RED_SANDSTONE);
                                } else {
                                    set(block, Material.SANDSTONE);
                                }
                            } else {
                                set(block, Material.SANDSTONE);
                            }
                            continue;
                    }
                }
            }
        }
        Runtime.getRuntime().gc();
    }

}

package net.mcatlas.environment;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Beehive;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static net.mcatlas.environment.EnvironmentUtil.*;

import java.util.Random;

public class NetherPopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 1; y < 240; y++) { // only y=23 and below
                    Block block = chunk.getBlock(x, y, z);
                    BlockData blockData = block.getBlockData();

                    // we don't need to edit air blocks
                    if (blockData.getMaterial() == Material.AIR) {
                        continue;
                    }

                    // blocks to edit that are above the bottom layers of the map
                    if (y >= 24) {
                        switch (blockData.getMaterial()) {
                            // replace nether gold with quartz
                            case NETHER_GOLD_ORE:
                                if (chance(90)) {
                                    set(block, Material.NETHER_QUARTZ_ORE);
                                }
                                continue;
                            // gold blocks in bastions will have some beehives with angry bees interspersed
                            case GOLD_BLOCK:
                                if (chance(20)) {
                                    set(block, Material.BEE_NEST);
                                    Beehive beehive = (Beehive) block.getState();
                                    beehive.setMaxEntities(10);
                                    if (chance(50)) {
                                        for (int i = 0; i < 2; i++) {
                                            Bee bee = (Bee) world.spawnEntity(block.getLocation(), EntityType.BEE);
                                            bee.setAnger(999999); // ALWAYS ANGRY (maybe)
                                            beehive.addEntity(bee);
                                        }
                                    }
                                    if (chance(40)) {
                                        Location spawnLocation = block.getLocation().clone().add(0, random.nextInt(10), 0);
                                        spawnLocation.getBlock().setType(Material.AIR);
                                        Bee bee = (Bee) world.spawnEntity(spawnLocation, EntityType.BEE);
                                        bee.setAnger(999999);
                                        bee.setHive(block.getLocation());
                                        bee.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 1, true, false));
                                        AttributeInstance attr = bee.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                                        attr.setBaseValue(200D);
                                        bee.setHealth(200D);
                                    }
                                }
                                continue;
                            case CHEST:
                                Chest chest = (Chest) block.getState();
                                if (chance(10)) {
                                    chest.getBlockInventory().addItem(new ItemStack(Material.PANDA_SPAWN_EGG, 1));
                                }
                                if (chance(10)) {
                                    chest.getBlockInventory().addItem(new ItemStack(Material.NAUTILUS_SHELL, 1));
                                    if (chance(50)) {
                                        chest.getBlockInventory().addItem(new ItemStack(Material.NAUTILUS_SHELL, 1));
                                    }
                                }

                                int maxCoord = Math.abs(x) > Math.abs(z) ? Math.abs(x) : Math.abs(z);
                                int multiplier = 1 + (maxCoord / 3000);
                                if (multiplier > 5) multiplier = 5;
                                for (ItemStack item : chest.getBlockInventory().getContents()) {
                                    int newStackSize = multiplier * item.getAmount();
                                    if (newStackSize > item.getMaxStackSize()) {
                                        continue;
                                    }
                                    item.setAmount(newStackSize);
                                }
                                if (chance(80)) {
                                    chest.getBlockInventory().remove(Material.ENCHANTED_GOLDEN_APPLE);
                                }
                                continue;
                        }
                    }

                    if (y >= 24) continue;

                    // from this point on, anything below y=24

                    // these are blocks which should be left alone before making the bottom nether layers, and/or should be edited beforehand
                    switch (blockData.getMaterial()) {
                        // replace with nether gold ore around bedrock level, then just replace with netherrack otherwise
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
                        // 40% of ancient debris is removed; ~10% replaced with gold blocks
                        case ANCIENT_DEBRIS:
                            if (chance(40)) {
                                break;
                            }
                            if (chance(10)) {
                                set(block, Material.GOLD_BLOCK);
                            }
                            continue;
                        // these blocks are skipped and left as is
                        case BEDROCK:
                        case BLACKSTONE:
                        case NETHER_GOLD_ORE:
                        case NETHER_QUARTZ_ORE:
                            continue;
                    }

                    // from this point on, all of the blocks below y=24 that werent already covered will be edited

                    // some blocks near y=24 are skipped for a nice transition into the lower layers
                    if (y == 23) {
                        if (chance(60)) {
                            continue;
                        }
                    } else if (y == 22) {
                        if (chance(25)) {
                            continue;
                        }
                    }

                    // ANY blocks that reached this far will now be replaced with the various layers at the bottom of the map
                    // depending on the biome
                    Biome biome = block.getBiome();
                    switch (biome) {
                        // nether wastes: concrete layer
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
                        // basalt deltas: terracotta layer
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
                        // crimson forest: gravel and clay layer
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
                        // soul sand valley: sand and red sand layer
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
                        // warped forest: sandstone and red sandstone layer
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
    }

}

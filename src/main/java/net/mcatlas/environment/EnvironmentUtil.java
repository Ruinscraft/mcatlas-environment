package net.mcatlas.environment;

import org.bukkit.*;
import org.bukkit.block.Block;

import java.awt.*;
import java.awt.Color;
import java.util.Random;

public class EnvironmentUtil {

    public static Random RANDOM = new Random();

    public static final String MSG_HEY = ChatColor.RED + ""
            + ChatColor.BOLD + "Hey! " + ChatColor.RESET;
    public static final String MSG_WHOOSH = ChatColor.RED + ""
            + ChatColor.BOLD + "WHOOOSH. " + ChatColor.RESET;

    public static String[] directions = { "N", "NE", "E", "SE", "S", "SW", "W", "NW", "N" };

    public static Block set(Block block, Material material) {
        block.setType(material, false);
        return block;
    }

    public static double kelvinToFahrenheit(double kelvin) {
        double celsius = kelvinToCelsius(kelvin);
        return (celsius * 9/5) + 32;
    }

    public static double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    public static Color getColorFromTemperature(double fahrenheit) {
        double r = 0;
        double g = 0;
        double b = 0;

        // RED
        if (fahrenheit > 75) {
            r = 255;
        } else if (fahrenheit > 55) {
            r = 255 - (75 - fahrenheit);
        } else if (fahrenheit > -65) {
            // -65F to 55F colors
            // -65F is 0, 55F is 235
            r = (((fahrenheit + 65) / 120) * 235);
        } else {
            r = 0;
        }

        // GREEN
        if (fahrenheit > 125) {
            g = 0;
        } else if (fahrenheit > 55) {
            // 125F is 0, 55F is 255
            g = 255 - (((fahrenheit - 55) / 70) * 255);
        } else if (fahrenheit >= -65) {
            // -65F to 35F
            // -65F is 55, 55F is 255
            g = (((fahrenheit + 65) / 120) * 200) + 55;
        } else {
            g = 55;
        }

        // BLUE
        if (fahrenheit > 75) {
            // b is 0 >= 75F
            b = 0;
        } else if (fahrenheit > 55) {
            // b is 200 at 55F
            // 200 at 55F to 0 at 75F
            b = 255 - ((((fahrenheit - 55) / 20) * 200) + 55);
        } else if (fahrenheit > 35) {
            // b is 255 at 35F
            // 255 at 35F to 200 at 55F
            b = 255 - (((fahrenheit - 35) / 20) * 55);
        } else {
            // b is 255 below 35
            b = 255;
        }

        Color color = new Color((int) r, (int) g, (int) b);

        return color;
    }

    public static void createTornado(Location location) {
        location.setY(location.getWorld().getHighestBlockYAt(location) + 1.25);

        World world = location.getWorld();

        Bukkit.getScheduler().runTaskTimerAsynchronously(EnvironmentPlugin.get(), () -> {
            world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
            world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
            world.spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
        }, 10, 2);

        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 5);

        Bukkit.getScheduler().runTaskTimerAsynchronously(EnvironmentPlugin.get(), () -> {
            for (double height = .75; height < 16; height += .75) {
                double width = 1.25 + ((height * height) / 40);
                double radian = RANDOM.nextDouble() * (Math.PI * 2);
                world.spawnParticle(Particle.REDSTONE, location.clone().add(Math.sin(radian) * width, height,  Math.cos(radian) * width), 1, dustOptions);
            }
        }, 10, 3);
    }

    public static String degreesToCardinal(double degrees) {
        return directions[(int) Math.round(((double) degrees % 360) / 45)];
    }

    /**
     * Returns true number% of the time.
     * Example: (40) would give you a 40% chance to return true.
     * @param number double out of 100
     * @return true number% of the time, false (100 - number)% of the time
     */
    public static boolean chance(double number) {
        return chanceOutOf(number, 100);
    }

    /**
     * Returns true number% of the time.
     * Example: (40) would give you a 40% chance to return true.
     * @param number integer out of 100
     * @return true number% of the time, false (100 - number)% of the time
     */
    public static boolean chance(int number) {
        return chanceOutOf(number, 100);
    }

    /**
     * Returns true number out of outOf times.
     * Example: (50, 250) is a 20% chance.
     * @param number double smaller than
     * @param outOf any value
     * @return true depending on your chance for it to be true, false if not
     */
    public static boolean chanceOutOf(double number, int outOf) {
        return chanceOutOf((int) number, outOf);
    }

    /**
     * Returns true number out of outOf times.
     * Example: (50, 250) is a 20% chance.
     * @param number integer smaller than outOf
     * @param outOf any value
     * @return true depending on your chance for it to be true, false if not
     */
    public static boolean chanceOutOf(int number, int outOf) {
        number--;
        if (number < 0) return false;

        if (number >= outOf) return true;

        int nextInt = RANDOM.nextInt(outOf);

        return number >= nextInt;
    }

}

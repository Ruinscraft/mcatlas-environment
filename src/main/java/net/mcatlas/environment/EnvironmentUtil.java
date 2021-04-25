package net.mcatlas.environment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Random;

public class EnvironmentUtil {

    public static Random RANDOM = new Random();

    public static final String MSG_HEY = ChatColor.RED + ""
            + ChatColor.BOLD + "Hey! " + ChatColor.RESET;
    public static final String MSG_WHOOSH = ChatColor.RED + ""
            + ChatColor.BOLD + "WHOOOSH. " + ChatColor.RESET;

    public static Block set(Block block, Material material) {
        block.setType(material, false);
        return block;
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

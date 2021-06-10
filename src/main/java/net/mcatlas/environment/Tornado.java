package net.mcatlas.environment;

import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import static net.mcatlas.environment.EnvironmentUtil.*;

public class Tornado {

    private Location location;
    private String area;
    private int radiusWarning; //unused currently
    private boolean cancelled = false;

    public Tornado(Location location, String area) {
        this.location = location;
        this.area = area;
        this.radiusWarning = 80;
    }

    public Location getLocation() {
        return this.location;
    }

    public String getArea() {
        return this.area;
    }

    public void update(Tornado other) {
        Location newLocation = other.getLocation().clone();
        this.area = other.getArea();
        double xDiff = newLocation.getX() - location.getX();
        double zDiff = newLocation.getZ() - location.getZ();
        Bukkit.getScheduler().runTask(EnvironmentPlugin.get(), () -> {
            location.add(xDiff / 100, 0, zDiff / 100);
        });

        final int endMovingLocation = EnvironmentPlugin.get().getServer().getCurrentTick() + (4 * 100);
        new BukkitRunnable() {
            @Override
            public void run() {
                location.add(xDiff / 100, 0, zDiff / 100);
                if (endMovingLocation > EnvironmentPlugin.get().getServer().getCurrentTick() || cancelled) {
                    cancel();
                }
            }
        }.runTaskTimer(EnvironmentPlugin.get(), 0L, 4L);
    }

    public void spawn() {
        World world = location.getWorld();

        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 6);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (cancelled) {
                    cancel();
                }

                Bukkit.getScheduler().runTask(EnvironmentPlugin.get(), () -> { // just to be safe
                    location.setY(location.getWorld().getHighestBlockYAt(location) + 1.25);
                });

                world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                world.spawnParticle(Particle.EXPLOSION_NORMAL, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);
                world.spawnParticle(Particle.EXPLOSION_LARGE, location.clone().add((RANDOM.nextDouble() - .5) * 3, 0, (RANDOM.nextDouble() - .5) * 4), 1);

                for (double height = .75; height < 20; height += .9) {
                    double width = 1.25 + ((height * height) / 40);
                    double radian = RANDOM.nextDouble() * (Math.PI * 2);
                    Location spawnParticle = location.clone().add(Math.sin(radian) * width, height,  Math.cos(radian) * width);
                    if (chance(2)) {
                        world.playSound(spawnParticle.clone(), Sound.ITEM_ELYTRA_FLYING, SoundCategory.WEATHER, 0.4F, 0.8F);
                    }
                    world.spawnParticle(Particle.REDSTONE, location.clone().add(Math.sin(radian) * width, height,  Math.cos(radian) * width), 1, dustOptions);
                }
            }
        }.runTaskTimerAsynchronously(EnvironmentPlugin.get(), 10L, 2L);
    }

    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean equals(Object obj) {
        System.out.println("UHHH");
        if (!(obj instanceof Tornado)) return false;
        Tornado tornado = (Tornado) obj;
        if (getLocation().equals(tornado.getLocation())) {
            System.out.println("TRUE");
            return true;
        }
        if (tornado.getLocation() == null) return false;

        Location orig = getLocation().clone();
        orig.setY(64);
        Location newLoc = tornado.getLocation().clone();
        newLoc.setY(64);
        if (orig.distance(newLoc) < 10) {
            System.out.println("TRUEE");
            return true;
        }
        System.out.println("UHHHHH");
        return false;
    }

}

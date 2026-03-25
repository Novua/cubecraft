package net.novua.cc_trails;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Renders a mathematically-defined particle trail using a helix/spiral equation.
 */
public final class HelixTrailRenderer {

    /**
     * Parametric helix around the player's path direction:
     * x(t) = r * cos(t)
     * y(t) = v * t
     * z(t) = r * sin(t)
     *
     * The local x/z axes are mapped to the player's right/up vectors so the
     * spiral follows player orientation instead of world axes.
     */
    public void render(Player player, TrailManager.Settings settings, double phase, TrailStyle style, int groupFrame) {
        World world = player.getWorld();
        Location baseLocation = player.getLocation().clone().add(0.0, settings.verticalOffset(), 0.0);

        // Force horizontal-only orientation by removing pitch influence.
        Vector forward = player.getLocation().getDirection().setY(0.0);
        if (forward.lengthSquared() < 1.0e-6) {
            forward = new Vector(0, 0, 1);
        } else {
            forward.normalize();
        }
        Vector worldUp = new Vector(0, 1, 0);
        Vector right = worldUp.clone().crossProduct(forward);

        if (right.lengthSquared() < 1.0e-6) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }

        for (int i = 0; i < settings.points(); i++) {
            if ((i % settings.groupSize()) != groupFrame) {
                continue;
            }

            double t = phase + (i * settings.step());

            double angle = t * settings.frequency();
            Vector offset;
            if (style == TrailStyle.ORBIT) {
                // Flat horizontal orbit around the player.
                double side = settings.radius() * Math.cos(angle);
                double front = settings.radius() * Math.sin(angle);
                offset = right.clone().multiply(side)
                        .add(forward.clone().multiply(front));
            } else if (style == TrailStyle.WAVE) {
                // Sine wave trailing behind the player, not to the side.
                double back = i * settings.step();
                double side = settings.radius() * Math.sin(angle);
                offset = right.clone().multiply(side)
                        .subtract(forward.clone().multiply(back));
            } else {
                // Helix corkscrew trailing behind the player.
                double back = settings.verticalStretch() * i;
                double side = settings.radius() * Math.cos(angle);
                double vertical = settings.radius() * Math.sin(angle);
                offset = right.clone().multiply(side)
                        .add(worldUp.clone().multiply(vertical))
                        .subtract(forward.clone().multiply(back));
            }

            Location particleLocation = baseLocation.clone().add(offset);
            world.spawnParticle(
                    settings.particle(),
                    particleLocation,
                    settings.count(),
                    settings.offsetX(), settings.offsetY(), settings.offsetZ(),
                    settings.extra()
            );
        }
    }

    public static Particle resolveParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Particle.END_ROD;
        }
    }
}

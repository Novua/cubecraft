package net.novua.cc_trails;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrailManager {

    private final Cc_trails plugin;
    private final HelixTrailRenderer renderer = new HelixTrailRenderer();
    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Double> phaseByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TrailStyle> styleByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> groupTickByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> movementActiveByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> movementActiveUntilMsByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastMoveLocationByPlayer = new ConcurrentHashMap<>();

    private Settings settings;
    private BukkitTask task;

    public TrailManager(Cc_trails plugin) {
        this.plugin = plugin;
        this.settings = loadSettings();
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, settings.periodTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        this.settings = loadSettings();
        start();
    }

    public void enable(Player player) {
        enable(player, settings.defaultStyle());
    }

    public void enable(Player player, TrailStyle style) {
        enabledPlayers.add(player.getUniqueId());
        phaseByPlayer.putIfAbsent(player.getUniqueId(), 0.0);
        styleByPlayer.put(player.getUniqueId(), style);
        groupTickByPlayer.putIfAbsent(player.getUniqueId(), 0L);
        movementActiveByPlayer.put(player.getUniqueId(), false);
        movementActiveUntilMsByPlayer.put(player.getUniqueId(), 0L);
        lastMoveLocationByPlayer.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void disable(Player player) {
        UUID id = player.getUniqueId();
        enabledPlayers.remove(id);
        phaseByPlayer.remove(id);
        styleByPlayer.remove(id);
        groupTickByPlayer.remove(id);
        movementActiveByPlayer.remove(id);
        movementActiveUntilMsByPlayer.remove(id);
        lastMoveLocationByPlayer.remove(id);
    }

    public boolean isEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!enabledPlayers.contains(player.getUniqueId())) {
                continue;
            }
            if (settings.movingOnly() && !isMovementActive(player)) {
                continue;
            }

            double phase = phaseByPlayer.getOrDefault(player.getUniqueId(), 0.0);
            TrailStyle style = styleByPlayer.getOrDefault(player.getUniqueId(), settings.defaultStyle());
            long groupTick = groupTickByPlayer.getOrDefault(player.getUniqueId(), 0L);
            int frame = (int) ((groupTick / settings.groupFrameDelayTicks()) % settings.groupSize());
            renderer.render(player, settings, phase, style, frame);
            phaseByPlayer.put(player.getUniqueId(), phase + settings.phaseStep());
            groupTickByPlayer.put(player.getUniqueId(), groupTick + 1);
        }
    }

    private boolean isMovementActive(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long until = movementActiveUntilMsByPlayer.getOrDefault(id, 0L);

        if (now <= until) {
            movementActiveByPlayer.put(id, true);
            return true;
        }

        movementActiveByPlayer.put(id, false);
        return false;
    }

    public void handleMove(Player player, Location to) {
        UUID id = player.getUniqueId();
        if (!enabledPlayers.contains(id) || to == null) {
            return;
        }

        Location last = lastMoveLocationByPlayer.getOrDefault(id, to);
        boolean positionChanged = !samePosition(last, to);
        long now = System.currentTimeMillis();
        long activeUntil = movementActiveUntilMsByPlayer.getOrDefault(id, 0L);

        if (positionChanged) {
            movementActiveByPlayer.put(id, true);
            movementActiveUntilMsByPlayer.put(id, now + settings.movementTimeoutMs());
            lastMoveLocationByPlayer.put(id, to.clone());
            return;
        }

        if (now > activeUntil) {
            movementActiveByPlayer.put(id, false);
        }
    }

    private boolean samePosition(Location a, Location b) {
        return Math.abs(a.getX() - b.getX()) < 1.0e-5
                && Math.abs(a.getY() - b.getY()) < 1.0e-5
                && Math.abs(a.getZ() - b.getZ()) < 1.0e-5;
    }

    private Settings loadSettings() {
        plugin.reloadConfig();

        int periodTicks = Math.max(1, plugin.getConfig().getInt("trail.periodTicks", 2));
        double radius = plugin.getConfig().getDouble("trail.radius", 0.45);
        double frequency = plugin.getConfig().getDouble("trail.frequency", 2.2);
        int points = Math.max(1, plugin.getConfig().getInt("trail.points", 12));
        double step = plugin.getConfig().getDouble("trail.step", 0.35);
        double phaseStep = plugin.getConfig().getDouble("trail.phaseStep", 0.30);
        double verticalOffset = plugin.getConfig().getDouble("trail.verticalOffset", 1.0);
        double verticalStretch = plugin.getConfig().getDouble("trail.verticalStretch", 0.10);
        boolean movingOnly = plugin.getConfig().getBoolean("trail.movingOnly", true);
        long movementTimeoutMs = Math.max(1L, plugin.getConfig().getLong("trail.movementTimeoutMs", 200L));
        int groupSize = Math.max(1, plugin.getConfig().getInt("trail.groupSize", 3));
        int groupFrameDelayTicks = Math.max(1, plugin.getConfig().getInt("trail.groupFrameDelayTicks", 1));
        TrailStyle defaultStyle = TrailStyle.from(plugin.getConfig().getString("trail.defaultStyle", "helix"));
        if (defaultStyle == null) {
            defaultStyle = TrailStyle.HELIX;
        }

        Particle particle = HelixTrailRenderer.resolveParticle(plugin.getConfig().getString("trail.particle", "END_ROD"));
        int count = Math.max(1, plugin.getConfig().getInt("trail.count", 1));
        double offsetX = plugin.getConfig().getDouble("trail.offset.x", 0.0);
        double offsetY = plugin.getConfig().getDouble("trail.offset.y", 0.0);
        double offsetZ = plugin.getConfig().getDouble("trail.offset.z", 0.0);
        double extra = plugin.getConfig().getDouble("trail.extra", 0.0);

        return new Settings(
                periodTicks, radius, frequency, points, step, phaseStep,
                verticalOffset, verticalStretch, movingOnly, movementTimeoutMs, groupSize, groupFrameDelayTicks, defaultStyle,
                particle, count, offsetX, offsetY, offsetZ, extra
        );
    }

    public record Settings(
            int periodTicks,
            double radius,
            double frequency,
            int points,
            double step,
            double phaseStep,
            double verticalOffset,
            double verticalStretch,
            boolean movingOnly,
            long movementTimeoutMs,
            int groupSize,
            int groupFrameDelayTicks,
            TrailStyle defaultStyle,
            Particle particle,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra
    ) {
    }
}

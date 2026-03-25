package net.novua.cc_trails;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class TrailPlayerListener implements Listener {

    private final TrailManager trailManager;

    public TrailPlayerListener(TrailManager trailManager) {
        this.trailManager = trailManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        trailManager.handleMove(event.getPlayer(), event.getTo());
    }
}

package com.chkaduuu.mcbank.listeners;

import org.bukkit.event.EventHandler;
import com.chkaduuu.mcbank.models.PlayerData;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.player.PlayerJoinEvent;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.event.Listener;

public class PlayerJoinListener implements Listener
{
    private final McBank plugin;

    public PlayerJoinListener(final McBank plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd != null) {
            pd.setName(player.getName());
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (pd == null || pd.getPendingNotifications().isEmpty()) {
                return;
            }
            // FIX: explicit String type in for-each instead of raw iterator with Object cast
            for (final String msg : pd.getPendingNotifications()) {
                player.sendMessage(msg);
            }
            pd.clearPendingNotifications();
            this.plugin.getAccountManager().saveData();
        }, 20L);
    }
}

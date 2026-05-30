
package com.chkaduuu.mcbank.listeners;

import org.bukkit.event.EventHandler;
import com.chkaduuu.mcbank.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.event.Listener;

public class ATMListener implements Listener
{
    private final McBank plugin;
    private final Map<UUID, String> pinInput;
    private final Map<UUID, Boolean> awaitingPin;
    
    public ATMListener(final McBank plugin) {
        this.pinInput = new HashMap<UUID, String>();
        this.awaitingPin = new HashMap<UUID, Boolean>();
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!this.plugin.getATMManager().isATM(event.getClickedBlock().getLocation())) {
            return;
        }
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        if (!this.plugin.getAccountManager().hasAccount(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("no_account"));
            return;
        }
        if (this.plugin.getConfigManager().isAtmPinRequired()) {
            final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
            if (pd.getPin() == null) {
                player.sendMessage(this.plugin.getLangManager().get("pin_required"));
                return;
            }
            if (this.plugin.getAccountManager().isPinLocked(uuid)) {
                player.sendMessage(this.plugin.getLangManager().get("pin_locked"));
                return;
            }
            player.sendMessage(this.plugin.getLangManager().get("pin_required"));
            player.sendMessage("§eEnter your 4-digit PIN:");
            this.plugin.getChatInputListener().requestInput(player, ChatInputListener.InputType.WITHDRAW_AMOUNT, input -> this.handlePinEntry(player, uuid, input), "atm");
        }
        else {
            this.openATMForPlayer(player, uuid);
        }
    }
    
    private void handlePinEntry(final Player player, final UUID uuid, final String input) {
        final PlayerData pd = this.plugin.getAccountManager().getPlayerData(uuid);
        if (pd == null) {
            return;
        }
        if (this.plugin.getAccountManager().isPinLocked(uuid)) {
            player.sendMessage(this.plugin.getLangManager().get("pin_locked"));
            return;
        }
        if (pd.getPin().equals(input)) {
            this.plugin.getAccountManager().resetPinAttempts(uuid);
            this.openATMForPlayer(player, uuid);
        }
        else {
            final int attempts = this.plugin.getAccountManager().incrementPinAttempts(uuid);
            if (this.plugin.getAccountManager().isPinLocked(uuid)) {
                player.sendMessage(this.plugin.getLangManager().get("pin_locked"));
            }
            else {
                final int remaining = this.plugin.getAccountManager().getRemainingPinAttempts(uuid);
                player.sendMessage(this.plugin.getLangManager().get("pin_incorrect", "%remaining%", String.valueOf(remaining)));
            }
        }
    }
    
    private void openATMForPlayer(final Player player, final UUID uuid) {
        if (!this.plugin.getAccountManager().canDoAtmOperation(uuid)) {
            final int limit = this.plugin.getConfigManager().getAtmDailyLimit();
            player.sendMessage(this.plugin.getLangManager().get("atm_daily_limit_reached", "%limit%", String.valueOf(limit)));
            return;
        }
        this.plugin.getGuiManager().openATMMenu(player);
    }
}

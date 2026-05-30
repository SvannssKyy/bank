
package com.chkaduuu.mcbank.listeners;

import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import com.chkaduuu.mcbank.McBank;
import org.bukkit.event.Listener;

public class ChatInputListener implements Listener
{
    private final McBank plugin;
    private final Map<UUID, PendingInput> pendingInputs;
    
    public ChatInputListener(final McBank plugin) {
        this.pendingInputs = new HashMap<UUID, PendingInput>();
        this.plugin = plugin;
    }
    
    public void requestInput(final Player player, final InputType type, final Consumer<String> callback, final String extraData) {
        final UUID uuid = player.getUniqueId();
        this.cancelPending(uuid);
        final BukkitTask timeout = this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.pendingInputs.remove(uuid);
            player.sendMessage(this.plugin.getLangManager().get("chat_input_timeout"));
            return;
        }, 600L);
        this.pendingInputs.put(uuid, new PendingInput(type, callback, timeout, extraData));
        player.sendMessage(this.plugin.getLangManager().get("chat_input_type"));
    }
    
    public void cancelPending(final UUID uuid) {
        final PendingInput pending = this.pendingInputs.remove(uuid);
        if (pending != null && pending.timeout != null) {
            pending.timeout.cancel();
        }
    }
    
    public boolean hasPendingInput(final UUID uuid) {
        return this.pendingInputs.containsKey(uuid);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final PendingInput pending = this.pendingInputs.get(uuid);
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        final String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            this.cancelPending(uuid);
            return;
        }
        this.pendingInputs.remove(uuid);
        if (pending.timeout != null) {
            pending.timeout.cancel();
        }
        final String finalMsg = msg;
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> pending.callback.accept(finalMsg));
    }
    
    public enum InputType
    {
        DEPOSIT_AMOUNT, 
        WITHDRAW_AMOUNT, 
        CASHOUT_AMOUNT, 
        TRANSFER_PLAYER, 
        TRANSFER_AMOUNT, 
        LOAN_AMOUNT, 
        LOAN_REPAY_AMOUNT;
    }
    
    public static class PendingInput
    {
        public final InputType type;
        public final Consumer<String> callback;
        public final BukkitTask timeout;
        public final String extraData;
        
        public PendingInput(final InputType type, final Consumer<String> callback, final BukkitTask timeout, final String extraData) {
            this.type = type;
            this.callback = callback;
            this.timeout = timeout;
            this.extraData = extraData;
        }
    }
}
